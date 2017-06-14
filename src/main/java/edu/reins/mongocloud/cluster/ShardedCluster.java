package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.fsm.ClusterAction;
import edu.reins.mongocloud.cluster.fsm.ClusterStateMachine;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO     添加Submitted状态中config/router/replica的失效问题
// TODO     添加由Running向Died的转换
// TODO     从Context中移除Cluster
// TODO     将Shards加入集群
@Slf4j
public class ShardedCluster implements Cluster {
    private final ClusterStateMachine stateMachine;

    private final Context context;
    private final ClusterID id;
    private final ClusterDefinition definition;

    private final ConfigCluster configCluster;
    private final RouterCluster routerCluster;
    private final List<ReplicaCluster> shards;

    @Nothrow
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

    @Nothrow
    private ClusterStateMachine buildStateMachine() {
        val builder = ClusterStateMachine.create();

        // from NEW: start init
        builder.transition()
                .from(ClusterState.NEW).to(ClusterState.WAIT_CONFIG)
                .on(ClusterEventType.INIT)
                .perform(new OnInit());

        // from WAIT_CONFIG: wait config cluster to be done
        builder.transition()
                .from(ClusterState.WAIT_CONFIG).to(ClusterState.WAIT_ROUTER)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.eventIsFrom(configCluster))
                .perform(new OnConfigClusterReady());

        // from WAIT_ROUTER: wait router cluster to be done
        builder.transition()
                .from(ClusterState.WAIT_ROUTER).to(ClusterState.WAIT_SHARDS)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.eventIsFrom(routerCluster))
                .perform(new OnRouterClusterReady());

        // from WAIT_SHARDS: wait all shards to be done
        builder.transition()
                .from(ClusterState.WAIT_SHARDS).to(ClusterState.WAIT_SHARDS)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.shardsNotFullyRunning(shards))
                .perform(new OnChildReady());

        builder.transition()
                .from(ClusterState.WAIT_SHARDS).to(ClusterState.RUNNING)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.allShardsRunning(shards))
                .perform(new OnChildReady());

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
        return shards.stream()
                .flatMap(shard -> shard.getInstances().stream())
                .collect(Collectors.toList());
    }

    @Nothrow
    @Override
    public void handle(final ClusterEvent event) {
        stateMachine.fire(event.getType(), event);
    }


    private final class OnInit extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onInit(cluster: {}): sharded cluster init", getID());

            LOG.info("> onInit(cluster: {}): register config cluster", getID());
            context.getClusters().put(configCluster.getID(), configCluster);

            LOG.info("> onInit(cluster: {}): register router cluster", getID());
            context.getClusters().put(routerCluster.getID(), routerCluster);

            LOG.info("> onInit(cluster: {}): register shards", getID());
            shards.forEach(shard -> context.getClusters().put(shard.getID(), shard));

            LOG.info("> onInit(cluster: {}): init config cluster", getID());
            notifyChild(configCluster, ClusterEventType.INIT);
        }
    }

    private final class OnConfigClusterReady extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onConfigReady(cluster: {}): prepare to init routers", getID());

            notifyChild(routerCluster, ClusterEventType.INIT, configCluster.getMeta());
        }
    }

    private final class OnRouterClusterReady extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onRouterReady(cluster: {}): init shards", getID());

            shards.forEach(shard -> notifyChild(shard, ClusterEventType.INIT));
        }
    }

    private final class OnChildReady extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onNewChildReady(cluster: {}, child: {})", getID(), event.getPayload(ClusterID.class));
        }
    }

    @Nothrow
    private void notifyChild(final Cluster child, final ClusterEventType eventType) {
        context.getEventBus().post(new ClusterEvent(child.getID(), eventType));
    }

    @Nothrow
    private void notifyChild(final Cluster child, final ClusterEventType eventType, final Object payload) {
        context.getEventBus().post(new ClusterEvent(child.getID(), eventType, payload));
    }
}