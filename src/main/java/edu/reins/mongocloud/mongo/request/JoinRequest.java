package edu.reins.mongocloud.mongo.request;

import edu.reins.mongocloud.cluster.RouterClusterMeta;
import edu.reins.mongocloud.model.ClusterID;
import lombok.Value;

@Value
public final class JoinRequest {
    private final ClusterID cluster;
    private final RouterClusterMeta router;
    private final ClusterID participant;
}
