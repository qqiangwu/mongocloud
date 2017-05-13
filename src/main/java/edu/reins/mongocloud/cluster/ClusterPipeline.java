package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.cluster.command.Command;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface ClusterPipeline {
    void post(@Nonnull Command command);
}
