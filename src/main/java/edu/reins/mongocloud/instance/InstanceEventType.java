package edu.reins.mongocloud.instance;

/**
 * @author wuqq
 */
public enum InstanceEventType {
    INIT,

    // by resource provider
    LAUNCHED,
    RUNNING,
    FAILED,
    KILLED,
    ERROR,

    // by cluster
    KILL
}