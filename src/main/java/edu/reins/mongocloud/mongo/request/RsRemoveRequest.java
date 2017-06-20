package edu.reins.mongocloud.mongo.request;

import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceID;
import lombok.Value;

@Value
public class RsRemoveRequest {
    private final ClusterID cluster;
    private final InstanceID instance;
}
