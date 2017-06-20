package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.cluster.ClusterEvent;
import edu.reins.mongocloud.cluster.ClusterEventType;
import edu.reins.mongocloud.instance.fsm.InstanceAction;
import edu.reins.mongocloud.instance.fsm.InstanceStateMachine;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.model.InstanceLaunchRequest;
import edu.reins.mongocloud.support.Errors;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@ToString
public class InstanceImpl implements Instance {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final Context context;
    private final ClusterID parentID;
    private final InstanceID id;
    private final int localId;
    private final InstanceDefinition definition;
    private final InstanceStateMachine stateMachine;
    private final Map<String, String> env;
    private Optional<InstanceHost> host = Optional.empty();

    public InstanceImpl(
            final Context context,
            final Cluster parent,
            final int index,
            final InstanceDefinition definition,
            final Map<String, String> env) {
        this.context = context;
        this.parentID = parent.getID();
        this.id = new InstanceID(String.format("%s-%d", parent.getID().getValue(), index));
        this.localId = index;
        this.definition = definition;
        this.env = env;
        this.stateMachine = buildStateMachine();

        this.stateMachine.start();
    }

    private InstanceStateMachine buildStateMachine() {
        val builder = InstanceStateMachine.create();

        // operation INIT
        builder.transition()
                .from(InstanceState.NEW).to(InstanceState.SUBMITTED)
                .on(InstanceEventType.INIT)
                .perform(new OnInit());

        // on launching finished
        builder.transition()
                .from(InstanceState.SUBMITTED).to(InstanceState.STAGING)
                .on(InstanceEventType.LAUNCHED)
                .perform(new OnLaunched());

        // on running by scheduler
        builder.transition()
                .from(InstanceState.STAGING).to(InstanceState.RUNNING)
                .on(InstanceEventType.RUNNING)
                .perform(new OnRunning());

        // operation KILL
        builder.transitions()
                .fromAmong(InstanceState.values()).to(InstanceState.DIEING)
                .on(InstanceEventType.KILL)
                .perform(new OnKill());

        builder.transition()
                .from(InstanceState.DIEING).to(InstanceState.FINISHED)
                .on(InstanceEventType.KILLED)
                .perform(new OnCleanup());

        // done
        return builder.newStateMachine(InstanceState.NEW, this, context);
    }

    @Override
    public void handle(final InstanceEvent event) {
        writeLock.lock();

        try {
            stateMachine.fire(event.getType(), event);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public InstanceID getID() {
        return id;
    }

    @Override
    public int getLocalID() {
        return localId;
    }

    @Override
    public InstanceState getState() {
        readLock.lock();

        try {
            return stateMachine.getCurrentState();
        } finally {
            readLock.unlock();
        }
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
        readLock.lock();

        try {
            return host.orElseThrow(Errors.throwException(IllegalStateException.class, "instance is not running"));
        } finally {
            readLock.unlock();
        }
    }
    
    private final class OnInit extends InstanceAction {
        @Nothrow
        @Override
        protected void doExec(final InstanceEvent event) {
            LOG.info("doInit(instance: {}): submit to the scheduler", getID());

            context.getResourceProvider()
                    .launch(new InstanceLaunchRequest(getID(), getDefinition(), env));
        }
    }
    
    private final class OnLaunched extends InstanceAction {
        @Nothrow
        @Override
        protected void doExec(final InstanceEvent event) {
            final InstanceHost instanceHost = event.getPayload(InstanceHost.class);

            host = Optional.of(instanceHost);

            LOG.info("onLaunched(instance: {}, host: {})", getID(), instanceHost);
        }
    }
    
    private final class OnRunning extends InstanceAction {
        @Nothrow
        @Override
        protected void doExec(final InstanceEvent event) {
            LOG.info("onRunning(instance: {}, parent: {}): notify parent",
                    getID(), parentID);

            context.getEventBus()
                    .post(new ClusterEvent(parentID, ClusterEventType.CHILD_RUNNING, getID()));
        }
    }

    private final class OnKill extends InstanceAction {
        @Nothrow
        @Override
        protected void doExec(final InstanceEvent event) {
            LOG.info("onKill(instance: {})", getID());

            context.getResourceProvider().kill(getID());
        }
    }

    private final class OnCleanup extends InstanceAction {
        @Nothrow
        @Override
        protected void doExec(final InstanceEvent event) {
            LOG.info("onCleanup(instance: {})", getID());

            context.getEventBus().post(new ClusterEvent(parentID, ClusterEventType.CHILD_FINISHED, getID()));
        }
    }
}
