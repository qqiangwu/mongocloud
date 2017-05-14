package edu.reins.mongocloud.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.io.Serializable;

@Data
public class Instance implements Serializable {
    private InstanceType type;
    private InstanceState state = InstanceState.SUBMITTED;

    @Nullable
    private String slaveID;

    @Nullable
    private String taskID;

    private String name;
    private String id;
    private String image;
    private String args;

    private double cpus;
    private long memory;
    private long disk;

    @Nullable
    private String hostIP;

    @Nullable
    private Integer port;
}
