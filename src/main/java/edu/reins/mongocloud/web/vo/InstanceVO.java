package edu.reins.mongocloud.web.vo;

import edu.reins.mongocloud.instance.*;
import edu.reins.mongocloud.model.InstanceID;
import lombok.Data;

@Data
public class InstanceVO {
    private InstanceID id;
    private InstanceState state;
    private InstanceType type;
    private InstanceHost host;
    private ContainerInfo containerInfo;
    private InstanceReport report;
}
