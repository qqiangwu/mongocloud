package edu.reins.mongocloud;

import edu.reins.mongocloud.model.Instance;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

@ThreadSafe
public interface ResourceProvider {
    void launch(@Nonnull Instance instance);
    void kill(@Nonnull String instanceID);

    void sync(@Nonnull List<Instance> instances);
}
