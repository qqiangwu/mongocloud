package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.model.InstanceID;
import lombok.experimental.UtilityClass;
import org.apache.mesos.Protos;

@UtilityClass
public final class Instances {
    public static Protos.TaskID toTaskID(final InstanceID id) {
        return Protos.TaskID.newBuilder()
                .setValue(id.getValue())
                .build();
    }

    public static String toTaskName(final InstanceID id) {
        return toTaskID(id).getValue();
    }

    public static String toAddress(final Instance instance) {
        final InstanceHost host = instance.getHost();

        return String.format("%s:%d", host.getIp(), host.getPort());
    }
}
