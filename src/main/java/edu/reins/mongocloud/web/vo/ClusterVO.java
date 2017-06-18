package edu.reins.mongocloud.web.vo;

import edu.reins.mongocloud.cluster.ClusterReport;
import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.model.ClusterID;
import lombok.Data;

import java.util.List;

@Data
public class ClusterVO {
    private ClusterID id;
    private ClusterState state;
    private List<InstanceVO> instances;
    private ClusterReport report;
}
