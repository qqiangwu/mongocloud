package edu.reins.mongocloud.clustermanager;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.ClusterManager;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.cluster.ShardedCluster;
import edu.reins.mongocloud.clustermanager.exception.ClusterIDConflictException;
import edu.reins.mongocloud.model.ClusterDefinition;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.annotation.ContextInsensitive;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class ClusterManagerImpl implements ClusterManager, Actor<ClusterManagerEvent> {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Context context;

    private final StateMachineImpl stateMachine;

    @Nothrow
    public ClusterManagerImpl() {
        val builder = StateMachineBuilderFactory.create(
                StateMachineImpl.class,
                ClusterManagerState.class,
                ClusterManagerEventType.class,
                Void.class);

        builder.transition()
                .from(ClusterManagerState.START).to(ClusterManagerState.RUNNING)
                .on(ClusterManagerEventType.SETUP);

        builder.transitions()
                .fromAmong(ClusterManagerState.values())
                .to(ClusterManagerState.CLOSED)
                .on(ClusterManagerEventType.FAILOVER);

        builder.transitions()
                .fromAmong(ClusterManagerState.values())
                .to(ClusterManagerState.CLOSED)
                .on(ClusterManagerEventType.DESTROYED);

        builder.transitions()
                .fromAmong(ClusterManagerState.values())
                .to(ClusterManagerState.CLOSED)
                .on(ClusterManagerEventType.CLOSE);

        stateMachine = builder.newStateMachine(ClusterManagerState.START);
        stateMachine.start();
    }

    @PostConstruct
    public synchronized void setup() {
        eventBus.register(ClusterManagerEvent.class, this);
    }

    @Nothrow
    @Override
    public synchronized void handle(final ClusterManagerEvent event) {
        stateMachine.fire(event.getType());
    }

    @Nothrow
    @Override
    public synchronized boolean isInitialized() {
        return stateMachine.getCurrentState() == ClusterManagerState.RUNNING;
    }

    /**
     *
     * @throws ClusterIDConflictException
     * @throws IllegalStateException        if clusterManager is not initialized
     */
    @Override
    public synchronized void createCluster(final ClusterDefinition clusterDefinition)
            throws ClusterIDConflictException {
        ensureInitialized();

        val clusterID = new ClusterID(clusterDefinition.getId());
        val cluster = new ShardedCluster(clusterID, clusterDefinition, context);

        if (context.getClusters().putIfAbsent(clusterID, cluster) != null) {
            throw new ClusterIDConflictException(clusterID);
        }

        eventBus.post(new ClusterEvent(clusterID, ClusterEventType.INIT));
    }

    private void ensureInitialized() {
        if (!isInitialized()) {
            throw new IllegalStateException("ClusterMananger is not initialized");
        }
    }

    @ContextInsensitive
    private static class StateMachineImpl extends
            AbstractStateMachine<StateMachineImpl, ClusterManagerState, ClusterManagerEventType, Void> {
        @Nothrow
        protected void onSETUP(
                final ClusterManagerState from, final ClusterManagerState to, final ClusterManagerEventType event) {
            LOG.info("cluster starts to running");
        }

        @Nothrow
        protected void onFAILOVER(
                final ClusterManagerState from, final ClusterManagerState to, final ClusterManagerEventType event) {
            LOG.info("cluster is closed");

            shutdown();
        }

        @Nothrow
        protected void onDESTROYED(
                final ClusterManagerState from, final ClusterManagerState to, final ClusterManagerEventType event) {
            LOG.info("cluster is destroyed");

            shutdown();
        }

        @Nothrow
        protected void onCLOSE(
                final ClusterManagerState from, final ClusterManagerState to, final ClusterManagerEventType event) {
            LOG.info("cluster is closed");

            shutdown();
        }

        @Nothrow
        private void shutdown() {
            System.exit(0);
        }
    }
}
