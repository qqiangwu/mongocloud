package edu.reins.mongocloud;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.mongo.request.*;
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
    void rsRemove(RsRemoveRequest rsRemoveRequest);

    @Nothrow
    void remove(RemoveRequest removeRequest);

    @Nothrow
    void collect(Cluster cluster);
}
