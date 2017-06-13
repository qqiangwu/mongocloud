package edu.reins.mongocloud.cluster.mongo;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MongoEventDispatcher implements Actor<MongoEvent> {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private MongoMediator mongoMediator;

    @PostConstruct
    public void setup() {
        eventBus.register(MongoEvent.class, this);
    }

    @Override
    public void handle(final MongoEvent event) {
        switch (event.getType()) {
            case INIT_RS:
                mongoMediator.initRs(event.getPayload(RsDefinition.class));
                break;

            default:
                throw new AssertionError("Bad MongoEvent");
        }
    }
}
