package edu.reins.mongocloud.impl;

import edu.reins.mongocloud.ClusterManager;
import edu.reins.mongocloud.MongoCloudInitializer;
import edu.reins.mongocloud.clustermanager.exception.ClusterIDConflictException;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * TODO     remove this, it's for testing purpose only
 */
@Slf4j
@Component
public class SubmitInitializer implements MongoCloudInitializer {
    @Autowired
    private TaskExecutor executorService;

    @Autowired
    private ClusterManager clusterManager;

    @Override
    public void initialize(final ApplicationContext context) {
        executorService.execute(this::exec);
    }

    private void exec() {
        LOG.info("submitTask");

        waitInitialization();

        if (clusterManager.isInitialized()) {
            launchCluster();
        }
    }

    @Nothrow
    private void waitInitialization() {
        val duration = Duration.ofSeconds(1).toMillis();

        while (!clusterManager.isInitialized() && !Thread.interrupted()) {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Nothrow
    private void launchCluster() {
        try {
            clusterManager.createCluster(new ClusterDefinition("test", 1));
        } catch (ClusterIDConflictException e) {
            throw new AssertionError(e);
        }
    }
}
