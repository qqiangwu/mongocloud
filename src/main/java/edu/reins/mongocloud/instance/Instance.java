package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.model.InstanceID;

public interface Instance extends Actor<InstanceEvent> {
    InstanceID getID();
    InstanceState getState();
    InstanceType getType();
    InstanceDefinition getDefinition();
    InstanceHost getHost();
}
