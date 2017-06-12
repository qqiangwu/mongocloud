package edu.reins.mongocloud.support;

import edu.reins.mongocloud.model.ClusterID;

import java.util.UUID;

public abstract class IDGenerator {
    private static String nextID() {
        return UUID.randomUUID().toString();
    }

    public static ClusterID generateClusterID() {
        return new ClusterID(nextID());
    }
}
