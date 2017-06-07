package edu.reins.mongocloud.support;

import edu.reins.mongocloud.instance.Instance;
import org.apache.mesos.Protos;

public abstract class Tasks {
    public static String name(final Instance instance) {
        return String.format("%s.%s", instance.getName(), instance.getId());
    }

    public static Protos.TaskID id(final Instance instance) {
        return Protos.TaskID.newBuilder().setValue(instance.getTaskID()).build();
    }
}
