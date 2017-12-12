package edu.reins.mongocloud.tmp.core;

import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.model.InstanceLaunchRequest;
import edu.reins.mongocloud.tmp.EventBus;

import java.util.List;

public class NativeResourceProvider implements ResourceProvider {
    private EventBus eventBus;

    @Override
    public void launch(InstanceLaunchRequest request) {

    }

    @Override
    public void kill(InstanceID instanceID) {

    }

    @Override
    public void sync(List<Instance> instances) {

    }

}
