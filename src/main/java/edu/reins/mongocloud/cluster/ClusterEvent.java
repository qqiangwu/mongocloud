package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.impl.AbstractEvent;
import edu.reins.mongocloud.model.ClusterID;

public final class ClusterEvent extends AbstractEvent<ClusterEventType> {
    private final ClusterID id;

    public ClusterEvent(final ClusterID id, final ClusterEventType eventType) {
        this(id, eventType, null);
    }

    public ClusterEvent(final ClusterID id, final ClusterEventType eventType, final Object payload) {
        super(eventType, payload);

        this.id = id;
    }

    public ClusterID getClusterID() {
        return id;
    }
}
