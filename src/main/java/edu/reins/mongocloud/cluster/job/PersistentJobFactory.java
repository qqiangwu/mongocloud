package edu.reins.mongocloud.cluster.job;

import edu.reins.mongocloud.Store;
import edu.reins.mongocloud.cluster.Job;
import edu.reins.mongocloud.cluster.JobFactory;
import edu.reins.mongocloud.cluster.job.rs.ReplicaSetJobBuilder;
import edu.reins.mongocloud.exception.internal.NotImplementedException;
import edu.reins.mongocloud.model.Instance;
import edu.reins.mongocloud.model.InstanceType;
import edu.reins.mongocloud.model.JobDefinition;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.UUID;

@Component
public class PersistentJobFactory implements JobFactory {
    @Value("${docker.data.image}")
    private String dockerImageForDataServer;

    @Value("${docker.data.args}")
    private String dockerArgs;

    @Autowired
    private Store persistentStore;

    @Override
    public Job createReplicaSets(@Nonnull final JobDefinition jobDefinition) {
        val job = createJobRepresentation(jobDefinition);

        job.create();

        return job;
    }

    private Job createJobRepresentation(final JobDefinition jobDefinition) {
        val builder = new ReplicaSetJobBuilder();

        for (int i = 0, len = jobDefinition.getCount(); i < len; ++i) {
            builder.add(createDataInstance(jobDefinition.getName(), i));
        }

        builder.store(persistentStore);

        return builder.build();
    }

    private Instance createDataInstance(final String jobName, final int i) {
        val instance = new Instance();

        instance.setName(String.format("%s-%d", jobName, i));
        instance.setType(InstanceType.DATA_SERVER);
        instance.setId(UUID.randomUUID().toString());
        instance.setImage(dockerImageForDataServer);
        instance.setArgs(dockerArgs);
        instance.setCpus(1.0);
        instance.setMemory(1024);
        instance.setDisk(10 * 1024);

        return instance;
    }

    @Override
    public Job createShardCluster(@Nonnull final JobDefinition jobDefinition) {
        throw new NotImplementedException("shard is not supported now");
    }
}
