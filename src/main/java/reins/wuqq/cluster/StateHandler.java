package reins.wuqq.cluster;

import reins.wuqq.Cluster;
import reins.wuqq.ResourceStatusListener;
import reins.wuqq.model.ClusterState;

public interface StateHandler extends Cluster, ResourceStatusListener {
    void enter();
    void leave();

    ClusterState getState();
}
