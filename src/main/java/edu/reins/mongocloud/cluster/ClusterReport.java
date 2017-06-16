package edu.reins.mongocloud.cluster;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public final class ClusterReport implements Cloneable {
    private Integer storageInMB;
    private Integer shardCount;

    public ClusterReport clone() {
        try {
            return (ClusterReport) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("clone failed", e);
        }
    }
}