package edu.reins.mongocloud.cluster.mongo;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface MongoMediator {
    void initRs(RsDefinition rsDefinition);
}
