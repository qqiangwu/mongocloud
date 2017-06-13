package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.impl.AbstractEvent;
import edu.reins.mongocloud.model.InstanceID;

public final class InstanceEvent extends AbstractEvent<InstanceEventType> {
    private final InstanceID instanceID;

    public InstanceEvent(final InstanceEventType eventType, final InstanceID instanceID) {
        super(eventType);

        this.instanceID = instanceID;
    }

    public InstanceEvent(final InstanceEventType eventType, final InstanceID instanceID, final Object payload) {
        super(eventType, payload);

        this.instanceID = instanceID;
    }

    public InstanceID getInstanceID() {
        return instanceID;
    }
}
