package edu.reins.mongocloud.impl;

import edu.reins.mongocloud.Event;

import java.util.Objects;

/**
 * @author wuqq
 */
public abstract class AbstractEvent<EventType extends Enum<EventType>> implements Event<EventType> {
    private final EventType type;
    private final Object payload;

    protected AbstractEvent(final EventType type) {
        this(type, null);
    }

    protected AbstractEvent(final EventType type, final Object payload) {
        this.type = type;
        this.payload = payload;
    }

    @Override
    public EventType getType() {
        return type;
    }

    public <T> T getPayload(final Class<T> clazz) {
        Objects.requireNonNull(payload);

        return clazz.cast(payload);
    }
}