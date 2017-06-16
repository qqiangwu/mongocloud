package edu.reins.mongocloud.cluster.mongo;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterReport;
import edu.reins.mongocloud.support.annotation.Nothrow;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface MongoMediator {
    @Nothrow
    void initRs(RsDefinition rsDefinition);

    @Nothrow
    void collect(Cluster cluster);
}
