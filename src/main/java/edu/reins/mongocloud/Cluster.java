package edu.reins.mongocloud;

import edu.reins.mongocloud.model.JobDefinition;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Facade for both cluster access and mutation
 */
@ThreadSafe
public interface Cluster {
    boolean isInitialized();

    /**
     * Remove all jobs
     *
     * @throws edu.reins.mongocloud.cluster.exception.ClusterUninitializedException
     */
    void clean();

    /**
     *
     * @param jobDefinition
     * @throws edu.reins.mongocloud.cluster.exception.ClusterUninitializedException
     */
    void submit(@Nonnull JobDefinition jobDefinition);
}