package edu.reins.mongocloud.model;

/**
 * 不考虑节点失效的情况
 */
public enum ClusterState {
    PREPARING_CONFIG,
    PREPARING_PROXY,
    PREPARING_SHARD,
    RUNNING,
    RECYCLE
}