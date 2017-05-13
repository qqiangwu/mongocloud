package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Store;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.JobDefinition;
import org.apache.mesos.Protos;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.List;

/**
 *
 * Will only be mutated by the pipeline thread. Other threads can only read the data.
 *
 * This class is serializable
 */
@ThreadSafe
public interface Job extends Serializable {
    JobDefinition getDefinition();

    List<Instance> getInstances();

    boolean contains(@Nonnull String instanceID);

    void create();
    void attach(@Nonnull Store store);

    void onInstanceRunning(@Nonnull String instanceID, @Nonnull Protos.TaskStatus status);

    void onInstanceLaunched(@Nonnull String instanceID, @Nonnull Instance payload);

    void onInstanceKilled(@Nonnull String instanceID, @Nonnull Protos.TaskStatus status);

    void onInstanceFailed(@Nonnull String instanceID, @Nonnull Protos.TaskStatus status);

    void onInstanceError(@Nonnull String instanceID, @Nonnull Protos.TaskStatus status);
}
