package edu.reins.mongocloud.mongo.request;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceType;
import edu.reins.mongocloud.model.ClusterID;
import lombok.Value;
import lombok.val;

import java.util.List;

@Value
public final class RsRequest {
    private final ClusterID clusterID;
    private final boolean isConfig;
    private final List<Instance> members;

    private RsRequest(final ClusterID clusterID, final boolean isConfig, final List<Instance> members) {
        this.clusterID = clusterID;
        this.isConfig = isConfig;
        this.members = members;
    }

    public static RsRequest from(final Cluster cluster) {
        val isConfig = cluster.getInstances().get(0).getType().equals(InstanceType.CONFIG);
        val members = cluster.getInstances();

        return new RsRequest(cluster.getID(), isConfig, members);
    }
}
