package edu.reins.mongocloud;

/**
 * @author wuqq
 */
public interface EventBus {
    <E extends Event> void register(Class<E> eventType, Actor<E> actor);
    void post(Event event);
}