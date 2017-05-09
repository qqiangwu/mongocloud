package edu.reins.mongocloud.support;

import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.InstanceState;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskID;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public abstract class InstanceUtil {
    private static final Predicate<Instance> IS_RUNNING = withState(InstanceState.RUNNING);
    private static final Predicate<Instance> NOT_RUNNING = instance -> !instance.getState().equals(InstanceState.RUNNING);

    public static final Predicate<Instance> withID(@Nonnull final Protos.TaskID taskID) {
        return instance -> instance.getId().equals(taskID.getValue());
    }

    public static final Predicate<Instance> withState(@Nonnull final InstanceState state) {
        return instance -> instance.getState().equals(state);
    }

    public static final Predicate<Instance> notRunning() {
        return NOT_RUNNING;
    }

    public static final Predicate<Instance> isRunning() {
        return IS_RUNNING;
    }

    public static final TaskID toTaskID(@Nonnull final Instance instance) {
        return TaskID.newBuilder().setValue(instance.getId()).build();
    }

    public static final String toReadable(@Nonnull final Instance instance) {
        return String.format("%s-%s:%s", instance.getName(), instance.getHostIP(), instance.getPort());
    }
}
