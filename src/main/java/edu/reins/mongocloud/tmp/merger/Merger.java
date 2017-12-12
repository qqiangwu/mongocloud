package edu.reins.mongocloud.tmp.merger;

import edu.reins.mongocloud.tmp.ActionExecutor;
import edu.reins.mongocloud.tmp.CompactionStrategyGenerator;
import edu.reins.mongocloud.tmp.Service;

public class Merger implements Service {
    private CompactionStrategyGenerator compactionStrategyGenerator;
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
