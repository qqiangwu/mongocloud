package edu.reins.mongocloud.cluster.impl;

import edu.reins.mongocloud.Cluster;
import edu.reins.mongocloud.cluster.ClusterPipeline;
import edu.reins.mongocloud.cluster.ClusterStore;
import edu.reins.mongocloud.cluster.command.Command;
import edu.reins.mongocloud.cluster.command.CommandType;
import edu.reins.mongocloud.cluster.exception.ClusterUninitializedException;
import edu.reins.mongocloud.model.JobDefinition;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@Component
@Slf4j
@ThreadSafe
public class MongoCluster implements Cluster {
    @Autowired
    ClusterStore clusterStore;

    @Autowired
    ClusterPipeline clusterPipeline;

    @Override
    public boolean isInitialized() {
        return clusterStore.isInitialized();
    }

    private void ensureInitialized() {
        if (!isInitialized()) {
            throw new ClusterUninitializedException("badRequest");
        }
    }

    @Override
    public void clean() {
        ensureInitialized();

        val cmd = new Command(CommandType.CLEAN_CLUSTER);

        clusterPipeline.post(cmd);

        cmd.waitForFinish();
    }

    @Override
    public void submit(@Nonnull final JobDefinition jobDefinition) {
        ensureInitialized();

        val cmd = new Command(CommandType.CREATE_JOB, jobDefinition);

        clusterPipeline.post(cmd);

        cmd.waitForFinish();
    }
}