package edu.reins.mongocloud.tmp;

public interface BalanceStrategyGenerator {
    boolean needRebalancing();
    BalanceAction generateStrategy();
}
