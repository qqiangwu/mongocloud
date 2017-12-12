package edu.reins.mongocloud.tmp.monitor;

import edu.reins.mongocloud.tmp.Service;
import edu.reins.mongocloud.tmp.agent.Metric;

import java.util.List;
import java.util.Map;

public class Monitor implements Service, ReportHandler {
    private NIOServer nioServer;
    private MetricStore metricStore;
    private ChunkHeatStore chunkHeatStore;

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public void stop() {

    }

    @Override
    public void onReceiveMetric(String shard, Metric metric) {

    }

    @Override
    public void onReceiveHeat(String shard, List<Metric> heats) {

    }

    public Map<String, Metric> getShardMetric(String shard) {
        return null;
    }

    public List<Metric> getShardChunkHeat(String shard) {
        return null;
    }
}
