package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.fsm.ClusterAction;
import edu.reins.mongocloud.cluster.fsm.ClusterStateMachine;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceEvent;
import edu.reins.mongocloud.instance.InstanceEventType;
import edu.reins.mongocloud.instance.InstanceImpl;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@ToString
public class RouterCluster implements Cluster {
    private static final String ROUTER_SERVER_DEFINITION = "instance.router.definition";
    private static final String CONFIG_SERVER_PLACEHOLDER = "$CONFIG";

    private final ClusterID id;
    private final ClusterID parent;
    private final Context context;
    private final List<Instance> instances;
    private final ClusterStateMachine stateMachine;

    @Nothrow
    public RouterCluster(final Cluster parent, final Context context) {
        this.id = new ClusterID(String.format("%s-router", parent.getID().getValue()));
        this.parent = parent.getID();
        this.context = context;
        this.instances = new ArrayList<>();
        this.stateMachine = buildStateMachine();
    }

    @Nothrow
    private ClusterStateMachine buildStateMachine() {
        val builder = ClusterStateMachine.create();

        // init cluster: create and launch router instances
        builder.transition()
                .from(ClusterState.NEW).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.INIT)
                .perform(new OnInit());

        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.instancesNotFullyRunning(instances))
                .perform(new OnNewChildReady());

        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.RUNNING)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.allInstancesRunning(instances))
                .perform(Arrays.asList(new OnNewChildReady(), new OnRunning()));

        // done
        return builder.newStateMachine(ClusterState.NEW, this. context);
    }

    @Nothrow
    @Override
    public ClusterID getID() {
        return id;
    }

    @Nothrow
    @Override
    public ClusterState getState() {
        return stateMachine.getCurrentState();
    }

    @Nothrow
    @Override
    public List<Instance> getInstances() {
        return instances;
    }

    @Nothrow
    @Override
    public void handle(final ClusterEvent event) {
        stateMachine.fire(event.getType(), event);
    }

    /**
     * @throws IllegalStateException    if the cluster is not running
     */
    public RouterClusterMeta getMeta() {
        if (!getState().equals(ClusterState.RUNNING)) {
            throw new IllegalStateException("Router cluster is not running");
        }

        final List<String> members = instances.stream()
                .map(Instance::getHost)
                .map(host -> String.format("%s:%d", host.getIp(), host.getPort()))
                .collect(Collectors.toList());

        return new RouterClusterMeta(members);
    }

    private final class OnInit extends ClusterAction {

        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onInit(cluster: {}): register instances and send INIT", getID());

            final ConfigClusterMeta meta = event.getPayload(ConfigClusterMeta.class);

            createInstances(meta);
            registerInstances();
            initInstances();
        }

        @Nothrow
        private void createInstances(final ConfigClusterMeta meta) {
            final InstanceDefinition routerDefinition = createInstanceDefinition();
            final Map<String, String> env = Collections.singletonMap(Clusters.ENV_CONFIG, meta.toID());

            IntStream.range(0, 3)
                    .mapToObj(i -> new InstanceImpl(context, RouterCluster.this, i, routerDefinition, env))
                    .forEach(instances::add);
        }

        @Nothrow
        private void registerInstances() {
            instances.forEach(instance -> context.getInstances().put(instance.getID(), instance));
        }

        @Nothrow
        private void initInstances() {
            instances.forEach(instance ->
                    context.getEventBus().post(new InstanceEvent(InstanceEventType.INIT, instance.getID())));
        }

        @Nothrow
        private InstanceDefinition createInstanceDefinition() {
            return Clusters.loadDefinition(context.getEnv(), ROUTER_SERVER_DEFINITION);
        }
    }

    private final class OnNewChildReady extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onNewChildReady(cluster: {}, instanceID: {})",
                    getID(), event.getPayload(InstanceID.class));
        }
    }

    private final class OnRunning extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onRunning(cluster: {}, instance: {}): launch finished",
                    getID(), event.getPayload(InstanceID.class));

            context.getEventBus()
                    .post(new ClusterEvent(parent, ClusterEventType.CHILD_RUNNING, getID()));
        }
    }
}
