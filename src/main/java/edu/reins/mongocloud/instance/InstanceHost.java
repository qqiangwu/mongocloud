package edu.reins.mongocloud.instance;

import lombok.Value;
import org.apache.mesos.Protos;

@Value
public final class InstanceHost {
    private final Protos.SlaveID slaveID;
    private final String ip;
    private final int port;
}
