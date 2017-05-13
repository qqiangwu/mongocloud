package edu.reins.mongocloud.cluster.processor;

import edu.reins.mongocloud.ResourceProvider;
import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.cluster.ClusterStore;
import edu.reins.mongocloud.cluster.JobFactory;
import edu.reins.mongocloud.cluster.exception.JobNameConflictException;
import edu.reins.mongocloud.model.JobDefinition;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.concurrent.NotThreadSafe;

@Component
@NotThreadSafe
public class RunningProcessor extends JobAwarePipelineProcessor {
    @Autowired
    JobFactory jobFactory;

    @Autowired
    ResourceProvider resourceProvider;

    @Autowired
    ClusterStore clusterStore;

    public RunningProcessor() {
        super(ClusterState.RUNNING);
    }

    @Override
    public void active() {
        clusterStore.initialize();
    }

    @Override
    public void deactive() {
        clusterStore.stop();
    }

    /**
     * Persist first, then launch the instances.
     */
    @Override
    protected ClusterState handleJobCreation(final JobDefinition jobDefinition) {
        ensureNameUsable(jobDefinition.getName());

        val job = jobFactory.createReplicaSets(jobDefinition);

        job.getInstances().forEach(resourceProvider::launch);

        return getState();
    }

    private void ensureNameUsable(final String name) {
        clusterStore.getAllJobs()
                .stream()
                .filter(j -> name.equals(j.getDefinition().getName()))
                .findAny()
                .orElseThrow(() -> new JobNameConflictException(name));
    }
}
