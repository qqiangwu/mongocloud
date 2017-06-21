package edu.reins.mongocloud.instance;

import lombok.Data;

@Data
public final class InstanceReport implements Cloneable {
    private Integer totalReads;
    private Integer totalWrites;

    private Integer cpuPercent;

    @java.beans.ConstructorProperties({"totalReads", "totalWrites", "cpuPercent"})
    InstanceReport(Integer totalReads, Integer totalWrites, Integer cpuPercent) {
        this.totalReads = totalReads;
        this.totalWrites = totalWrites;
        this.cpuPercent = cpuPercent;
    }

    public static InstanceReportBuilder builder() {
        return new InstanceReportBuilder();
    }

    public InstanceReport clone() {
        try {
            return (InstanceReport) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public static class InstanceReportBuilder {
        private Integer totalReads;
        private Integer totalWrites;
        private Integer cpuPercent;

        InstanceReportBuilder() {
        }

        public InstanceReport.InstanceReportBuilder totalReads(Integer totalReads) {
            this.totalReads = totalReads;
            return this;
        }

        public InstanceReport.InstanceReportBuilder totalWrites(Integer totalWrites) {
            this.totalWrites = totalWrites;
            return this;
        }

        public InstanceReport.InstanceReportBuilder cpuPercent(Integer cpuPercent) {
            this.cpuPercent = cpuPercent;
            return this;
        }

        public InstanceReport build() {
            return new InstanceReport(totalReads, totalWrites, cpuPercent);
        }

        public String toString() {
            return "edu.reins.mongocloud.instance.InstanceReport.InstanceReportBuilder(totalReads=" + this.totalReads + ", totalWrites=" + this.totalWrites + ", cpuPercent=" + this.cpuPercent + ")";
        }
    }
}
