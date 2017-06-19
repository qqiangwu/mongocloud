package edu.reins.mongocloud.instance;

public enum InstanceState {
    NEW,
    SUBMITTED,
    STAGING,
    RUNNING,
    FAILED,
    DIEING,
    FINISHED
}
