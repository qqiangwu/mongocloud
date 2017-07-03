package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.Fsm;
import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;

public interface Instance extends Fsm<InstanceState, InstanceEvent>{
    ClusterID getParentID();
    InstanceID getID();
    int getLocalID();
    InstanceState getState();
    InstanceType getType();
    InstanceDefinition getDefinition();
    InstanceReport getReport();

    /**
     * @throws IllegalStateException if the instance is not running
     */
    InstanceHost getHost();

    /**
     * @throws IllegalStateException if the instance is not running
     */
    ContainerInfo getContainerInfo();
}
