package edu.reins.mongocloud.tmp.cluster;

import edu.reins.mongocloud.tmp.EventBus;
import org.aspectj.bridge.Message;

public class AsyncEventBus implements EventBus, EventDispatcher {
    private ClusterManager clusterManager;

    @Override
    public void postMessage(long target, Message message) {

    }

    @Override
    public void dispatch(long target, Message message) {

    }
}
