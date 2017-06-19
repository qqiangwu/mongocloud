package edu.reins.mongocloud.mongo.request;

import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceID;
import lombok.Value;

@Value
public final class RsJoinRequest {
    private final ClusterID cluster;
    private final InstanceID instance;
}
