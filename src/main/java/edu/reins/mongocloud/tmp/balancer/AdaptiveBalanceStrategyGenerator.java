package edu.reins.mongocloud.tmp.balancer;

import edu.reins.mongocloud.tmp.BalanceAction;
import edu.reins.mongocloud.tmp.BalanceStrategyGenerator;

public class AdaptiveBalanceStrategyGenerator implements BalanceStrategyGenerator {
    @Override
    public boolean needRebalancing() {
        return false;
    }

    @Override
    public BalanceAction generateStrategy() {
        return null;
    }
}
