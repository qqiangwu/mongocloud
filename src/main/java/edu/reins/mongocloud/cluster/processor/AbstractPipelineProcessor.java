package edu.reins.mongocloud.cluster.processor;

import edu.reins.mongocloud.cluster.ClusterPipelineProcessor;
import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.cluster.command.Command;
import edu.reins.mongocloud.cluster.command.CommandType;
import edu.reins.mongocloud.cluster.exception.BadCommandException;
import edu.reins.mongocloud.cluster.exception.IllegalTransitionException;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.JobDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.Protos;

import javax.annotation.Nonnull;

@Slf4j
public abstract class AbstractPipelineProcessor implements ClusterPipelineProcessor {
    private final ClusterState state;

    public AbstractPipelineProcessor(@Nonnull final ClusterState state) {
        this.state = state;
    }

    @Override
    public final ClusterState process(@Nonnull Command command) {
        switch (command.getType()) {
            case CLUSTER_SETUP: return handleClusterSetup();
            case CLUSTER_DESTROYED: return handleClusterDestroyed();
            case CLUSTER_FAILOVER: return handleClusterFailover();
            case INSTANCE_LAUNCHED: return handleInstanceLaunched(command.getPayload(Instance.class));
            case INSTANCE_RUNNING: return handleInstanceRunning(command.getPayload(Protos.TaskStatus.class));
            case INSTANCE_FAILED: return handleInstanceFailed(command.getPayload(Protos.TaskStatus.class));
            case INSTANCE_KILLED: return handleInstanceKilled(command.getPayload(Protos.TaskStatus.class));
            case INSTANCE_ERROR: return handleInstanceError(command.getPayload(Protos.TaskStatus.class));
            case CREATE_JOB: return handleJobCreation(command.getPayload(JobDefinition.class));
        }

        throw new BadCommandException(String.format("command: %s", command));
    }

    @Override
    public void active() {
        log.info("active(state: {})", getState());
    }

    @Override
    public void deactive() {
        log.info("deactive(state: {})", getState());
    }

    @Override
    public final ClusterState getState() {
        return this.state;
    }

    protected ClusterState handleClusterFailover() {
        throw new IllegalTransitionException(state, CommandType.CLUSTER_FAILOVER);
    }

    protected ClusterState handleClusterDestroyed() {
        throw new IllegalTransitionException(state, CommandType.CLUSTER_DESTROYED);
    }

    protected ClusterState handleClusterSetup() {
        throw new IllegalTransitionException(state, CommandType.CLUSTER_SETUP);
    }

    protected ClusterState handleInstanceRunning(final Protos.TaskStatus payload) {
        throw new IllegalTransitionException(state, CommandType.INSTANCE_RUNNING);
    }

    protected ClusterState handleInstanceLaunched(final Instance payload) {
        throw new IllegalTransitionException(state, CommandType.INSTANCE_LAUNCHED);
    }

    protected ClusterState handleInstanceKilled(final Protos.TaskStatus payload) {
        throw new IllegalTransitionException(state, CommandType.INSTANCE_KILLED);
    }

    protected ClusterState handleInstanceFailed(final Protos.TaskStatus payload) {
        throw new IllegalTransitionException(state, CommandType.INSTANCE_FAILED);
    }

    protected ClusterState handleInstanceError(final Protos.TaskStatus payload) {
        throw new IllegalTransitionException(state, CommandType.INSTANCE_ERROR);
    }

    protected ClusterState handleJobCreation(final JobDefinition payload) {
        throw new IllegalTransitionException(state, CommandType.CREATE_JOB);
    }
}
