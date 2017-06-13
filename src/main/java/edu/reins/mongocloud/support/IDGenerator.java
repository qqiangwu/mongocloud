package edu.reins.mongocloud.support;

import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.support.annotation.Nothrow;

import java.util.UUID;

public abstract class IDGenerator {
    @Nothrow
    private static String nextID() {
        return UUID.randomUUID().toString();
    }

    @Nothrow
    public static ClusterID generateClusterID() {
        return new ClusterID(nextID());
    }
}
