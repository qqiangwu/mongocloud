package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.fsm.ClusterAction;
import edu.reins.mongocloud.cluster.fsm.ClusterStateMachine;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.mongo.request.JoinRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.squirrelframework.foundation.fsm.AnonymousCondition;
import org.squirrelframework.foundation.fsm.Condition;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO     添加Submitted状态中config/router/replica的失效问题
// TODO     添加由Running向Died的转换
// TODO     从Context中移除Cluster
// TODO     将Shards加入集群
// TODO     SCALE_IN/SCALE_OUT
@Slf4j
public class ShardedCluster implements Cluster {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final ClusterStateMachine stateMachine;

    private final Context context;
    private final ClusterID id;
    private final ClusterDefinition definition;

    private final ConfigCluster configCluster;
    private final RouterCluster routerCluster;
    private final List<ReplicaCluster> shards;

    private final ClusterReport report;
    private int joined = 0;
    private int idSeen = 0;

    private final Condition<ClusterEvent> allShardsJoined = new AnonymousCondition<ClusterEvent>() {
        @Override
        public boolean isSatisfied(ClusterEvent context) {
            return joined + 1 == shards.size();
        }
    };

    private final Condition<ClusterEvent> notAllShardsJoined = new AnonymousCondition<ClusterEvent>() {
        @Override
        public boolean isSatisfied(ClusterEvent context) {
            return joined + 1 < shards.size();
        }
    };

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

        idSeen = shards.size();
        report = new ClusterReport(0, clusterDefinition.getCount());

        stateMachine = buildStateMachine();
        stateMachine.start();
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
                .perform(new OnChildReady());

        builder.transition()
                .from(ClusterState.WAIT_SHARDS).to(ClusterState.WAIT_SHARDS)
                .on(ClusterEventType.CHILD_JOINED)
                .when(notAllShardsJoined)
                .perform(new OnChildJoined());

        builder.transition()
                .from(ClusterState.WAIT_SHARDS).to(ClusterState.RUNNING)
                .on(ClusterEventType.CHILD_JOINED)
                .when(allShardsJoined)
                .perform(new OnChildJoined());

        // Running相关状态
        builder.onEntry(ClusterState.RUNNING)
                .perform(new OnEnterRunning());

        builder.onExit(ClusterState.RUNNING)
                .perform(new OnExitRunning());

        // status Update
        builder.transition()
                .from(ClusterState.RUNNING).to(ClusterState.RUNNING)
                .on(ClusterEventType.UPDATE_STATUS)
                .perform(new OnStatusUpdate());

        // scale out
        builder.transition()
                .from(ClusterState.RUNNING).to(ClusterState.WAIT_SHARDS)
                .on(ClusterEventType.SCALE_OUT)
                .perform(new OnScaleOut());

        // scale in
        // TODO
        /*
        builder.transition()
                .from(ClusterState.RUNNING).to(ClusterState.SCALING_IN)
                .on(ClusterEventType.SCALE_IN)
                .perform(new OnScaleIn());
        */

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
        readLock.lock();

        try {
            return stateMachine.getCurrentState();
        } finally {
            readLock.unlock();
        }
    }

    @Nothrow
    @Override
    public List<Instance> getInstances() {
        readLock.lock();

        try {
            return shards.stream()
                    .flatMap(shard -> shard.getInstances().stream())
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    @Nothrow
    @Override
    public ClusterReport getReport() {
        readLock.lock();

        try {
            return report.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Nothrow
    @Override
    public void handle(final ClusterEvent event) {
        writeLock.lock();

        try {
            stateMachine.fire(event.getType(), event);
        } finally {
            writeLock.unlock();
        }
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

            val childID = event.getPayload(ClusterID.class);
            val request = new JoinRequest(getID(), routerCluster.getID(), childID);

            context.getMongoMediator().join(request);
        }
    }

    private final class OnChildJoined extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onNewChildJoined(cluster: {}, child: {})", getID(), event.getPayload(ClusterID.class));

            ++joined;
        }
    }

    private final class OnEnterRunning extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onEnterRunning(cluster: {}): register to the monitor", getID());

            context.getMonitor().register(getID());
        }
    }

    private final class OnExitRunning extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onExitRunning(cluster: {}): unregister to the monitor", getID());

            context.getMonitor().unregister(getID());
        }
    }

    private final class OnStatusUpdate extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onStatusUpdate(cluster: {})", getID());

            final ClusterReport newReport = event.getPayload(ClusterReport.class);

            report.setStorageInMB(newReport.getStorageInMB());
        }
    }

    private final class OnScaleOut extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onScaleOut(cluster: {}, from: {}, to: {})", getID(), shards.size(), shards.size() + 1);

            final int index = idSeen++;
            final ReplicaCluster newCluster = new ReplicaCluster(ShardedCluster.this, index, context);

            LOG.info("< onScaleOut(cluster: {}, new: {}): register new cluster", getID(), newCluster.getID());
            shards.add(newCluster);
            context.getClusters().put(newCluster.getID(), newCluster);

            LOG.info("< onScaleOut(cluster: {}, new: {}): init new cluster", getID(), newCluster.getID());
            notifyChild(newCluster, ClusterEventType.INIT);
        }
    }

    private final class OnScaleIn extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onScaleIn(cluster: {}, from: {}, to: {})", getID(), shards.size(), shards.size() - 1);
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