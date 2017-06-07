package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ClusterImpl implements Actor<ClusterEvent> {
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void setup() {
        eventBus.register(ClusterEvent.class, this);
    }

    @Override
    public void handle(final ClusterEvent event) {

    }
}
