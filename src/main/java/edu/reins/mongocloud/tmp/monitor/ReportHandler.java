package edu.reins.mongocloud.tmp.monitor;

import edu.reins.mongocloud.tmp.agent.Metric;

import java.util.List;

public interface ReportHandler {
    void onReceiveMetric(String shard, Metric metric);
    void onReceiveHeat(String shard, List<Metric> heats);
}
