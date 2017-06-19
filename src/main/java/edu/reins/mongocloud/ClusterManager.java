package edu.reins.mongocloud;

import edu.reins.mongocloud.clustermanager.exception.ClusterIDConflictException;
import edu.reins.mongocloud.clustermanager.exception.ClusterNotFoundException;
import edu.reins.mongocloud.clustermanager.exception.OperationNotAllowedException;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.model.ClusterID;

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
     *
     * @throws IllegalStateException  if the clusterManager is not initialized
     */
    void createCluster(ClusterDefinition clusterDefinition) throws ClusterIDConflictException;

    /**
     * @param clusterID
     *
     * @throws ClusterNotFoundException if the id is not found
     * @throws OperationNotAllowedException if the cluster is in bad state
     *
     * @throws IllegalStateException  if the clusterManager is not initialized
     */
    void scaleOut(ClusterID clusterID) throws ClusterNotFoundException, OperationNotAllowedException;

    /**
     * @param clusterID
     *
     * @throws ClusterNotFoundException if the id is not found
     * @throws OperationNotAllowedException if the cluster is in bad state
     *
     * @throws IllegalStateException  if the clusterManager is not initialized
     */
    void scaleIn(ClusterID clusterID) throws ClusterNotFoundException, OperationNotAllowedException;
}