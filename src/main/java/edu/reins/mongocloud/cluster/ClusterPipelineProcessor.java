package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.cluster.command.Command;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface ClusterPipelineProcessor {
    void active();
    void deactive();

    ClusterState getState();
    ClusterState process(@Nonnull Command command);
}
