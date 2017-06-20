package edu.reins.mongocloud.cluster;

public enum ClusterEventType {
    // 初始化一个新创建的Cluster
    INIT,

    // 子集群初始化完成
    CHILD_RUNNING,
    // 子集群join成功
    CHILD_JOINED,
    // 子集群被成功移除出MongoCluster，接下来需要将它关闭
    CHILD_REMOVED,
    // 子集群已经被关闭
    CHILD_FINISHED,

    // RS构建完成
    RS_INITED,

    // 遇到某种错误
    FAIL,

    // 收到状态更新
    UPDATE_STATUS,

    // 令cluster自动伸缩
    SCALE_IN,
    SCALE_OUT,

    // 关闭一个cluster
    KILL
}