package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class ClusterEventDispatcher implements Actor<ClusterEvent> {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Context context;

    @PostConstruct
    public void setup() {
        eventBus.register(ClusterEvent.class, this);
    }

    @Override
    public void handle(final ClusterEvent event) {
        final Cluster cluster = context.getClusters().get(event.getClusterID());

        if (cluster == null) {
            log.warn("cluster(id: {}) not exists", event.getClusterID());
        } else {
            cluster.handle(event);
        }
    }
}
