package edu.reins.mongocloud.clustermanager;

import edu.reins.mongocloud.impl.AbstractEvent;

public final class ClusterManagerEvent extends AbstractEvent<ClusterManagerEventType> {
    public ClusterManagerEvent(final ClusterManagerEventType clusterManagerEventType) {
        super(clusterManagerEventType);
    }
}
