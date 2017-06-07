package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.Cluster;
import edu.reins.mongocloud.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * @author wuqq
 */
@Component
@Slf4j
public class InstanceEventDispatcher implements Actor<InstanceEvent> {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Cluster cluster;

    @PostConstruct
    public void setup() {
        eventBus.register(InstanceEvent.class, this);
    }

    @Override
    public void handle(final InstanceEvent event) {
        final Optional<Instance> instance = cluster.getInstance(event.getInstanceID());

        if (!instance.isPresent()) {
            log.warn("instance(id: {}) not exists", event.getInstanceID());
        }

        instance.ifPresent(inst -> inst.handle(event));
    }
}
