package edu.reins.mongocloud.mongo;

import edu.reins.mongocloud.MongoMediator;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.mongo.command.CollectClusterCommand;
import edu.reins.mongocloud.mongo.command.InitRsCommand;
import edu.reins.mongocloud.mongo.command.JoinCommand;
import edu.reins.mongocloud.mongo.request.JoinRequest;
import edu.reins.mongocloud.mongo.request.RemoveRequest;
import edu.reins.mongocloud.mongo.request.RsRequest;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Async
@Component
@Slf4j
public class MongoMediatorImpl implements MongoMediator {
    @Autowired
    private InitRsCommand initRsCommand;

    @Autowired
    private CollectClusterCommand collectClusterCommand;

    @Autowired
    private JoinCommand joinCommand;

    @Nothrow
    @Override
    public void initRs(final RsRequest rsRequest) {
        LOG.info("initRs(cluster: {}, members: {})", rsRequest.getClusterID(), rsRequest.getMembers().size());

        initRsCommand.exec(rsRequest);
    }

    @Nothrow
    @Override
    public void join(final JoinRequest joinRequest) {
        LOG.info("join(cluster: {}, child: {})", joinRequest.getCluster(), joinRequest.getParticipant());

        joinCommand.exec(joinRequest);
    }

    @Nothrow
    @Override
    public void remove(final RemoveRequest removeRequest) {
        LOG.info("remove(cluster: {}, child: {})", removeRequest.getCluster(), removeRequest.getParticipant());
    }

    @Nothrow
    @Override
    public void collect(final Cluster cluster) {
        LOG.info("collect(cluster: {})", cluster.getID());

        collectClusterCommand.exec(cluster);
    }
}
