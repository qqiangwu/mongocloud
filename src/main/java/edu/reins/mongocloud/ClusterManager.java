package edu.reins.mongocloud;

import edu.reins.mongocloud.clustermanager.exception.ClusterIDConflictException;
import edu.reins.mongocloud.model.ClusterDefinition;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Facade for both cluster access and mutation
 */
@ThreadSafe
public interface ClusterManager {
    boolean isInitialized();

    /**
     *
     * @param clusterDefinition
     * @throws ClusterIDConflictException if the cluster name is already exists
     * @throws IllegalStateException  if the clusterManager is not initialized
     */
    void createCluster(ClusterDefinition clusterDefinition) throws ClusterIDConflictException;
}