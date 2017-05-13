package edu.reins.mongocloud.cluster.impl;

import edu.reins.mongocloud.ResourceStatusListener;
import edu.reins.mongocloud.cluster.ClusterPipeline;
import edu.reins.mongocloud.cluster.command.Command;
import edu.reins.mongocloud.cluster.command.CommandType;
import edu.reins.mongocloud.model.Instance;
import org.apache.mesos.Protos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

@Component
@NotThreadSafe
public class ResourceStatusListenerImpl implements ResourceStatusListener {
    @Autowired
    ClusterPipeline clusterPipeline;

    @Override
    public void onPlatformPrepared() {
        clusterPipeline.post(new Command(CommandType.CLUSTER_SETUP));
    }

    @Override
    public void onClusterDestroyed() {
        clusterPipeline.post(new Command(CommandType.CLUSTER_DESTROYED));
    }

    @Override
    public void onFailover() {
        clusterPipeline.post(new Command(CommandType.CLUSTER_FAILOVER));
    }

    @Override
    public void onInstanceLaunched(@Nonnull final Instance instance) {

    }

    @Override
    public void onInstanceRunning(@Nonnull final Protos.TaskStatus status) {

    }

    @Override
    public void onInstanceFailed(@Nonnull final Protos.TaskStatus status) {

    }

    @Override
    public void onInstanceKilled(@Nonnull final Protos.TaskStatus status) {

    }

    @Override
    public void onInstanceError(@Nonnull final Protos.TaskStatus status) {

    }
}
