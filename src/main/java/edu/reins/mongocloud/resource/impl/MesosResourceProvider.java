package edu.reins.mongocloud.resource.impl;

import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.support.TaskBuilder;
import edu.reins.mongocloud.support.TaskMatcher;
import edu.reins.mongocloud.support.annotation.SoftState;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.SchedulerDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@ThreadSafe
@SoftState
@Slf4j(topic = "resourceProvider")
public class MesosResourceProvider extends AbstractMesosResourceProvider {
    @Value("${docker.volume}")
    private String dockerVolume;

    private Queue<Instance> pendingTasks = new ConcurrentLinkedQueue<>();

    @Override
    public void launch(@Nonnull final Instance instance) {
        pendingTasks.add(instance);
    }

    @Override
    public void kill(@Nonnull final String instanceID) {
        log.info("kill(instanceID: {})", instanceID);

        schedulerDriver.killTask(Protos.TaskID.newBuilder().setValue(instanceID).build());
    }

    // FIXME:   use better scheduling algorithms
    @Override
    public void resourceOffers(@Nonnull final SchedulerDriver driver, @Nonnull final List<Offer> offers) {
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
        resourceStatusListener.onInstanceLaunched(instance);

        schedulerDriver.launchTasks(Arrays.asList(offer.getId()), Arrays.asList(taskInfo));
    }

    private void syncInstance(final Offer offer, final Protos.TaskInfo taskInfo, final Instance instance) {
        instance.setHostIP(offer.getHostname());
        instance.setSlaveID(taskInfo.getSlaveId().getValue());
        instance.setTaskID(taskInfo.getTaskId().getValue());
    }
}