package reins.wuqq;

import org.apache.mesos.Protos.TaskID;
import reins.wuqq.model.Instance;

import javax.annotation.Nonnull;

public interface ResourceProvider {
    Instance launch(@Nonnull Instance instance);
    void kill(@Nonnull TaskID taskID);
    void sync(@Nonnull TaskID taskID);
}
