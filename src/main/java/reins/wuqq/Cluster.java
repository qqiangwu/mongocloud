package reins.wuqq;

import reins.wuqq.model.ClusterDetail;

public interface Cluster {
    ClusterDetail getDetail();

    boolean isInitialized();

    void scaleOutTo(int shardNumber);
    void scaleInTo(int shardNumber);
}