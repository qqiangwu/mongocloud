package edu.reins.mongocloud.instance;

import lombok.Value;

@Value
public final class InstanceHost {
    private final String slaveID;
    private final String ip;
    private final int port;
}
