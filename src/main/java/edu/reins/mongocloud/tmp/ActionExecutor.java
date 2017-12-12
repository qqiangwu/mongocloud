package edu.reins.mongocloud.tmp;

public interface ActionExecutor {
    void execute(BalanceAction action);
    void execute(CompactionAction action);
}
