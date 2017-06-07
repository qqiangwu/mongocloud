package edu.reins.mongocloud.resource;

import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.ResourceProvider;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceEvent;
import edu.reins.mongocloud.instance.InstanceEventType;
import edu.reins.mongocloud.instance.InstanceState;
import edu.reins.mongocloud.support.Instances;
import edu.reins.mongocloud.support.TaskBuilder;
import edu.reins.mongocloud.support.TaskMatcher;
import edu.reins.mongocloud.support.annotation.Ignored;
import edu.reins.mongocloud.support.annotation.SoftState;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Daemon
@ThreadSafe
@SoftState
@Slf4j
public class MesosResourceProvider implements ResourceProvider, Scheduler {
    @Value("${docker.volume}")
    private String dockerVolume;

    @Autowired
    private SchedulerDriver schedulerDriver;

    @Autowired
    private EventBus eventBus;

    private Queue<Instance> pendingTasks = new ConcurrentLinkedQueue<>();

    @Override
    public void registered(SchedulerDriver schedulerDriver, Protos.FrameworkID frameworkID, Protos.MasterInfo masterInfo) {
        log.info("register(framework: {}, master: {})", frameworkID.getValue(), masterInfo.getId());

        notifyCluster(ClusterEventType.SETUP);
    }

    // Handling master failover
    @Override
    public void reregistered(SchedulerDriver schedulerDriver, Protos.MasterInfo masterInfo) {
        log.info("reregistered(master: {})", masterInfo.getId());

        // implicit reconcile
        schedulerDriver.reconcileTasks(Collections.emptySet());
    }

    @Ignored
    @Override
    public void offerRescinded(SchedulerDriver schedulerDriver, Protos.OfferID offerID) {
        log.debug("rescinded(offer: {})", offerID.getValue());
    }

    @Override
    public void statusUpdate(SchedulerDriver schedulerDriver, Protos.TaskStatus taskStatus) {
        switch (taskStatus.getState()) {
            case TASK_STAGING:
            case TASK_STARTING:
                // ignore it, since it will finally transit to RUNNING OR TERMINATED
                break;

            case TASK_RUNNING:
                notifyInstance(taskStatus.getTaskId(), InstanceEventType.RUNNING);
                break;

            case TASK_LOST:
                notifyInstance(taskStatus.getTaskId(), InstanceEventType.FAILED);
                break;

            // killed by the executor
            case TASK_KILLED:
                notifyInstance(taskStatus.getTaskId(), InstanceEventType.KILLED);
                break;

            // this state should not be seen
            case TASK_FINISHED:
                log.warn("badStatus(task: {}): should not see FINISHED", taskStatus.getTaskId());
                // fallthrough

                // task failed due to misconfigured commandInfo
            case TASK_FAILED:
            case TASK_ERROR:
                notifyInstance(taskStatus.getTaskId(), InstanceEventType.ERROR);
                break;
        }
    }

    @Ignored
    @Override
    public void frameworkMessage(SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, byte[] bytes) {
        log.debug("frameworkMessage(executor: {}, slave: {})", executorID.getValue(), slaveID.getValue());
    }

    @Ignored
    @Override
    public void disconnected(SchedulerDriver schedulerDriver) {
        log.error("disconnected");
    }

    @Ignored
    @Override
    public void slaveLost(SchedulerDriver schedulerDriver, Protos.SlaveID slaveID) {
        log.info("slaveLost(slave: {})", slaveID.getValue());
    }

    @Ignored
    @Override
    public void executorLost(SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, int i) {
        log.info("executorLost(executor: {}, slave: {})", executorID.getValue(), slaveID.getValue());
    }

    private static final String FRAMEWORK_REMOVED = "Framework has been removed";
    private static final String FRAMEWORK_FAILOVER = "Framework failover";

    @Override
    public void error(SchedulerDriver schedulerDriver, String s) {
        switch (s) {
            case FRAMEWORK_REMOVED:
                notifyCluster(ClusterEventType.DESTROYED);
                break;

            case FRAMEWORK_FAILOVER:
                notifyCluster(ClusterEventType.FAILOVER);
                break;

            default:
                log.error("error(msg: {})", s);
                break;
        }
    }

    @Override
    public void sync(final List<Instance> instances) {
        val reconcilable = new ArrayList<Protos.TaskStatus>();

        instances.forEach(instance -> {
            if (instance.getState().equals(InstanceState.SUBMITTED)) {
                launch(instance);
            } else {
                final Optional<Protos.SlaveID> slaveID = Instances.slaveID(instance);
                final Optional<Protos.TaskID> taskID = Instances.taskID(instance);

                if (!slaveID.isPresent() || !taskID.isPresent()) {
                    log.warn("syncBadInstance(instance: {})", instance);
                }

                final Protos.TaskStatus status = Protos.TaskStatus.newBuilder()
                        .setTaskId(taskID.get())
                        .setSlaveId(slaveID.get())
                        .build();

                reconcilable.add(status);
            }
        });

        schedulerDriver.reconcileTasks(reconcilable);
    }

    private void notifyCluster(final ClusterEventType eventType) {
        eventBus.post(new ClusterEvent(eventType));
    }

    private void notifyInstance(final Protos.TaskID instanceID, final InstanceEventType eventType) {
        eventBus.post(new InstanceEvent(eventType, instanceID.getValue()));
    }

    @Override
    public void launch(final Instance instance) {
        pendingTasks.add(instance);
    }

    @Override
    public void kill(final String instanceID) {
        log.info("kill(instanceID: {})", instanceID);

        schedulerDriver.killTask(Protos.TaskID.newBuilder().setValue(instanceID).build());
    }

    // FIXME:   use better scheduling algorithms
    @Override
    public void resourceOffers(final SchedulerDriver driver, final List<Offer> offers) {
        for (val offer: offers) {
            val launched = tryLaunch(offer);

            if (!launched) {
                driver.declineOffer(offer.getId());
            }
        }

        log.trace("resourceOffers(pending: {})", pendingTasks.size());
    }

    private boolean tryLaunch(final Offer offer) {
        for (int i = 0, len = pendingTasks.size(); i < len; ++i) {
            val task = pendingTasks.poll();
            val launched = tryLaunchTaskOn(task, offer);

            if (launched) {
                return true;
            }
        }

        return false;
    }

    private boolean tryLaunchTaskOn(final Instance instance, final Offer offer) {
        val launchable = TaskMatcher.matches(offer, instance);

        if (launchable) {
            launchOn(instance, offer);
            return true;
        }

        return false;
    }

    private void launchOn(final Instance instance, final Offer offer) {
        log.debug("> launch(id: {}, instance: {}, slave: {})", instance.getId(), instance, offer.getSlaveId().getValue());

        val taskInfo = new TaskBuilder()
                .setDockerVolume(dockerVolume)
                .setOffer(offer)
                .setInsance(instance)
                .build();

        syncInstance(offer, taskInfo, instance);

        // store update go first
        eventBus.post(new InstanceEvent(InstanceEventType.LAUNCHED, instance));

        schedulerDriver.launchTasks(Arrays.asList(offer.getId()), Arrays.asList(taskInfo));
    }

    private void syncInstance(final Offer offer, final Protos.TaskInfo taskInfo, final Instance instance) {
        instance.setHostIP(offer.getHostname());
        instance.setSlaveID(taskInfo.getSlaveId().getValue());
        instance.setTaskID(taskInfo.getTaskId().getValue());
    }
}