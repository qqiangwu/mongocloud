package edu.reins.mongocloud.mongo;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.EventBus;
import edu.reins.mongocloud.MongoMediator;
import edu.reins.mongocloud.mongo.request.RsRequest;
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
                mongoMediator.initRs(event.getPayload(RsRequest.class));
                break;

            default:
                throw new AssertionError("Bad MongoEvent");
        }
    }
}
