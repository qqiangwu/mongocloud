package edu.reins.mongocloud.resource;

import edu.reins.mongocloud.support.MesosUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.*;
import org.apache.mesos.Protos.ContainerInfo.DockerInfo;
import org.apache.mesos.Protos.ContainerInfo.DockerInfo.Network;
import org.apache.mesos.Protos.Value.Scalar;
import org.apache.mesos.Protos.Value.Type;
import org.apache.mesos.SchedulerDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.support.ClusterUtil;
import edu.reins.mongocloud.support.ResourceDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * FIXME 目前，不考虑消息丢失与机器失效的情况。
 *
 */
@Component
@ThreadSafe
@Slf4j(topic = "resourceProvider")
public class MesosResourceProvider extends AbstractMesosResourceProvider {
    @Autowired
    private PersistedSchedulerDetail schedulerTasks;

    @PostConstruct
    public void setup() {
        log.info("setup(cluster: {})", schedulerTasks.get());
    }

    @Override
    public synchronized Instance launch(@Nonnull final Instance instance) {
       schedulerTasks.addPendingTask(instance);
       return instance;
    }

    @Override
    public synchronized void kill(@Nonnull final TaskID taskID) {
        schedulerDriver.killTask(taskID);
    }

    @Override
    public synchronized void resourceOffers(@Nonnull final SchedulerDriver driver, @Nonnull final List<Offer> offers) {
        val pendingTasks = schedulerTasks.getPendingTasks();
        boolean hasLaunched = false;

        for (val offer: offers) {
            val launched = tryLaunch(offer, pendingTasks);

            if (!launched) {
                driver.declineOffer(offer.getId());
            } else {
                hasLaunched = launched;
            }
        }

        if (hasLaunched) {
            schedulerTasks.setPendingTasks(pendingTasks);

            log.trace("resourceOffers(pending: {})", schedulerTasks.getPendingTasks().size());
        }
    }

    private boolean tryLaunch(final Offer offer, final List<Instance> pendingTasks) {
        val iterator = pendingTasks.iterator();

        while (iterator.hasNext()) {
            val task = iterator.next();
            val launched = tryLaunchTaskOn(task, offer);

            if (launched) {
                iterator.remove();
                return true;
            }
        }

        return false;
    }

    private boolean tryLaunchTaskOn(final Instance task, final Offer offer) {
        val launchable = hasSufficientResource(offer.getResourcesList(), task);

        if (launchable) {
            launchOn(task, offer);
            return true;
        }

        return false;
    }

    private void launchOn(final Instance instance, final Offer offer) {
        log.debug("> launch(id: {}, instance: {}, node: {})",
                instance.getId(), instance, offer.getSlaveId().getValue());


        val taskInfo = buildTask(instance, offer);

        log.debug("< launch(id: {}, task: {})", instance.getId(), taskInfo);

        schedulerDriver.launchTasks(Arrays.asList(offer.getId()), Arrays.asList(taskInfo));

        instance.setHostIP(offer.getHostname());

        resourceStatusListener.onInstanceLaunched(instance);
    }

    private TaskInfo buildTask(final Instance instance, final Offer offer) {
        val portMapping = preparePortMapping(instance, offer);
        val containerInfo = buildContainer(instance, portMapping);
        val commandInfo = buildCommand(instance);

        return TaskInfo.newBuilder()
                .setTaskId(MesosUtil.toID(instance.getId()))
                .setName(ClusterUtil.getTaskName(instance))
                .setSlaveId(offer.getSlaveId())
                .setContainer(containerInfo)
                .setCommand(commandInfo)
                .addResources(getMemRequirement(instance))
                .addResources(getCPURequirement(instance))
                .addResources(getDiskRequirement(instance))
                .addResources(getPortMapping(portMapping))
                .build();
    }

    private DockerInfo.PortMapping preparePortMapping(final Instance instance, final Offer offer) {
        val containerPort = getContainerPort(instance);
        val hostPort = new ResourceDescriptor(offer.getResourcesList()).getPorts().get(0);

        instance.setPort(hostPort);

        return DockerInfo.PortMapping.newBuilder()
                .setContainerPort(containerPort)
                .setHostPort(hostPort)
                .setProtocol("tcp")
                .build();
    }

