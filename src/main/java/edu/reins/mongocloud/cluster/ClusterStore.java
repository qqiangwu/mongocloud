package edu.reins.mongocloud.cluster;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Optional;

@ThreadSafe
public interface ClusterStore {
    boolean isInitialized();

    void initialize();
    void waitForInitializing();
    void stop();

    void clear();

    Optional<Job> getJob(@Nonnull String instanceID);

    List<Job> getAllJobs();
}