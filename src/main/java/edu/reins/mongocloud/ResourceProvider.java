package edu.reins.mongocloud;

import edu.reins.mongocloud.instance.Instance;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

@ThreadSafe
public interface ResourceProvider {
    void launch(Instance instance);
    void kill(String instanceID);

    void sync(List<Instance> instances);
}
