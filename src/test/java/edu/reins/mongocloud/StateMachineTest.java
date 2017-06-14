package edu.reins.mongocloud;

import edu.reins.mongocloud.clustermanager.ClusterManagerEventType;
import edu.reins.mongocloud.clustermanager.ClusterManagerState;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.annotation.ContextInsensitive;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author wuqq
 */
@Ignore(value = "Just use it to be familiar with FSM")
public class StateMachineTest {
    @ContextInsensitive
    private static class StateMachineImpl extends
            AbstractStateMachine<StateMachineImpl, ClusterManagerState, ClusterManagerEventType, Void> {
        @Nothrow
        protected void transitFromSTARTToRUNNINGOnSETUP(
                final ClusterManagerState from, final ClusterManagerState to, final ClusterManagerEventType event) {
            System.out.println("cluster starts to running");
        }

        @Nothrow
        protected void transitFromAnyToClosedOnFailover(
                final ClusterManagerState from, final ClusterManagerState to, final ClusterManagerEventType event) {
            System.out.println("cluster is closed");
        }

        @Nothrow
        protected void transitFromAnyToClosedOnDestroyed(
                final ClusterManagerState from, final ClusterManagerState to, final ClusterManagerEventType event) {
            System.out.println("cluster is destroyed");
        }

        @Nothrow
        protected void onSETUP(
                final ClusterManagerState from, final ClusterManagerState to, final ClusterManagerEventType event) {
            System.out.println("cluster is destroyed");
        }
    }

    @Test
    public void testSimple() {
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

        val stateMachine = builder.newStateMachine(ClusterManagerState.START);

        stateMachine.fire(ClusterManagerEventType.SETUP);
    }
}
