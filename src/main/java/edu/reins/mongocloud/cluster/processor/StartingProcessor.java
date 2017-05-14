package edu.reins.mongocloud.cluster.processor;

import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.cluster.ClusterStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.concurrent.NotThreadSafe;

@Component
@NotThreadSafe
@Slf4j
public class StartingProcessor extends AbstractPipelineProcessor {
    @Autowired
    ClusterStore clusterStore;

    public StartingProcessor() {
        super(ClusterState.STARTING);
    }

    @Override
    protected ClusterState handleClusterDestroyed() {
        log.info("clusterDestroyed: please restart the framework");

        clusterStore.clear();

        return ClusterState.CLOSING;
    }

    @Override
    protected ClusterState handleClusterSetup() {
        log.info("clusterSetup");

        return hasInstances()? ClusterState.RECOVERING: ClusterState.RUNNING;
    }

    private boolean hasInstances() {
        return clusterStore.getAllJobs().stream().anyMatch(j -> !j.getInstances().isEmpty());
    }
}
