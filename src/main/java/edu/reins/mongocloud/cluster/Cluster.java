package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.ClusterID;

import java.util.List;

public interface Cluster extends Actor<ClusterEvent> {
    ClusterID getID();
    ClusterState getState();
    List<Instance> getInstances();
}
