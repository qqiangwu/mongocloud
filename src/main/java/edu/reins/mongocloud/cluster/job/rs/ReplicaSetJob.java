package edu.reins.mongocloud.cluster.job.rs;

import edu.reins.mongocloud.Store;
import edu.reins.mongocloud.cluster.Job;
import edu.reins.mongocloud.exception.internal.ProgrammingError;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.JobDefinition;
import lombok.Getter;
import lombok.Setter;
import org.apache.mesos.Protos;

import javax.annotation.Nonnull;
import java.util.List;


@Getter
@Setter
public class ReplicaSetJob implements Job {
    private List<Instance> instances;
    private JobDefinition jobDefinition;

    private transient Store persistentStore;

    public ReplicaSetJob(@Nonnull final Store store, @Nonnull final JobDefinition jobDefinition, @Nonnull final List<Instance> instances) {
        this.instances = instances;
        this.jobDefinition = jobDefinition;
        this.persistentStore = store;
    }

    @Override
    public JobDefinition getDefinition() {
        return jobDefinition;
    }

    @Override
    public List<Instance> getInstances() {
        return instances;
    }

    @Override
    public boolean contains(@Nonnull final String instanceID) {
        return instances.stream().anyMatch(i -> i.getId().equals(instanceID));
    }

    @Override
    public void create() {
        ensureAttached();

        persistentStore.put(jobDefinition.getName(), this);
    }

    private void ensureAttached() {
        if (persistentStore == null) {
            throw new ProgrammingError(String.format("job[%s] shouldn't be detached", jobDefinition.getName()));
        }
    }

    @Override
    public void attach(@Nonnull final Store store) {
        persistentStore = store;
    }

    @Override
    public void onInstanceRunning(@Nonnull final String instanceID, @Nonnull final Protos.TaskStatus status) {
        ensureAttached();
    }

    @Override
    public void onInstanceLaunched(@Nonnull final String instanceID, @Nonnull final Instance payload) {
        ensureAttached();
    }

    @Override
    public void onInstanceKilled(@Nonnull final String instanceID, @Nonnull final Protos.TaskStatus status) {
        ensureAttached();
    }

    @Override
    public void onInstanceFailed(@Nonnull final String instanceID, @Nonnull final Protos.TaskStatus status) {
        ensureAttached();
    }

    @Override
    public void onInstanceError(@Nonnull final String instanceID, @Nonnull final Protos.TaskStatus status) {
        ensureAttached();
    }
}