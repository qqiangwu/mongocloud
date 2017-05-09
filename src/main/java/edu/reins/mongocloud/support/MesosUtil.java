package edu.reins.mongocloud.support;

import org.apache.mesos.Protos;

import javax.annotation.Nonnull;

public abstract class MesosUtil {
    public static final Protos.TaskID toID(@Nonnull final String id) {
        return Protos.TaskID.newBuilder().setValue(id).build();
    }
}
