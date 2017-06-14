package edu.reins.mongocloud.instance.fsm;

import edu.reins.mongocloud.instance.InstanceEvent;
import edu.reins.mongocloud.instance.InstanceEventType;
import edu.reins.mongocloud.instance.InstanceState;
import edu.reins.mongocloud.support.FsmAction;

public abstract class InstanceAction
        extends FsmAction<InstanceStateMachine, InstanceState, InstanceEventType, InstanceEvent> {
}
