package reins.wuqq;

import reins.wuqq.model.ClusterDetail;

public interface Cluster {
    ClusterDetail getDetail();

    void scaleOutTo(long shardNumber);
    void scaleInTo(long shardNumber);
}
