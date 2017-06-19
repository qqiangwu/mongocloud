package edu.reins.mongocloud.instance;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.Context;
import edu.reins.mongocloud.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class InstanceEventDispatcher implements Actor<InstanceEvent> {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Context context;

    @PostConstruct
    public void setup() {
        eventBus.register(InstanceEvent.class, this);
    }

    @Override
    public void handle(final InstanceEvent event) {
        final Instance instance = context.getInstances().get(event.getInstanceID());

        if (instance == null) {
            LOG.warn("instance(id: {}) not exists", event.getInstanceID());
        } else {
            instance.handle(event);
        }
    }
}
