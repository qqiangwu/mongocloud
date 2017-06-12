package edu.reins.mongocloud;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface Event<EventType extends Enum<EventType>> {
    EventType getType();
    <T> T getPayload(Class<T> clazz);
}
