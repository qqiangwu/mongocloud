package edu.reins.mongocloud.cluster.processor;

import edu.reins.mongocloud.ResourceProvider;
import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.cluster.ClusterStore;
import edu.reins.mongocloud.cluster.Job;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.support.InstanceUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.mesos.Protos;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import java.util.Optional;

// TODO     no such job exception
@Slf4j
public abstract class JobAwarePipelineProcessor extends AbstractPipelineProcessor {
    @Autowired
    ClusterStore clusterStore;

    @Autowired
    ResourceProvider resourceProvider;

    public JobAwarePipelineProcessor(@Nonnull final ClusterState state) {
        super(state);
    }

    @Override
    protected ClusterState handleInstanceRunning(final Protos.TaskStatus payload) {
        val instanceID = InstanceUtil.instanceID(payload);

        getJobOrKill(instanceID)
                .ifPresent(job -> job.onInstanceRunning(instanceID, payload));

        return getState();
    }

    private Optional<Job> getJob(final String instanceID) {
        return clusterStore.getJob(instanceID);
    }

    private Optional<Job> getJobOrKill(final String instanceID) {
        val job = clusterStore.getJob(instanceID);

        if (!job.isPresent()) {
            resourceProvider.kill(instanceID);
        }

        return job;
    }

    @Override
    protected ClusterState handleInstanceLaunched(final Instance payload) {
        val instanceID = payload.getId();

        getJobOrKill(instanceID)
                .ifPresent(job -> job.onInstanceLaunched(instanceID, payload));

        return getState();
    }

    @Override
    protected ClusterState handleInstanceKilled(final Protos.TaskStatus payload) {
        val instanceID = InstanceUtil.instanceID(payload);

        // if not found, ignore it
        getJob(instanceID)
                .ifPresent(job -> job.onInstanceKilled(instanceID, payload));

        return getState();
    }

    @Override
    protected ClusterState handleInstanceFailed(final Protos.TaskStatus payload) {
        val instanceID = InstanceUtil.instanceID(payload);

        getJob(instanceID)
                .ifPresent(job -> job.onInstanceFailed(instanceID, payload));

        return getState();
    }

    @Override
    protected ClusterState handleInstanceError(final Protos.TaskStatus payload) {
        val instanceID = InstanceUtil.instanceID(payload);

        getJob(instanceID)
                .ifPresent(job -> job.onInstanceError(instanceID, payload));

        return getState();
    }
}