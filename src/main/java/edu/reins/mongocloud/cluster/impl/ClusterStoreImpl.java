package edu.reins.mongocloud.cluster.impl;

import edu.reins.mongocloud.Store;
import edu.reins.mongocloud.cluster.ClusterStore;
import edu.reins.mongocloud.cluster.Job;
import edu.reins.mongocloud.exception.internal.NotImplementedError;
import edu.reins.mongocloud.support.annotation.SoftState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@SoftState
public class ClusterStoreImpl implements ClusterStore {
    private boolean initialized = false;

    @Autowired
    Store persistentStore;

    List<Job> jobs;

    @Override
    public synchronized boolean isInitialized() {
        return initialized;
    }

    @Override
    public synchronized void initialize() {
        initialized = true;

        notifyAll();
    }

    @Override
    public synchronized void waitForInitializing() {
        while (!isInitialized()) {
            try {
                wait();
            } catch (InterruptedException e) {
                /* ignore e */
            }
        }
    }

    @Override
    public synchronized void stop() {
        initialized = false;

        notifyAll();
    }

    @Override
    public synchronized void clear() {
        throw new NotImplementedError("ClusterStoreImpl::clear");
    }

    @Override
    public synchronized Optional<Job> getJob(@Nonnull String instanceID) {
        return getAllJobs().stream()
                .filter(j -> j.contains(instanceID))
                .findFirst();
    }

    @Override
    public synchronized List<Job> getAllJobs() {
        if (jobs == null) {
            loadAllJobs();
        }

        return jobs;
    }

    private void loadAllJobs() {
        jobs = new ArrayList<>();

        persistentStore.keys()
                .stream()
                .filter(key -> !key.startsWith(Store.META_PREFIX))
                .forEach(key -> {
                    persistentStore.get(key, Job.class).ifPresent(job -> {
                        job.attach(persistentStore);
                        jobs.add(job);
                    });
                });
    }
}
