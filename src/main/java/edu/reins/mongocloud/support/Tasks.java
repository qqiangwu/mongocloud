package edu.reins.mongocloud.support;

import edu.reins.mongocloud.model.Instance;
import org.apache.mesos.Protos;

import javax.annotation.Nonnull;

public abstract class Tasks {
    public static String name(@Nonnull final Instance instance) {
        return String.format("%s.%s", instance.getName(), instance.getId());
    }

    public static Protos.TaskID id(@Nonnull final Instance instance) {
        return Protos.TaskID.newBuilder().setValue(instance.getTaskID()).build();
    }
}
