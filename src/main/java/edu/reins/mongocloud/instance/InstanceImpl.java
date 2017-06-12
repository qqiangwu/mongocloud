package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.model.InstanceLaunchRequest;
import edu.reins.mongocloud.support.Errors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import java.util.Optional;

@Slf4j
public class InstanceImpl implements Instance {
    private final Context context;
    private final ClusterID parentID;
    private final InstanceID id;
    private final InstanceDefinition definition;
    private final StateMachineImpl stateMachine;
    private Optional<InstanceHost> host = Optional.empty();

    public InstanceImpl(
            final Context context,
            final Cluster parent,
            final int index,
            final InstanceDefinition definition) {
        this.context = context;
        this.parentID = parent.getID();
        this.id = new InstanceID(String.format("%s-%d", parent.getID().getValue(), index));
        this.definition = definition;
        this.stateMachine = buildStateMachine();
    }

    private StateMachineImpl buildStateMachine() {
        val builder = StateMachineBuilderFactory
                .create(StateMachineImpl.class, InstanceState.class, InstanceEventType.class, InstanceEvent.class);

        // from NEW: start init
        builder.transition()
                .from(InstanceState.NEW).to(InstanceState.SUBMITTED)
                .on(InstanceEventType.INIT);

        // from SUBMITTED
        builder.transition()
                .from(InstanceState.SUBMITTED).to(InstanceState.STAGING)
                .on(InstanceEventType.LAUNCHED);

        builder.transition()
                .from(InstanceState.STAGING).to(InstanceState.RUNNING)
                .on(InstanceEventType.RUNNING);

        // done
        return builder.newStateMachine(InstanceState.NEW, this);
    }

    @Override
    public void handle(final InstanceEvent event) {
        stateMachine.fire(event.getType(), event);
    }

    @Override
    public InstanceID getID() {
        return id;
    }

    @Override
    public InstanceState getState() {
        return stateMachine.getCurrentState();
    }

    @Override
    public InstanceType getType() {
        return getDefinition().getType();
    }

    @Override
    public InstanceDefinition getDefinition() {
        return definition;
    }

    @Override
    public InstanceHost getHost() {
        return host.orElseThrow(Errors.throwException(IllegalStateException.class, "instance is not running"));
    }

    private class StateMachineImpl extends
            AbstractStateMachine<StateMachineImpl, InstanceState, InstanceEventType, InstanceEvent> {
        protected void transmitFromNewToSubmittedOnInit(
                InstanceState from, InstanceState to, InstanceEventType event, InstanceEvent ctx) {
            log.info("onInit(instance: {}): submit to the scheduler", getID());

            context.getResourceProvider().launch(new InstanceLaunchRequest(getID(), getDefinition()));
        }

        protected void transmitFromSubmittedToStagingOnLaunched(
                InstanceState from, InstanceState to, InstanceEventType event, InstanceEvent ctx) {
            log.info("onLaunched(instance: {})", getID());

            final InstanceHost host = ctx.getPayload(InstanceHost.class);

            InstanceImpl.this.host = Optional.of(host);

            log.info("> onLaunched(instance: {}, host: {})", getID(), host);
        }

        protected void transmitFromStagingToRunningOnRunning(
                InstanceState from, InstanceState to, InstanceEventType event, InstanceEvent ctx) {
            log.info("onRunning(instance: {}, parent: {}): notify parent", getID(), parentID);

            context.getEventBus().post(new ClusterEvent(parentID, ClusterEventType.CHILD_RUNNING, getID()));
        }
    }
}
