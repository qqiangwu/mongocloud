package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceEvent;
import edu.reins.mongocloud.instance.InstanceEventType;
import edu.reins.mongocloud.instance.InstanceImpl;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class RouterCluster implements Cluster {
    private static final String ROUTER_SERVER_DEFINITION = "instance.router.definition";
    private static final String CONFIG_SERVER_PLACEHOLDER = "$CONFIG";

    private final ClusterID id;
    private final ClusterID parent;
    private final Context context;
    private final List<Instance> instances;
    private final StateMachineImpl stateMachine;

    @Nothrow
    public RouterCluster(final Cluster parent, final Context context) {
        this.id = new ClusterID(String.format("%s::router", parent.getID().getValue()));
        this.parent = parent.getID();
        this.context = context;
        this.instances = new ArrayList<>();
        this.stateMachine = buildStateMachine();
    }

    @Nothrow
    private StateMachineImpl buildStateMachine() {
        val builder = StateMachineBuilderFactory
                .create(StateMachineImpl.class, ClusterState.class, ClusterEventType.class, ClusterEvent.class);

        // from NEW to INIT: launch all routers
        builder.transition()
                .from(ClusterState.NEW).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.INIT);

        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.instancesNotFullyRunning(instances));

        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.RUNNING)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.allInstancesRunning(instances));

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

    @AllArgsConstructor
    private static class StateMachineImpl extends
            AbstractStateMachine<StateMachineImpl, ClusterState, ClusterEventType, ClusterEvent> {
        private final RouterCluster self;
        private final Context context;

        @Nothrow
        protected void transmitFromNewToSubmittedOnInit(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            LOG.info("entrySubmitted: register instances and send INIT");

            final EventBus eventBus = context.getEventBus();
            final Map<InstanceID, Instance> instanceContext = context.getInstances();
            final ConfigClusterMeta meta = c.getPayload(ConfigClusterMeta.class);

            self.instances.addAll(createInstances(meta));
            self.instances.forEach(instance -> {
                instanceContext.put(instance.getID(), instance);

                eventBus.post(new InstanceEvent(InstanceEventType.INIT, instance.getID()));
            });
        }

        @Nothrow
        private List<Instance> createInstances(final ConfigClusterMeta meta) {
            final InstanceDefinition routerDefinition = createInstanceDefinition();
            final Map<String, String> env = Collections.singletonMap(Clusters.ENV_CONFIG, meta.toID());

            return IntStream.range(0, 3)
                    .mapToObj(i -> new InstanceImpl(context, self, i, routerDefinition, env))
                    .collect(Collectors.toList());
        }

        @Nothrow
        private InstanceDefinition createInstanceDefinition() {
            return context.getEnv().getProperty(ROUTER_SERVER_DEFINITION, InstanceDefinition.class);
        }

        @Nothrow
        protected void transitFromSubmittedToRunning(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            LOG.info("transitFromSubmittedToRunning: launch finished");

            context.getEventBus()
                    .post(new ClusterEvent(self.parent, ClusterEventType.CHILD_RUNNING, self.getID()));
        }

        @Nothrow
        protected void onChildRunning(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            LOG.info("onChildRunning(instanceID: {})", c.getPayload(InstanceID.class));
        }
    }
}
