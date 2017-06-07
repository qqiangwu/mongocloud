package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.Actor;
import lombok.Data;

import javax.annotation.Nullable;
import java.io.Serializable;

@Data
public class Instance implements Serializable, Actor<InstanceEvent> {
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

    @Override
    public void handle(final InstanceEvent event) {

        // TODO
    }
}
