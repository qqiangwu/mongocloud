package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.annotation.ContextInsensitive;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class ClusterImpl implements Actor<ClusterEvent> {
    @Autowired
    private EventBus eventBus;

    private final StateMachineImpl stateMachine = StateMachineBuilderFactory.create(
            StateMachineImpl.class,
            ClusterState.class,
            ClusterEventType.class,
            Void.class).newStateMachine(ClusterState.STARTT, this);

    @PostConstruct
    public void setup() {
        eventBus.register(ClusterEvent.class, this);
    }

    @Override
    public void handle(final ClusterEvent event) {
        stateMachine.fire(event.getType());
    }

    @ContextInsensitive
    private static class StateMachineImpl extends
            AbstractStateMachine<StateMachineImpl, ClusterState, ClusterEventType, Void> {
        private final ClusterImpl cluster;

        public StateMachineImpl(final ClusterImpl cluster) {
            this.cluster = cluster;
        }

        protected void transitFromStartToRunningOnSetup(
                final ClusterState from, final ClusterState to, final ClusterEventType event) {
            log.info("cluster starts to running");
        }

        protected void transitFromAnyToClosedOnFailover(
                final ClusterState from, final ClusterState to, final ClusterEventType event) {
            log.info("cluster is closed");

            shutdown();
        }

        protected void transitFromAnyToClosedOnDestroyed(
                final ClusterState from, final ClusterState to, final ClusterEventType event) {
            log.info("cluster is destroyed");

            shutdown();
        }

        private void shutdown() {
            System.exit(0);
        }
    }
}
