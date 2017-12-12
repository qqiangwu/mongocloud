package edu.reins.mongocloud.tmp.cluster;

import org.aspectj.bridge.Message;

public interface EventDispatcher {
    void dispatch(long target, Message message);
}
