package edu.reins.mongocloud.cluster.job.rs;

import edu.reins.mongocloud.Store;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.JobDefinition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReplicaSetJobBuilder {
    private final List<Instance> instances = new ArrayList<>();
    private Store store;
    private JobDefinition jobDefinition;

    public ReplicaSetJobBuilder add(@Nonnull final Instance instance) {
        instances.add(instance);

        return this;
    }

    public ReplicaSetJobBuilder store(@Nonnull final Store store) {
        this.store = store;

        return this;
    }

    public ReplicaSetJobBuilder jobDefinition(@Nonnull final JobDefinition jobDefinition) {
        this.jobDefinition = jobDefinition;

        return this;
    }

    public ReplicaSetJob build() {
        Objects.requireNonNull(store);
        Objects.requireNonNull(jobDefinition);

        return new ReplicaSetJob(store, jobDefinition, instances);
    }
}
