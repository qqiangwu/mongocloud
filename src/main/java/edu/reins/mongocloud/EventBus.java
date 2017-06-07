package edu.reins.mongocloud;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface EventBus {
    <E extends Event> void register(Class<E> eventType, Actor<E> actor);
    void post(Event event);
}