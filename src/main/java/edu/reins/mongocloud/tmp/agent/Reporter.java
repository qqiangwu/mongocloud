package edu.reins.mongocloud.tmp.agent;

import java.util.List;

public interface Reporter {
    void reportMetric(Metric metric);
    void reportHeat(List<Metric> heats);
}
