package edu.reins.mongocloud.resourceprovider.support;

import edu.reins.mongocloud.instance.Instances;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceLaunchRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.val;
import org.apache.mesos.Protos;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class TaskBuilder {
    private Protos.Offer offer;
    private InstanceLaunchRequest instanceRequest;
    private InstanceDefinition definition;

    @Nothrow
    public TaskBuilder setOffer(final Protos.Offer offer) {
        this.offer = offer;

        return this;
    }

    @Nothrow
    public TaskBuilder setInstanceRequest(final InstanceLaunchRequest instanceRequest) {
        this.instanceRequest = instanceRequest;
        this.definition = instanceRequest.getDefinition();

        return this;
    }

    /**
     * @throws NullPointerException if any fields are not provided
     */
    public Protos.TaskInfo build() {
        Objects.requireNonNull(offer);
        Objects.requireNonNull(instanceRequest);

        return buildTask();
    }

    @Nothrow
    private Protos.TaskInfo buildTask() {
        val portMapping = preparePortMapping();
        val containerInfo = buildContainer(portMapping);
        val commandInfo = buildCommand();

        return Protos.TaskInfo.newBuilder()
                .setTaskId(Instances.toTaskID(instanceRequest.getInstanceID()))
                .setName(Instances.toTaskName(instanceRequest.getInstanceID()))
                .setSlaveId(offer.getSlaveId())
                .setContainer(containerInfo)
                .setCommand(commandInfo)
                .addResources(getMemRequirement())
                .addResources(getCPURequirement())
                .addResources(getDiskRequirement())
                .addResources(getPortMapping(portMapping))
                .build();
    }

    @Nothrow
    private Protos.ContainerInfo.DockerInfo.PortMapping preparePortMapping() {
        val containerPort = getContainerPort();
        val hostPort = new ResourceDescriptor(offer.getResourcesList()).getPorts().get(0);

        return Protos.ContainerInfo.DockerInfo.PortMapping.newBuilder()
                .setContainerPort(containerPort)
                .setHostPort(hostPort)
                .setProtocol("tcp")
                .build();
    }

    @Nothrow
    private int getContainerPort() {
        switch (instanceRequest.getDefinition().getType()) {
            case CONFIG: return 27019;
            case ROUTER: return 27017;
            case DATA: return 27018;
        }

        throw new AssertionError("Bad instanceRequest type");
    }

    @Nothrow
    private Protos.CommandInfo buildCommand() {
        val args = Arrays.stream(instanceRequest.getDefinition().getArgs().split(" "))
                .map(this::expandEnvVar)
                .collect(Collectors.toList());

        return Protos.CommandInfo.newBuilder()
                .setShell(false)
                .addAllArguments(args)
                .build();
    }

    @Nothrow
    private String expandEnvVar(final String arg) {
        return arg.startsWith("$")
                ? instanceRequest.getEnv().getOrDefault(arg.substring(1), arg)
                : arg;
    }

    @Nothrow
    private Protos.Resource getDiskRequirement() {
        return Protos.Resource.newBuilder()
                .setName("disk")
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(Protos.Value.Scalar.newBuilder().setValue(definition.getDisk()))
                .build();
    }

    @Nothrow
    private Protos.Resource getCPURequirement() {
        return Protos.Resource.newBuilder()
                .setName("cpus")
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(Protos.Value.Scalar.newBuilder().setValue(definition.getCpus()))
                .build();
    }

    @Nothrow
    private Protos.Resource getMemRequirement() {
        return Protos.Resource.newBuilder()
                .setName("mem")
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(Protos.Value.Scalar.newBuilder().setValue(definition.getMemory()))
                .build();
    }

    @Nothrow
    private Protos.Resource getPortMapping(final Protos.ContainerInfo.DockerInfo.PortMapping mapping) {
        val port = mapping.getHostPort();
        val range = Protos.Value.Ranges.newBuilder()
                .addRange(Protos.Value.Range.newBuilder().setBegin(port).setEnd(port));

        return Protos.Resource.newBuilder()
                .setName("ports")
                .setType(Protos.Value.Type.RANGES)
                .setRanges(range)
                .build();
    }

    @Nothrow
    private Protos.ContainerInfo.Builder buildContainer(final Protos.ContainerInfo.DockerInfo.PortMapping portMapping) {
        val dockerBuilder = Protos.ContainerInfo.DockerInfo.newBuilder()
                .setImage(definition.getImage())
                .setNetwork(Protos.ContainerInfo.DockerInfo.Network.BRIDGE)
                .setForcePullImage(false)
                .addPortMappings(portMapping);

        attachDockerParameters(dockerBuilder);

        return Protos.ContainerInfo.newBuilder()
                .setType(Protos.ContainerInfo.Type.DOCKER)
                .setDocker(dockerBuilder);
    }

    @Nothrow
    private void attachDockerParameters(final Protos.ContainerInfo.DockerInfo.Builder builder) {
        final int cpuPeriod = 10_000;
        final int cpuQuota = (int)(cpuPeriod * definition.getCpus());

        builder.addParametersBuilder().setKey("cpu-period").setValue(String.valueOf(cpuPeriod)).build();
        builder.addParametersBuilder().setKey("cpu-quota").setValue(String.valueOf(cpuQuota)).build();
    }
}
