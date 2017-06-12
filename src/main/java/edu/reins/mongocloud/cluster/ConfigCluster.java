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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ConfigCluster implements Cluster {
    private static final String CONFIG_SERVER_DEFINITION = "instance.config.definition";
    private Optional<ConfigClusterMeta> meta = Optional.empty();

    private final ClusterID id;
    private final ClusterID parent;
    private final Context context;
    private final List<Instance> instances;
    private final StateMachineImpl stateMachine;

    public ConfigCluster(final Cluster parent, final Context context) {
        final InstanceDefinition configServerDef =
                context.getEnv().getProperty(CONFIG_SERVER_DEFINITION, InstanceDefinition.class);

        this.id = new ClusterID(String.format("%s::config", parent.getID().getValue()));
        this.parent = parent.getID();
        this.context = context;
        this.instances = IntStream.range(0, 3)
                .mapToObj(i -> new InstanceImpl(context, this, i, configServerDef))
                .collect(Collectors.toList());

        this.stateMachine = buildStateMachine();
    }

    private StateMachineImpl buildStateMachine() {
        val builder = StateMachineBuilderFactory
                .create(StateMachineImpl.class, ClusterState.class, ClusterEventType.class, ClusterEvent.class);

        // from NEW: start init
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
        return builder.newStateMachine(ClusterState.NEW);
    }

    @Override
    public ClusterID getID() {
        return id;
    }

    @Override
    public ClusterState getState() {
        return stateMachine.getCurrentState();
    }

    public ConfigClusterMeta getMeta() {
        return meta.orElseThrow(() -> new IllegalStateException("ConfigCluster is not running"));
    }

    @Override
    public void handle(final ClusterEvent event) {
        stateMachine.fire(event.getType(), event);
    }

    private class StateMachineImpl extends
            AbstractStateMachine<StateMachineImpl, ClusterState, ClusterEventType, ClusterEvent> {
        protected void entrySubmitted(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            log.info("entrySubmitted: register instances and send INIT");

            final EventBus eventBus = context.getEventBus();
            final Map<InstanceID, Instance> instanceContext = context.getInstances();

            instances.forEach(instance -> {
                instanceContext.put(instance.getID(), instance);
                eventBus.post(new InstanceEvent(InstanceEventType.INIT, instance.getID()));
            });
        }

        protected void transitFromSubmittedToRunning(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            log.info("transitFromSubmittedToRunning: launch finished");

            meta = Optional.of(createMeta());

            context.getEventBus().post(new ClusterEvent(parent, ClusterEventType.CHILD_RUNNING, getID()));
        }

        protected void onChildRunning(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            log.info("onChildRunning(instanceID: {})", c.getPayload(InstanceID.class));
        }

        private ConfigClusterMeta createMeta() {
            final String name = getID().getValue();
            final List<String> members = instances.stream()
                    .map(Instance::getHost)
                    .map(host -> String.format("%s:%d", host.getIp(), host.getPort()))
                    .collect(Collectors.toList());

            return new ConfigClusterMeta(name, members);
        }
    }
}
