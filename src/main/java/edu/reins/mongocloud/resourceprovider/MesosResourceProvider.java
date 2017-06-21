package edu.reins.mongocloud.resourceprovider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.ResourceProvider;
import edu.reins.mongocloud.clustermanager.ClusterManagerEvent;
import edu.reins.mongocloud.clustermanager.ClusterManagerEventType;
import edu.reins.mongocloud.instance.*;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.model.InstanceLaunchRequest;
import edu.reins.mongocloud.support.TaskBuilder;
import edu.reins.mongocloud.support.TaskMatcher;
import edu.reins.mongocloud.support.annotation.Ignored;
import edu.reins.mongocloud.support.annotation.Nothrow;
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
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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

    @Autowired
    private ObjectMapper objectMapper;

    private BlockingQueue<InstanceLaunchRequest> pendingTasks = new ArrayBlockingQueue<>(1024);

    @Override
    @Nothrow
    public void registered(SchedulerDriver schedulerDriver, Protos.FrameworkID frameworkID, Protos.MasterInfo masterInfo) {
        LOG.info("register(framework: {}, master: {})", frameworkID.getValue(), masterInfo.getId());

        notifyClusterManager(ClusterManagerEventType.SETUP);
    }

    // Handling master failover
    @Override
    @Nothrow
    public void reregistered(SchedulerDriver schedulerDriver, Protos.MasterInfo masterInfo) {
        LOG.info("reregistered(master: {})", masterInfo.getId());

        // implicit reconcile
        schedulerDriver.reconcileTasks(Collections.emptySet());
    }

    @Ignored
    @Nothrow
    @Override
    public void offerRescinded(SchedulerDriver schedulerDriver, Protos.OfferID offerID) {
        LOG.debug("rescinded(offer: {})", offerID.getValue());
    }

    @Override
    @Nothrow
    public void statusUpdate(SchedulerDriver schedulerDriver, Protos.TaskStatus taskStatus) {
        switch (taskStatus.getState()) {
            case TASK_STAGING:
            case TASK_STARTING:
                // ignore it, since it will finally transit to RUNNING OR TERMINATED
                break;

            case TASK_RUNNING:
                if (taskStatus.getReason() == Protos.TaskStatus.Reason.REASON_RECONCILIATION) {
                    notifyInstance(taskStatus.getTaskId(), InstanceEventType.RUNNING);
                } else {
                    final ContainerInfo info = parseContainerInfo(taskStatus.getData());

                    notifyInstance(taskStatus.getTaskId(), InstanceEventType.RUNNING, info);
                }
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
                LOG.warn("badStatus(task: {}): should not see FINISHED", taskStatus.getTaskId());
                // fallthrough

                // task failed due to misconfigured commandInfo
            case TASK_FAILED:
            case TASK_ERROR:
                // FIXME    pass error message
                LOG.warn("failed(task: {}, msg: {})", taskStatus.getTaskId().getValue(), taskStatus.getMessage());
                notifyInstance(taskStatus.getTaskId(), InstanceEventType.ERROR);
                break;
        }
    }

    @Ignored
    @Nothrow
    @Override
    public void frameworkMessage(
            SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, byte[] bytes) {
        LOG.debug("frameworkMessage(executor: {}, slave: {})", executorID.getValue(), slaveID.getValue());
    }

    @Ignored
    @Nothrow
    @Override
    public void disconnected(SchedulerDriver schedulerDriver) {
        LOG.error("disconnected");
    }

    @Ignored
    @Nothrow
    @Override
    public void slaveLost(SchedulerDriver schedulerDriver, Protos.SlaveID slaveID) {
        LOG.info("slaveLost(slave: {})", slaveID.getValue());
    }

    @Ignored
    @Nothrow
    @Override
    public void executorLost(
            SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, int i) {
        LOG.info("executorLost(executor: {}, slave: {})", executorID.getValue(), slaveID.getValue());
    }

    private static final String FRAMEWORK_REMOVED = "Framework has been removed";
    private static final String FRAMEWORK_FAILOVER = "Framework failover";

    @Nothrow
    @Override
    public void error(SchedulerDriver schedulerDriver, String s) {
        switch (s) {
            case FRAMEWORK_REMOVED:
                notifyClusterManager(ClusterManagerEventType.DESTROYED);
                break;

            case FRAMEWORK_FAILOVER:
                notifyClusterManager(ClusterManagerEventType.FAILOVER);
                break;

            default:
                LOG.error("error(msg: {})", s);
                break;
        }
    }

    @Nothrow
    private void notifyClusterManager(final ClusterManagerEventType eventType) {
        eventBus.post(new ClusterManagerEvent(eventType));
    }

    @Nothrow
    private void notifyInstance(final Protos.TaskID taskID, final InstanceEventType eventType) {
        final InstanceID instanceID = new InstanceID(taskID.getValue());

        eventBus.post(new InstanceEvent(eventType, instanceID));
    }

    @Nothrow
    private void notifyInstance(final Protos.TaskID taskID, final InstanceEventType eventType, final Object payload) {
        final InstanceID instanceID = new InstanceID(taskID.getValue());

        eventBus.post(new InstanceEvent(eventType, instanceID, payload));
    }

    @Override
    @Nothrow
    public void launch(final InstanceLaunchRequest request) {
        try {
            pendingTasks.put(request);
        } catch (InterruptedException e) {
            LOG.info("launch: operation interrupted");
        }
    }

    @Nothrow
    @Override
    public void kill(final InstanceID id) {
        LOG.info("kill(instanceID: {})", id);

        schedulerDriver.killTask(Instances.toTaskID(id));
    }

    @Nothrow
    @Override
    public void sync(final List<Instance> instances) {
        // TODO add sync support
    }

    @Nothrow
    @Override
    public void resourceOffers(final SchedulerDriver driver, final List<Offer> offers) {
        for (val offer: offers) {
            val launched = tryLaunch(offer);

            if (!launched) {
                driver.declineOffer(offer.getId());
            }
        }
    }

    @Nothrow
    private boolean tryLaunch(final Offer offer) {
        while (!pendingTasks.isEmpty()) {
            val task = pendingTasks.poll();

            if (task != null) {
                val launched = tryLaunchTaskOn(task, offer);

                if (launched) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nothrow
    private boolean tryLaunchTaskOn(final InstanceLaunchRequest request, final Offer offer) {
        val launchable = TaskMatcher.matches(offer, request.getDefinition());

        if (launchable) {
            launchOn(request, offer);
            return true;
        }

        return false;
    }

    @Nothrow
    private void launchOn(final InstanceLaunchRequest request, final Offer offer) {
        LOG.debug("launch(id: {}, slave: {})", request.getInstanceID(), offer.getSlaveId().getValue());

        val taskInfo = new TaskBuilder()
                .setDockerVolume(dockerVolume)
                .setOffer(offer)
                .setInstanceRequest(request)
                .build();

        val instanceHost = createInstanceHost(offer, taskInfo);

        // store update go first
        eventBus.post(new InstanceEvent(InstanceEventType.LAUNCHED, request.getInstanceID(), instanceHost));

        schedulerDriver.launchTasks(Collections.singletonList(offer.getId()), Collections.singletonList(taskInfo));
    }

    @Nothrow
    private InstanceHost createInstanceHost(final Offer offer, final Protos.TaskInfo taskInfo) {
        final String hostIP = offer.getHostname();
        final Protos.SlaveID slaveID = taskInfo.getSlaveId();
        final int port = taskInfo.getContainer().getDocker().getPortMappings(0).getHostPort();

        return new InstanceHost(slaveID.getValue(), hostIP, port);
    }

    @Nothrow
    private ContainerInfo parseContainerInfo(final ByteString bytes) {
        try {
            final Map<String, Object>[] container = objectMapper.readValue(bytes.toByteArray(), Map[].class);
            final String containerName = (String) container[0].get("Name");
            final ContainerInfo info = new ContainerInfo(containerName, null);

            return info;
        } catch (IOException e) {
            throw new AssertionError("Bad containerInfo from docker", e);
        }
    }
}