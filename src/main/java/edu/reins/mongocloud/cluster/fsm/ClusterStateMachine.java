package edu.reins.mongocloud.cluster.fsm;

import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.cluster.ShardedCluster;
import edu.reins.mongocloud.instance.InstanceEvent;
import edu.reins.mongocloud.instance.InstanceEventType;
import edu.reins.mongocloud.instance.InstanceState;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

public final class ClusterStateMachine extends
        AbstractStateMachine<ClusterStateMachine, ClusterState, ClusterEventType, ClusterEvent> {
    public static StateMachineBuilder<ClusterStateMachine, ClusterState, ClusterEventType, ClusterEvent> create() {
        return StateMachineBuilderFactory
                .create(ClusterStateMachine.class, ClusterState.class, ClusterEventType.class, ClusterEvent.class);
    }
}