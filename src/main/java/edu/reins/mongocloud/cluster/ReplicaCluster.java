package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.fsm.ClusterAction;
import edu.reins.mongocloud.cluster.fsm.ClusterStateMachine;
import edu.reins.mongocloud.instance.*;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.mongo.request.RsJoinRequest;
import edu.reins.mongocloud.mongo.request.RsRemoveRequest;
import edu.reins.mongocloud.mongo.request.RsRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ReplicaCluster implements Cluster {
    private static final String DATA_SERVER_DEFINITION = "instance.data.definition";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final ClusterID id;
    private final ClusterID parent;
    private final Context context;
    private final List<Instance> instances;
    private final ClusterStateMachine stateMachine;
    private final InstanceDefinition dataServerDef;
    private final Map<String, String> env;
    private int idSeen;

    @Nothrow
    public ReplicaCluster(final Cluster parent, final int idx, final Context context) {
        this.dataServerDef = Clusters.loadDefinition(context.getEnv(), DATA_SERVER_DEFINITION);

        this.id = new ClusterID(String.format("%s-shard-%d", parent.getID().getValue(), idx));
        this.parent = parent.getID();
        this.context = context;
        this.env = Collections.singletonMap(Clusters.ENV_RS, getID().getValue());
        this.instances = IntStream.range(0, 3)
                .mapToObj(i -> new InstanceImpl(context, this, i, dataServerDef, env))
                .collect(Collectors.toList());

        this.idSeen = instances.size();
        this.stateMachine = buildStateMachine();
        this.stateMachine.start();
    }

    @Nothrow
    private ClusterStateMachine buildStateMachine() {
        val builder = ClusterStateMachine.create();

        // from NEW to INIT: launch all instances
        builder.transition()
                .from(ClusterState.NEW).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.INIT)
                .perform(new OnInit());

        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.statePartiallyIs(instances, InstanceState.RUNNING))
                .perform(new OnChildReady());

        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.INIT_RS)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.stateIs(instances, InstanceState.RUNNING))
                .perform(new OnChildrenRunning());

        // from INIT_RS: wait all instances to join the replica set
        builder.transition()
                .from(ClusterState.INIT_RS).to(ClusterState.RUNNING)
                .on(ClusterEventType.RS_INITED)
                .perform(new OnRsInited());

        builder.transition()
                .from(ClusterState.INIT_RS).to(ClusterState.FAILED)
                .on(ClusterEventType.FAIL)
                .perform(new OnFailed());

        // Kill operation
        builder.transition()
                .from(ClusterState.RUNNING).to(ClusterState.RECYCLE)
                .on(ClusterEventType.KILL)
                .perform(new OnKill());

        builder.transition()
                .from(ClusterState.RECYCLE).to(ClusterState.RECYCLE)
                .on(ClusterEventType.CHILD_FINISHED)
                .when(Conditions.statePartiallyIs(instances, InstanceState.FINISHED))
                .perform(new OnChildFinished());

        builder.transition()
                .from(ClusterState.RECYCLE).to(ClusterState.FINISHED)
                .on(ClusterEventType.CHILD_FINISHED)
                .when(Conditions.stateIs(instances, InstanceState.FINISHED))
                .perform(Arrays.asList(new OnChildFinished(), new OnCleanup()));

        // Scale out operation
        builder.transition()
                .from(ClusterState.RUNNING).to(ClusterState.SCALING_OUT)
                .on(ClusterEventType.SCALE_OUT)
                .perform(new OnScaleOut());
        builder.transition()
                .from(ClusterState.SCALING_OUT).to(ClusterState.WAIT_INSTANCE)
                .on(ClusterEventType.CHILD_RUNNING)
                .perform(new OnChildReadyInScaleOut());
        builder.transition()
                .from(ClusterState.WAIT_INSTANCE).to(ClusterState.RUNNING)
                .on(ClusterEventType.CHILD_JOINED);

        // Scale in operation
        builder.transition()
                .from(ClusterState.RUNNING).to(ClusterState.RUNNING)
                .on(ClusterEventType.SCALE_IN)
                .when(Conditions.sizeIs(instances, 1));
        builder.transition()
                .from(ClusterState.RUNNING).to(ClusterState.SCALING_IN)
                .on(ClusterEventType.SCALE_IN)
                .when(Conditions.sizeGreaterThan(instances, 1))
                .perform(new OnScaleIn());
        builder.transition()
                .from(ClusterState.SCALING_IN).to(ClusterState.SCALING_IN)
                .on(ClusterEventType.CHILD_REMOVED)
                .perform(new OnChildRemoved());
        builder.transition()
                .from(ClusterState.SCALING_IN).to(ClusterState.RUNNING)
                .on(ClusterEventType.CHILD_FINISHED)
                .perform(new OnChildFinished());

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
            return Collections.unmodifiableList(instances);
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
            LOG.info("onInit(cluster: {}): register instances and send INIT", getID());

            final EventBus eventBus = context.getEventBus();
            final Map<InstanceID, Instance> instanceContext = context.getInstances();

            instances.forEach(instance -> {
                instanceContext.put(instance.getID(), instance);
                eventBus.post(new InstanceEvent(InstanceEventType.INIT, instance.getID()));
            });
        }
    }

    private final class OnChildReady extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onChildReady(cluster: {}, child: {})", getID(), event.getPayload(InstanceID.class));
        }
    }

    private final class OnChildrenRunning extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onChildrenRunning(cluster: {}): launch finished, init rs", getID());

            final RsRequest rs = RsRequest.from(ReplicaCluster.this);

            context.getMongoMediator().initRs(rs);
        }
    }

    private final class OnRsInited extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onRsInited(cluster: {}): launch finished", getID());

            context.getEventBus()
                    .post(new ClusterEvent(parent, ClusterEventType.CHILD_RUNNING, getID()));
        }
    }

    private final class OnKill extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onKill(cluster: {})", getID());

            instances.forEach(instance -> notifyChild(instance.getID(), InstanceEventType.KILL));
        }
    }

    private final class OnChildFinished extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onChildRemoved(cluster: {}, child: {}): unregister the instance",
                    getID(), event.getPayload(InstanceID.class));

            final InstanceID child = event.getPayload(InstanceID.class);

            context.getInstances().remove(child);
            instances.removeIf(i -> i.getID().equals(child));
        }
    }

    private final class OnCleanup extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onCleanup(cluster: {})", getID());

            notifyParent(ClusterEventType.CHILD_FINISHED);
        }
    }

    // TODO     notify parent
    private final class OnFailed extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onFailed(id: {}, msg: {})", getID(), event.getPayload(String.class));

            System.exit(1);
        }
    }

    private final class OnScaleOut extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onScaleOut(id: {}, from: {}, to: {})", getID(), instances.size(), instances.size() + 1);

            LOG.info("< onScaleOut(id: {}): add new instance", getID());
            final Instance instance =
                    new InstanceImpl(context, ReplicaCluster.this, idSeen++, dataServerDef, env);

            instances.add(instance);
            context.getInstances().put(instance.getID(), instance);

            notifyChild(instance.getID(), InstanceEventType.INIT);
        }
    }

    private final class OnChildReadyInScaleOut extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onChildReadyInScaleOut(cluster: {}, child: {})", getID(), event.getPayload(InstanceID.class));

            final InstanceID child = event.getPayload(InstanceID.class);

            context.getMongoMediator().rsJoin(new RsJoinRequest(getID(), child));
        }
    }

    private final class OnScaleIn extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onScaleIn(cluster: {}, from: {}, to: {}): remove a child",
                    getID(), instances.size(), instances.size() - 1);

            final InstanceID victim = instances.get(instances.size() - 1).getID();

            context.getMongoMediator().rsRemove(new RsRemoveRequest(getID(), victim));
        }
    }

    private final class OnChildRemoved extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onChildRemoved(cluster: {}, child: {}): kill the child",
                    getID(), event.getPayload(InstanceID.class));

            final InstanceID victim = event.getPayload(InstanceID.class);

            instances.stream()
                    .map(Instance::getID)
                    .filter(victim::equals)
                    .forEach(id -> notifyChild(id, InstanceEventType.KILL));
        }
    }

    private void notifyChild(final InstanceID instanceID, final InstanceEventType event) {
        context.getEventBus().post(new InstanceEvent(event, instanceID));
    }

    private void notifyParent(final ClusterEventType event) {
        context.getEventBus().post(new ClusterEvent(parent, event, getID()));
    }
}