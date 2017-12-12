package edu.reins.mongocloud.tmp.autoscaler;

import edu.reins.mongocloud.tmp.EventBus;
import edu.reins.mongocloud.tmp.Service;
import edu.reins.mongocloud.tmp.monitor.Monitor;

public class Autoscaler implements Service {
    private EventBus eventBus;
    private Monitor monitor;

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public void stop() {

    }

    public void checkPeriodically() {

    }
}
