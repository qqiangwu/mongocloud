package edu.reins.mongocloud.instance;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceReport {
    private int totalReads;
    private int totalWrites;

    private double cpuUsage;
}
