package edu.reins.mongocloud.tmp;

public interface CompactionStrategyGenerator {
    boolean needCompaction();
    CompactionAction generateStrategy();
}
