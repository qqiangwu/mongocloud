package edu.reins.mongocloud.model;

import lombok.Value;

@Value
public final class ClusterDefinition {
    private final String id;
    private final int count;
}
