package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.mongo.MongoEvent;
import edu.reins.mongocloud.cluster.mongo.MongoEventType;
import edu.reins.mongocloud.cluster.mongo.RsDefinition;
import edu.reins.mongocloud.instance.*;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import java.util.Collections;
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

    @Nothrow
    public ConfigCluster(final Cluster parent, final Context context) {
        final InstanceDefinition configServerDef =
                context.getEnv().getProperty(CONFIG_SERVER_DEFINITION, InstanceDefinition.class);
        final Map<String, String> env = Collections.singletonMap(Clusters.ENV_RS, getID().getValue());

        this.id = new ClusterID(String.format("%s::config", parent.getID().getValue()));
        this.parent = parent.getID();
        this.context = context;
        this.instances = IntStream.range(0, 3)
                .mapToObj(i -> new InstanceImpl(context, this, i, configServerDef, env))
                .collect(Collectors.toList());

        this.stateMachine = buildStateMachine();
    }

    @Nothrow
    private StateMachineImpl buildStateMachine() {
        val builder = StateMachineBuilderFactory
                .create(StateMachineImpl.class, ClusterState.class, ClusterEventType.class, ClusterEvent.class);

        // from NEW: start init
        builder.transition()
                .from(ClusterState.NEW).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.INIT);

        // from SUBMITTED: wait all instances to be done
        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.instancesNotFullyRunning(instances));

        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.INIT_RS)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.allInstancesRunning(instances));

        // from INIT_RS: wait all instances to join the replica set
        builder.transition()
                .from(ClusterState.INIT_RS).to(ClusterState.RUNNING)
                .on(ClusterEventType.RS_INITED);

        builder.transition()
                .from(ClusterState.INIT_RS).to(ClusterState.FAILED)
                .on(ClusterEventType.FAIL);

        // done
        return builder.newStateMachine(ClusterState.NEW, this, context);
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

    /**
     * @throws IllegalStateException if the cluster is not running
     */
    public ConfigClusterMeta getMeta() {
        return meta.orElseThrow(() -> new IllegalStateException("ConfigCluster is not running"));
    }

    @Nothrow
    @Override
    public void handle(final ClusterEvent event) {
        stateMachine.fire(event.getType(), event);
    }

    @AllArgsConstructor
    private static class StateMachineImpl extends
            AbstractStateMachine<StateMachineImpl, ClusterState, ClusterEventType, ClusterEvent> {
        private final ConfigCluster self;
        private final Context context;

        @Nothrow
        protected void entrySubmitted(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            LOG.info("entrySubmitted: register instances and send INIT");

            final EventBus eventBus = context.getEventBus();
            final Map<InstanceID, Instance> instanceContext = context.getInstances();

            self.instances.forEach(instance -> {
                instanceContext.put(instance.getID(), instance);

                eventBus.post(new InstanceEvent(InstanceEventType.INIT, instance.getID()));
            });
        }

        @Nothrow
        protected void transitFromSubmittedToInitRs(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            LOG.info("transitFromSubmittedToInitRs(id: {}): launch finished", self.getID());

            final RsDefinition rs = RsDefinition.from(self);

            context.getEventBus().post(new MongoEvent(MongoEventType.INIT_RS, self.getID(), rs));
        }

        @Nothrow
        protected void transitFromInitRsToRunning(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            LOG.info("transitFromInitRsToRunning(id: {}): launch finished", self.getID());

            self.meta = Optional.of(createMeta());

            context.getEventBus()
                    .post(new ClusterEvent(self.parent, ClusterEventType.CHILD_RUNNING, self.getID()));
        }

        @Nothrow
        protected void transitFromAnyToFailed(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            LOG.info("transitFromAnyToFailed(id: {}, from: {}, msg: {})",
                    self.getID(), from, c.getPayload(String.class));
        }

        @Nothrow
        protected void onChildRunning(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent c) {
            LOG.info("onChildRunning(instanceID: {})", c.getPayload(InstanceID.class));
        }

        @Nothrow
        private ConfigClusterMeta createMeta() {
            final String name = self.getID().getValue();
            final List<String> members = self.instances.stream()
                    .map(Instances::toAddress)
                    .collect(Collectors.toList());

            return new ConfigClusterMeta(name, members);
        }
    }
}
