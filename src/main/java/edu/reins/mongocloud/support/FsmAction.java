package edu.reins.mongocloud.support;

import org.squirrelframework.foundation.fsm.AnonymousAction;
import org.squirrelframework.foundation.fsm.StateMachine;

public abstract class FsmAction<T extends StateMachine<T, S, E, C>, S, E, C> extends AnonymousAction<T, S, E, C> {
    protected S from;
    protected S to;

    @Override
    public void execute(S from, S to, E event, C context, T stateMachine) {
        this.from = from;
        this.to = to;

        doExec(context);
    }

    protected abstract void doExec(C event);
}
