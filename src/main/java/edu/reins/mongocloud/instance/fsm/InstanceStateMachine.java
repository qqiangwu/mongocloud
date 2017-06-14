package edu.reins.mongocloud.instance.fsm;

import edu.reins.mongocloud.instance.InstanceEvent;
import edu.reins.mongocloud.instance.InstanceEventType;
import edu.reins.mongocloud.instance.InstanceState;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

public final class InstanceStateMachine extends
        AbstractStateMachine<InstanceStateMachine, InstanceState, InstanceEventType, InstanceEvent> {
    public static StateMachineBuilder<InstanceStateMachine, InstanceState, InstanceEventType, InstanceEvent> create() {
        return StateMachineBuilderFactory
                .create(InstanceStateMachine.class, InstanceState.class, InstanceEventType.class, InstanceEvent.class);
    }
}