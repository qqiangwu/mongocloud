package reins.wuqq.cluster;

import reins.wuqq.Cluster;
import reins.wuqq.ResourceStatusListener;
import reins.wuqq.model.ClusterState;

public interface StateHandler extends ResourceStatusListener {
    void enter();
    void leave();

    void scaleOutTo(int shardNumber);
    void scaleInTo(int shardNumber);

    ClusterState getState();
}
