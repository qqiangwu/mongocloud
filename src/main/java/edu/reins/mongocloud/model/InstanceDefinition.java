package edu.reins.mongocloud.model;

import edu.reins.mongocloud.instance.InstanceType;
import lombok.Data;

@Data
public final class InstanceDefinition {
    private InstanceType type;
    private String image;
    private String args;

    private double cpus;
    private long memory;
    private long disk;
}
