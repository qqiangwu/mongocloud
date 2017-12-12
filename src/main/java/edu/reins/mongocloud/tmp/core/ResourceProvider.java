package edu.reins.mongocloud.tmp.core;

import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.InstanceID;
import edu.reins.mongocloud.model.InstanceLaunchRequest;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

@ThreadSafe
public interface ResourceProvider {
    void launch(InstanceLaunchRequest request);
    void kill(InstanceID instanceID);

    void sync(List<Instance> instances);
}
