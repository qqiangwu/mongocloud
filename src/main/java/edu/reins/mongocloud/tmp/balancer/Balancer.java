package edu.reins.mongocloud.tmp.balancer;

import edu.reins.mongocloud.tmp.ActionExecutor;
import edu.reins.mongocloud.tmp.BalanceStrategyGenerator;
import edu.reins.mongocloud.tmp.EventBus;
import edu.reins.mongocloud.tmp.Service;

public class Balancer implements Service {
    private BalanceStrategyGenerator strategyGenerator;
    private ActionExecutor actionExecutor;

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
