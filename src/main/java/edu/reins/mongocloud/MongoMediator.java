package edu.reins.mongocloud;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.mongo.request.JoinRequest;
import edu.reins.mongocloud.mongo.request.RemoveRequest;
import edu.reins.mongocloud.mongo.request.RsJoinRequest;
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
    void rsJoin(RsJoinRequest rsJoinRequest);

    @Nothrow
    void remove(RemoveRequest removeRequest);

    @Nothrow
    void collect(Cluster cluster);
}
