package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.impl.AbstractEvent;

public final class ClusterEvent extends AbstractEvent<ClusterEventType> {
    public ClusterEvent(final ClusterEventType clusterEventType) {
        super(clusterEventType);
    }
}
