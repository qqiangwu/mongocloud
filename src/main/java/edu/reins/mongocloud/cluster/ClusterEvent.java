package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.impl.AbstractEvent;
import edu.reins.mongocloud.model.ClusterID;

public class ClusterEvent extends AbstractEvent<ClusterEventType> {
    private final ClusterID id;
    private final Object payload;

    public ClusterEvent(final ClusterID id, final ClusterEventType eventType) {
        this(id, eventType, null);
    }

    public ClusterEvent(final ClusterID id, final ClusterEventType eventType, final Object payload) {
        super(eventType);

        this.id = id;
        this.payload = payload;
    }

    public ClusterID getClusterID() {
        return id;
    }
}
