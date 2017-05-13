package edu.reins.mongocloud.cluster.processor;

import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.cluster.ClusterStore;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.support.annotation.SoftState;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.Protos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;

@Component
@NotThreadSafe
@SoftState
@Slf4j
public class RecoveringProcessor extends JobAwarePipelineProcessor {
    @Autowired
    ClusterStore clusterStore;

    private Map<String /* InstanceID */, Instance> unsyncInstances = new HashMap<>();

    public RecoveringProcessor() {
        super(ClusterState.RECOVERING);
    }

    @Override
    public void active() {
        syncInstances();
    }

    private void syncInstances() {
        clusterStore.getAllJobs()
                .stream()
                .flatMap(job -> job.getInstances().stream())
                .forEach(instance -> unsyncInstances.put(instance.getId(), instance));
    }

    @Override
    public void deactive() {
        unsyncInstances.clear();
    }

    @Override
    protected ClusterState handleClusterFailover() {
        log.info("failover");

        return ClusterState.CLOSING;
    }

    @Override
    protected ClusterState handleInstanceRunning(final Protos.TaskStatus payload) {
        super.handleInstanceRunning(payload);

        onAck(payload.getTaskId().getValue());

        return nextState();
    }

    @Override
    protected ClusterState handleInstanceLaunched(final Instance payload) {
        super.handleInstanceLaunched(payload);

        onAck(payload.getTaskID());

        return nextState();
    }

    @Override
    protected ClusterState handleInstanceKilled(final Protos.TaskStatus payload) {
        super.handleInstanceKilled(payload);

        onAck(payload.getTaskId().getValue());

        return nextState();
    }

    @Override
    protected ClusterState handleInstanceFailed(final Protos.TaskStatus payload) {
        super.handleInstanceFailed(payload);

        onAck(payload.getTaskId().getValue());

        return nextState();
    }

    @Override
    protected ClusterState handleInstanceError(final Protos.TaskStatus payload) {
        super.handleInstanceError(payload);

        onAck(payload.getTaskId().getValue());

        return nextState();
    }

    private void onAck(final String instanceID) {
        log.info("onAck(instance: {})", instanceID);

        unsyncInstances.remove(instanceID);
    }

    private ClusterState nextState() {
        return unsyncInstances.isEmpty()? ClusterState.RUNNING: ClusterState.RECOVERING;
    }
}