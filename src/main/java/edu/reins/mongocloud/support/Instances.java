package edu.reins.mongocloud.support;

import edu.reins.mongocloud.model.Instance;
import org.apache.mesos.Protos;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class Instances {
    public static Optional<Protos.SlaveID> slaveID(@Nonnull final Instance instance) {
        return Optional.ofNullable(instance.getSlaveID() != null?
                Protos.SlaveID.newBuilder().setValue(instance.getSlaveID()).build():
                null);
    }

    public static Optional<Protos.TaskID> taskID(@Nonnull final Instance instance) {
        return Optional.ofNullable(instance.getTaskID() != null?
                Protos.TaskID.newBuilder().setValue(instance.getTaskID()).build():
                null);
    }

    public static String instanceID(@Nonnull final Protos.TaskStatus taskStatus) {
        return taskStatus.getTaskId().getValue();
    }
}
