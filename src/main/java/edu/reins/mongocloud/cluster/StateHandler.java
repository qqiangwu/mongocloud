package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.model.ClusterState;
import edu.reins.mongocloud.ResourceStatusListener;

public interface StateHandler extends ResourceStatusListener {
    void enter();
    void leave();

    void scaleOutTo(int shardNumber);
    void scaleInTo(int shardNumber);

    ClusterState getState();
}
