package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.impl.AbstractEvent;

import java.util.Optional;

/**
 * @author wuqq
 */
public final class InstanceEvent extends AbstractEvent<InstanceEventType> {
    private final String instanceID;
    private final Instance instance;

    public InstanceEvent(final InstanceEventType eventType, final String instanceID) {
        super(eventType);

        this.instanceID = instanceID;
        this.instance = null;
    }

    public InstanceEvent(final InstanceEventType eventType, final Instance instance) {
        super(eventType);

        this.instanceID = instance.getId();
        this.instance = instance;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public Optional<Instance> getInstance() {
        return Optional.ofNullable(instance);
    }
}
