package edu.reins.mongocloud.cluster.command;

public enum CommandType {
    CLUSTER_SETUP,
    CLUSTER_DESTROYED,
    CLUSTER_FAILOVER,

    INSTANCE_LAUNCHED,
    INSTANCE_RUNNING,
    INSTANCE_FAILED,
    INSTANCE_KILLED,
    INSTANCE_ERROR,

    CREATE_JOB,

    CLEAN_CLUSTER
}
