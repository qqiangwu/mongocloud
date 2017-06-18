package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.model.ClusterID;
import lombok.Value;

import java.util.List;

@Value
public final class RouterClusterMeta {
    private final ClusterID cluster;
    private final List<String> members;
}