package reins.wuqq;

import reins.wuqq.model.ClusterDetail;

public interface Cluster {
    ClusterDetail getDetail();

    void scaleOutTo(int shardNumber);
    void scaleInTo(int shardNumber);
}
