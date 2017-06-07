package edu.reins.mongocloud;

import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.model.JobDefinition;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;

/**
 * Facade for both cluster access and mutation
 */
@ThreadSafe
public interface Cluster {
    boolean isInitialized();

    void submit(JobDefinition jobDefinition);

    Optional<Instance> getInstance(String instanceID);
}