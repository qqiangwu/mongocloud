package edu.reins.mongocloud.tmp;

import org.aspectj.bridge.Message;

public interface EventBus {
    void postMessage(long target, Message message);
}
