package edu.reins.mongocloud.support;

import edu.reins.mongocloud.model.InstanceID;
import org.apache.mesos.Protos;

public abstract class Instances {
    public static Protos.TaskID toTaskID(final InstanceID id) {
        return Protos.TaskID.newBuilder()
                .setValue(id.getValue())
                .build();
    }

    public static String toTaskName(final InstanceID id) {
        return toTaskID(id).getValue();
    }
}
