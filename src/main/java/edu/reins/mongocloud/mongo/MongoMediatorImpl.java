package edu.reins.mongocloud.mongo;

import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.mongo.command.CollectClusterCommand;
import edu.reins.mongocloud.mongo.command.InitRsCommand;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MongoMediatorImpl implements MongoMediator {
    @Autowired
    private InitRsCommand initRsCommand;

    @Autowired
    private CollectClusterCommand collectClusterCommand;

    @Async
    @Nothrow
    @Override
    public void initRs(final RsDefinition rsDefinition) {
        LOG.info("initRs(cluster: {}, members: {})", rsDefinition.getClusterID(), rsDefinition.getMembers().size());

        initRsCommand.exec(rsDefinition);
    }

    @Nothrow
    @Async
    @Override
    public void collect(final Cluster cluster) {
        LOG.info("collect(cluster: {})", cluster.getID());

        collectClusterCommand.exec(cluster);
    }
}
