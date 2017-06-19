package edu.reins.mongocloud.cluster;

public enum ClusterState {
    NEW,

    // 初始化shard时的状态
    WAIT_CONFIG,
    WAIT_ROUTER,
    WAIT_SHARDS,

    // scale out replica set时用的
    WAIT_INSTANCE,

    // 初始化Data cluster时的状态
    SUBMITTED,
    INIT_RS,

    // 运行中
    RUNNING,

    // scale时的状态
    SCALING_OUT,
    SCALING_IN,
    // shard已经移除，正在准备回收
    RECYCLE,

    // 终止
    FAILED,
    FINISHED
}