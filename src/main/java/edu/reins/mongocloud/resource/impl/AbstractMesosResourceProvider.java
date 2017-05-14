package edu.reins.mongocloud.resource.impl;

import edu.reins.mongocloud.ResourceProvider;
import edu.reins.mongocloud.ResourceStatusListener;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.InstanceState;
import edu.reins.mongocloud.support.Instances;
import edu.reins.mongocloud.support.annotation.Ignored;
import edu.reins.mongocloud.support.annotation.Stateless;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Stateless
public abstract class AbstractMesosResourceProvider implements ResourceProvider, Scheduler {
    @Autowired
    protected ResourceStatusListener resourceStatusListener;

    @Autowired
    protected FrameworkStore frameworkStore;

    @Autowired
    protected SchedulerDriver schedulerDriver;

    @Override
    public void registered(SchedulerDriver schedulerDriver, Protos.FrameworkID frameworkID, Protos.MasterInfo masterInfo) {
        log.info("register(framework: {}, master: {})", frameworkID.getValue(), masterInfo.getId());

        frameworkStore.setFrameworkId(frameworkID);

        resourceStatusListener.onPlatformPrepared();
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
                resourceStatusListener.onInstanceRunning(taskStatus);
                break;

            case TASK_LOST:
                resourceStatusListener.onInstanceFailed(taskStatus);
                break;

                // killed by the executor
            case TASK_KILLED:
                resourceStatusListener.onInstanceKilled(taskStatus);
                break;

                // this state should not be seen
            case TASK_FINISHED:
                log.warn("badStatus(task: {}): should not see FINISHED", taskStatus.getTaskId());
                // fallthrough

                // task failed due to misconfigured commandInfo
            case TASK_FAILED:
            case TASK_ERROR:
                resourceStatusListener.onInstanceError(taskStatus);
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
                frameworkStore.reset();
                resourceStatusListener.onClusterDestroyed();
                break;

            case FRAMEWORK_FAILOVER:
                resourceStatusListener.onFailover();
                break;

            default:
                log.error("error(msg: {})", s);
                break;
        }
    }

    @Override
    public void sync(@Nonnull final List<Instance> instances) {
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
}
