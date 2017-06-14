package edu.reins.mongocloud.clustermanager;

public enum ClusterManagerEventType {
    // Sent by the resource layer
    SETUP,
    DESTROYED,
    FAILOVER,
    CLOSE

    // Sent by members
}