package edu.reins.mongocloud.mongo;

import edu.reins.mongocloud.MongoMediator;
import edu.reins.mongocloud.cluster.Cluster;
import edu.reins.mongocloud.mongo.command.*;
import edu.reins.mongocloud.mongo.request.JoinRequest;
import edu.reins.mongocloud.mongo.request.RemoveRequest;
import edu.reins.mongocloud.mongo.request.RsJoinRequest;
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

    @Autowired
    private RsJoinCommand rsJoinCommand;

    @Autowired
    private RemoveCommand removeCommand;

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
    public void rsJoin(final RsJoinRequest rsJoinRequest) {
        LOG.info("rsJoin(cluster: {}, child: {})", rsJoinRequest.getCluster(), rsJoinRequest.getInstance());

        rsJoinCommand.exec(rsJoinRequest);
    }

    @Nothrow
    @Override
    public void remove(final RemoveRequest removeRequest) {
        LOG.info("remove(cluster: {}, child: {})", removeRequest.getCluster(), removeRequest.getParticipant());

        removeCommand.exec(removeRequest);
    }

    @Nothrow
    @Override
    public void collect(final Cluster cluster) {
        LOG.info("collect(cluster: {})", cluster.getID());

        collectClusterCommand.exec(cluster);
    }
}
