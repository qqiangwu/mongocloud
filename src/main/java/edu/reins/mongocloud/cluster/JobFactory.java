package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.model.JobDefinition;

import javax.annotation.Nonnull;

public interface JobFactory {
    Job createReplicaSets(@Nonnull JobDefinition jobDefinition);
    Job createShardCluster(@Nonnull JobDefinition jobDefinition);
}
