package edu.reins.mongocloud.tmp.merger;

import edu.reins.mongocloud.tmp.CompactionAction;
import edu.reins.mongocloud.tmp.CompactionStrategyGenerator;

public class TwoPhraseCompactionStrategyGenerator implements CompactionStrategyGenerator {
    @Override
    public boolean needCompaction() {
        return false;
    }

    @Override
    public CompactionAction generateStrategy() {
        return null;
    }
}
