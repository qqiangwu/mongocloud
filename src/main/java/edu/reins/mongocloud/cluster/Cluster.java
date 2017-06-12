package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.model.ClusterID;

public interface Cluster extends Actor<ClusterEvent> {
    ClusterID getID();
    ClusterState getState();
}
