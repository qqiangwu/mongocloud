package edu.reins.mongocloud.cluster.fsm;

import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.instance.InstanceEvent;
import edu.reins.mongocloud.instance.InstanceEventType;
import edu.reins.mongocloud.instance.InstanceState;
import edu.reins.mongocloud.support.FsmAction;

public abstract class ClusterAction
        extends FsmAction<ClusterStateMachine, ClusterState, ClusterEventType, ClusterEvent> {
}
