package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.model.ClusterID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ShardedCluster implements Cluster {
    private final StateMachineImpl stateMachine;

    private final Context context;
    private final ClusterID id;
    private final ClusterDefinition definition;

    private final ConfigCluster configCluster;
    private final RouterCluster routerCluster;
    private final List<ReplicaCluster> shards;

    public ShardedCluster(ClusterID clusterID, ClusterDefinition clusterDefinition, Context context) {
        this.context = context;

        id = clusterID;
        definition = clusterDefinition;

        configCluster = new ConfigCluster(this, context);
        routerCluster = new RouterCluster(this, context);
        shards = IntStream.range(0, clusterDefinition.getCount())
                .mapToObj(i -> new ReplicaCluster(this, i, context))
                .collect(Collectors.toList());

        stateMachine = buildStateMachine();
    }

    private StateMachineImpl buildStateMachine() {
        val builder = StateMachineBuilderFactory
                .create(StateMachineImpl.class, ClusterState.class, ClusterEventType.class, ClusterEvent.class);

        // from NEW: start init
        builder.transition()
                .from(ClusterState.NEW).to(ClusterState.WAIT_CONFIG)
                .on(ClusterEventType.INIT);

        // from WAIT_CONFIG: wait config cluster to be done
        builder.transition()
                .from(ClusterState.WAIT_CONFIG).to(ClusterState.WAIT_ROUTER)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.eventIsFrom(configCluster));

        // from WAIT_ROUTER: wait router cluster to be done
        builder.transition()
                .from(ClusterState.WAIT_ROUTER).to(ClusterState.WAIT_SHARDS)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.eventIsFrom(routerCluster));

        // from WAIT_SHARDS: wait all shards to be done
        builder.transition()
                .from(ClusterState.WAIT_SHARDS).to(ClusterState.WAIT_SHARDS)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.shardsNotFullyRunning(shards));

        builder.transition()
                .from(ClusterState.WAIT_SHARDS).to(ClusterState.RUNNING)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.allShardsRunning(shards));

        // done
        return builder.newStateMachine(ClusterState.NEW, this);
    }

    @Override
    public ClusterID getID() {
        return id;
    }

    @Override
    public ClusterState getState() {
        return stateMachine.getCurrentState();
    }

    @Override
    public void handle(final ClusterEvent event) {
        stateMachine.fire(event.getType(), event);
    }

    // TODO     添加Submitted状态中config/router/replica的失效问题
    // TODO     添加由Running向Died的转换
    // TODO     从Context中移除Cluster
    @AllArgsConstructor
    private class StateMachineImpl extends
            AbstractStateMachine<StateMachineImpl, ClusterState, ClusterEventType, ClusterEvent> {
        protected void onInit(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent ctx) {
            log.info("onInit: register config/router/shards");

            context.getClusters().put(configCluster.getID(), configCluster);
            context.getClusters().put(routerCluster.getID(), routerCluster);

            shards.forEach(shard -> context.getClusters().put(shard.getID(), shard));
        }

        protected void entryWaitConfig(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent ctx) {
            log.info("entryWaitConfig: init config cluster");

            notifyChild(configCluster, ClusterEventType.INIT);
        }

        protected void entryWaitRouter(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent ctx) {
            log.info("entryWaitRouter: init router cluster");

            notifyChild(routerCluster, ClusterEventType.INIT, configCluster.getMeta());
        }

        protected void entryWaitShards(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent ctx) {
            if (from != to) {
                log.info("entryWaitShards");

                shards.forEach(shard -> notifyChild(shard, ClusterEventType.INIT, routerCluster.getMeta()));
            }
        }

        protected void onChildRunning(
                ClusterState from, ClusterState to, ClusterEventType event, ClusterEvent ctx) {
            log.info("onChildRunning(child: {})", ctx.getPayload(ClusterID.class));
        }

        private void notifyChild(final Cluster child, final ClusterEventType eventType) {
            context.getEventBus().post(new ClusterEvent(child.getID(), eventType));
        }

        private void notifyChild(final Cluster child, final ClusterEventType eventType, final Object payload) {
            context.getEventBus().post(new ClusterEvent(child.getID(), eventType, payload));
        }
    }
}