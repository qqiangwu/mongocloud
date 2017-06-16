package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.support.annotation.Nothrow;

import java.util.List;

public interface Cluster extends Actor<ClusterEvent> {
    ClusterID getID();
    ClusterState getState();
    List<Instance> getInstances();

    @Nothrow
    default ClusterReport getReport() {
        return ClusterReport.builder()
                .shardCount(getInstances().size())
                .storageInMB(0)
                .build();
    }
}
