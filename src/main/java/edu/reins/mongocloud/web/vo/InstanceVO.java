package edu.reins.mongocloud.web.vo;

import edu.reins.mongocloud.instance.ContainerInfo;
import edu.reins.mongocloud.instance.InstanceHost;
import edu.reins.mongocloud.instance.InstanceState;
import edu.reins.mongocloud.instance.InstanceType;
import edu.reins.mongocloud.model.InstanceID;
import lombok.Data;

@Data
public class InstanceVO {
    private InstanceID id;
    private InstanceState state;
    private InstanceType type;
    private InstanceHost host;
    private ContainerInfo containerInfo;
}
