package edu.reins.mongocloud.cluster;

import lombok.Value;

import java.util.List;

@Value
public final class RouterClusterMeta {
    private final List<String> members;
}