package reins.wuqq.resource;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import reins.wuqq.ResourceProvider;
import reins.wuqq.ResourceStatusListener;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;

@Slf4j(topic = "resourceProvider")
public abstract class AbstractMesosResourceProvider implements ResourceProvider, Scheduler {
    @Autowired
    @Qualifier("cluster")
    protected ResourceStatusListener resourceStatusListener;

    @Autowired
    protected PersistedFrameworkDetail frameworkConfiguration;

    @Autowired
    protected SchedulerDriver schedulerDriver;

    @Override
    public void registered(SchedulerDriver schedulerDriver, Protos.FrameworkID frameworkID, Protos.MasterInfo masterInfo) {
        log.info("Register(framework: {}, master: {})", frameworkID.getValue(), masterInfo.getId());

        frameworkConfiguration.setFrameworkId(frameworkID);
        resourceStatusListener.onPlatformPrepared();
    }

    @Override
    public void reregistered(SchedulerDriver schedulerDriver, Protos.MasterInfo masterInfo) {
        log.info("Reregistered(master: {})", masterInfo.getId());

        schedulerDriver.reconcileTasks(Collections.emptySet());
    }

    @Override
    public void offerRescinded(SchedulerDriver schedulerDriver, Protos.OfferID offerID) {
        log.debug("Rescinded(offer: {})", offerID.getValue());
    }

    @Override
    public void statusUpdate(SchedulerDriver schedulerDriver, Protos.TaskStatus taskStatus) {
        val taskIdMarker = MarkerFactory.getMarker("taskId:" + taskStatus.getTaskId().getValue());

        log.debug(taskIdMarker, "statusUpdate(status: {})", taskStatus.getState().toString());

        switch (taskStatus.getState()) {
            case TASK_STAGING:
            case TASK_STARTING:
                break;

            case TASK_RUNNING:
                onNodeStarted(taskStatus);
                resourceStatusListener.onInstanceStarted(taskStatus);
                break;

            case TASK_FINISHED:
            case TASK_LOST:
            case TASK_KILLED:
            case TASK_FAILED:
                onNodeLost(taskStatus);
                resourceStatusListener.onInstanceLost(taskStatus);
                break;

            case TASK_ERROR:
                log.error("statusUpdate:error(id: {}, reason: {}", taskStatus.getTaskId().getValue(), taskStatus.getReason());
        }
    }

    protected abstract void onNodeStarted(@Nonnull Protos.TaskStatus status);

    protected abstract void onNodeLost(@Nonnull Protos.TaskStatus status);

    @Override
    public void frameworkMessage(SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, byte[] bytes) {
        log.debug("FrameworkMessage(executor: {}, slave: {})", executorID.getValue(), slaveID.getValue());
    }

    @Override
    public void disconnected(SchedulerDriver schedulerDriver) {
        log.error("Disconnected");
    }

    @Override
    public void slaveLost(SchedulerDriver schedulerDriver, Protos.SlaveID slaveID) {
        log.info("SlaveLost(slave: {})", slaveID.getValue());
    }

    @Override
    public void executorLost(SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, int i) {
        log.info("ExecutorLost(executor: {}, slave: {})", executorID.getValue(), slaveID.getValue());
    }

    @Override
    public void error(SchedulerDriver schedulerDriver, String s) {
        log.error("Error(msg: {})", s);

        switch (s) {
            case FRAMEWORK_REMOVED:
                frameworkConfiguration.clearFrameworkId();
                resourceStatusListener.onClusterDestroyed();
                break;
        }
    }

    private static final String FRAMEWORK_REMOVED = "Framework has been removed";

    @Scheduled(fixedRate = 60 * 1000)
    public void sync() {
        log.info("Sync(scope: all)");

        schedulerDriver.reconcileTasks(Collections.emptySet());
    }

    @Override
    public void sync(@Nonnull Protos.TaskID taskID) {
        log.info("Sync(scope: {})", taskID.getValue());

        val taskStatus = Protos.TaskStatus.newBuilder()
                .setTaskId(taskID)
                .setState(Protos.TaskState.TASK_RUNNING)
                .build();

        schedulerDriver.reconcileTasks(Arrays.asList(taskStatus));
    }
}
