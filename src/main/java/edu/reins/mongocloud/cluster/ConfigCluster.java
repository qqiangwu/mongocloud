package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.fsm.ClusterAction;
import edu.reins.mongocloud.cluster.fsm.ClusterStateMachine;
import edu.reins.mongocloud.instance.*;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.mongo.MongoEvent;
import edu.reins.mongocloud.mongo.MongoEventType;
import edu.reins.mongocloud.mongo.request.RsRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO     notify parent of failure of init rs
@Slf4j
public class ConfigCluster implements Cluster {
    private static final String CONFIG_SERVER_DEFINITION = "instance.config.definition";
    private Optional<ConfigClusterMeta> meta = Optional.empty();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final ClusterID id;
    private final ClusterID parent;
    private final Context context;
    private final List<Instance> instances;
    private final ClusterStateMachine stateMachine;

    @Nothrow
    public ConfigCluster(final Cluster parent, final Context context) {
        final InstanceDefinition configServerDef = Clusters.loadDefinition(context.getEnv(), CONFIG_SERVER_DEFINITION);

        this.id = new ClusterID(String.format("%s-config", parent.getID().getValue()));
        this.parent = parent.getID();
        this.context = context;

        final Map<String, String> env = Collections.singletonMap(Clusters.ENV_RS, getID().getValue());

        this.instances = IntStream.range(0, 3)
                .mapToObj(i -> new InstanceImpl(context, this, i, configServerDef, env))
                .collect(Collectors.toList());

        this.stateMachine = buildStateMachine();
    }

    @Nothrow
    private ClusterStateMachine buildStateMachine() {
        val builder = ClusterStateMachine.create();

        // from NEW: start init
        builder.transition()
                .from(ClusterState.NEW).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.INIT)
                .perform(new OnInit());

        // from SUBMITTED: wait all instances to be done
        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.SUBMITTED)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.instancesNotFullyRunning(instances))
                .perform(new OnChildReady());

        builder.transition()
                .from(ClusterState.SUBMITTED).to(ClusterState.INIT_RS)
                .on(ClusterEventType.CHILD_RUNNING)
                .when(Conditions.allInstancesRunning(instances))
                .perform(Arrays.asList(new OnChildReady(), new OnAllChildrenReady()));

        // from INIT_RS: wait all instances to join the replica set
        builder.transition()
                .from(ClusterState.INIT_RS).to(ClusterState.RUNNING)
                .on(ClusterEventType.RS_INITED)
                .perform(new OnInitRsDone());

        builder.transition()
                .from(ClusterState.INIT_RS).to(ClusterState.FAILED)
                .on(ClusterEventType.FAIL)
                .perform(new OnInitRsFailed());

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
            val state = stateMachine.getCurrentState();
            return state == null? stateMachine.getInitialState(): state;
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

    /**
     * @throws IllegalStateException if the cluster is not running
     */
    public ConfigClusterMeta getMeta() {
        readLock.lock();

        try {
            return meta.orElseThrow(() -> new IllegalStateException("ConfigCluster is not running"));
        } finally {
            readLock.unlock();
        }
    }

    private final class OnInit extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onInit(cluster: {}): register and init instances", getID());

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

    private final class OnAllChildrenReady extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onAllChildrenReady(cluster: {}): init rs", getID());

            final RsRequest rs = RsRequest.from(ConfigCluster.this);

            context.getEventBus().post(new MongoEvent(MongoEventType.INIT_RS, getID(), rs));
        }
    }

    private final class OnInitRsDone extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onInitRsDone(cluster: {}): prepare to notify parent", getID());

            meta = Optional.of(createMeta());

            context.getEventBus()
                    .post(new ClusterEvent(parent, ClusterEventType.CHILD_RUNNING, getID()));
        }
    }

    private final class OnInitRsFailed extends ClusterAction {
        @Nothrow
        @Override
        protected void doExec(final ClusterEvent event) {
            LOG.info("onInitRsFailed(cluster: {}, msg: {}): notify parent", getID(), event.getPayload(String.class));
        }
    }

    @Nothrow
    private ConfigClusterMeta createMeta() {
        final String name = getID().getValue();
        final List<String> members = instances.stream()
                .map(Instances::toAddress)
                .collect(Collectors.toList());

        return new ConfigClusterMeta(name, members);
    }
}
