package edu.reins.mongocloud.cluster.mongo;

import edu.reins.mongocloud.model.Instance;

public interface MongoCommander {
    void addShardToCluster(Instance router, Instance shard);

    boolean removeShardFromCluster(Instance router, Instance shard);

    int getStorageInMB(Instance router);
}
