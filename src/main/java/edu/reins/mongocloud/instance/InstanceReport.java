package edu.reins.mongocloud.instance;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public final class InstanceReport implements Cloneable {
    private Integer totalReads;
    private Integer totalWrites;

    private Integer cpuPercent;

    public InstanceReport clone() {
        try {
            return (InstanceReport) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
