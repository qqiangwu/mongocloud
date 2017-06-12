package edu.reins.mongocloud.model;

import edu.reins.mongocloud.instance.InstanceType;
import lombok.Value;
import lombok.experimental.Wither;

@Value
public final class InstanceDefinition {
    private InstanceType type;
    private final String image;
    @Wither
    private final String args;

    private final double cpus;
    private final long memory;
    private final long disk;
}
