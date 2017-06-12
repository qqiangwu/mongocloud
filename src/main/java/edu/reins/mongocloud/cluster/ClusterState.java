package edu.reins.mongocloud.cluster;

public enum ClusterState {
    NEW,

    WAIT_CONFIG,
    WAIT_ROUTER,
    WAIT_SHARDS,

    SUBMITTED,

    RUNNING,

    CLEANUP,
    DIED
}