package edu.reins.mongocloud;

import org.apache.mesos.Protos.TaskID;
import edu.reins.mongocloud.model.Instance;

import javax.annotation.Nonnull;

public interface ResourceProvider {
    Instance launch(@Nonnull Instance instance);
    void kill(@Nonnull TaskID taskID);
    void sync(@Nonnull TaskID taskID);
}