    private int getContainerPort(final Instance instance) {
        switch (instance.getType()) {
            case CONFIG_SERVER: return 27019;
            case PROXY_SERVER: return 27017;
            case SHARD: return 27018;
        }

        throw new IllegalArgumentException("Bad instance type");
    }

    private CommandInfo buildCommand(final Instance instance) {
        val args = instance.getArgs().split(" ");

        return CommandInfo.newBuilder()
                .setShell(false)
                .addAllArguments(Arrays.asList(args))
                .build();
    }

    private Resource getDiskRequirement(final Instance instance) {
        return Resource.newBuilder()
                .setName("disk")
                .setType(Type.SCALAR)
                .setScalar(Scalar.newBuilder().setValue(instance.getDisk()))
                .build();
    }

    private Resource getCPURequirement(final Instance instance) {
        return Resource.newBuilder()
                .setName("cpus")
                .setType(Type.SCALAR)
                .setScalar(Scalar.newBuilder().setValue(instance.getCpus()))
                .build();
    }

    private Resource getMemRequirement(final Instance instance) {
        return Resource.newBuilder()
                .setName("mem")
                .setType(Type.SCALAR)
                .setScalar(Scalar.newBuilder().setValue(instance.getMemory()))
                .build();
    }

    private Resource getPortMapping(final DockerInfo.PortMapping mapping) {
        val port = mapping.getHostPort();
        val range = Value.Ranges.newBuilder()
                .addRange(Value.Range.newBuilder().setBegin(port).setEnd(port));

        return Resource.newBuilder()
                .setName("ports")
                .setType(Type.RANGES)
                .setRanges(range)
                .build();
    }

    @org.springframework.beans.factory.annotation.Value("${docker.volume}")
    private String dockerVolume;

    private ContainerInfo.Builder buildContainer(final Instance instance, final DockerInfo.PortMapping portMapping) {
        val docker = DockerInfo.newBuilder()
                .setImage(instance.getImage())
                .setNetwork(Network.BRIDGE)
                .setForcePullImage(false)
                .addPortMappings(portMapping)
                .build();

        val volume = Volume.newBuilder()
                .setContainerPath(dockerVolume)
                .setMode(Volume.Mode.RW)
                .build();

        return ContainerInfo.newBuilder()
                .setType(ContainerInfo.Type.DOCKER)
                //.addVolumes(volume)
                .setDocker(docker);
    }

    private boolean hasSufficientResource(final List<Resource> offeredResources, final Instance instance) {
        val offeredResourceDesc = new ResourceDescriptor(offeredResources);

        if (offeredResourceDesc.getCpus() < instance.getCpus()) {
            return false;
        }
        if (offeredResourceDesc.getMemory() < instance.getMemory()) {
            return false;
        }
        if (offeredResourceDesc.getDisk() < instance.getDisk()) {
            return false;
        }
        if (offeredResourceDesc.getPorts().isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    protected synchronized void onNodeStarted(final @Nonnull TaskStatus status) {
        log.info("OnNodeStarted(taskId: {}, pending: {})", status.getTaskId().getValue(), schedulerTasks.getPendingTasks().size());

        val pendingTasks = schedulerTasks.getPendingTasks();
        val rest = pendingTasks.stream()
                .filter(instance -> !instance.getId().equals(status.getTaskId().getValue()))
                .collect(Collectors.toList());

        if (pendingTasks.size() != rest.size()) {
            schedulerTasks.setPendingTasks(rest);
        }
    }

    @Override
    protected synchronized void onNodeLost(final @Nonnull TaskStatus status) {
        log.info("OnNodeLost(taskId: {}, reason: {})", status.getTaskId().getValue(), status.getReason());

        if (status.getState() == TaskState.TASK_FAILED) {
            schedulerDriver.killTask(status.getTaskId());
        }
    }

    @Override
    public void sync(@Nonnull Protos.TaskID taskID) {
        val isPending = schedulerTasks.getPendingTasks().stream()
                .filter(i -> i.getId().equals(taskID.getValue()))
                .findFirst();

        if (isPending.isPresent()) {
            log.info("sync(pending: {})", taskID.getValue());
        } else {
            super.sync(taskID);
        }
    }
}