package edu.reins.mongocloud.mongo;

import edu.reins.mongocloud.impl.AbstractEvent;
import edu.reins.mongocloud.model.ClusterID;

public class MongoEvent extends AbstractEvent<MongoEventType> {
    private final ClusterID sourceID;

    public MongoEvent(final MongoEventType mongoEventType, final ClusterID sourceID) {
        super(mongoEventType);

        this.sourceID = sourceID;
    }

    public MongoEvent(final MongoEventType mongoEventType, final ClusterID sourceID, final Object payload) {
        super(mongoEventType, payload);

        this.sourceID = sourceID;
    }

    public ClusterID getSourceID() {
        return sourceID;
    }
}
