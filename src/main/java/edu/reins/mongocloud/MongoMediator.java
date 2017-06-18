package edu.reins.mongocloud;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.RouterClusterMeta;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.mongo.request.JoinRequest;
import edu.reins.mongocloud.mongo.request.RsRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface MongoMediator {
    @Nothrow
    void initRs(RsRequest rsRequest);

    @Nothrow
    void join(JoinRequest joinRequest);

    @Nothrow
    void collect(Cluster cluster);
}
