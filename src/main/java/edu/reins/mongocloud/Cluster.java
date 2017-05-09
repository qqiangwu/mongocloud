package edu.reins.mongocloud;

import edu.reins.mongocloud.model.ClusterDetail;

public interface Cluster {
    ClusterDetail getDetail();

    boolean isInitialized();

    void scaleOutTo(int shardNumber);
    void scaleInTo(int shardNumber);
}