package edu.reins.mongocloud.impl;

import edu.reins.mongocloud.Event;
import edu.reins.mongocloud.support.annotation.Nothrow;

import java.util.Objects;

public abstract class AbstractEvent<EventType extends Enum<EventType>> implements Event<EventType> {
    private final EventType type;
    private final Object payload;

    @Nothrow
    protected AbstractEvent(final EventType type) {
        this(type, null);
    }

    @Nothrow
    protected AbstractEvent(final EventType type, final Object payload) {
        this.type = type;
        this.payload = payload;
    }

    @Nothrow
    @Override
    public EventType getType() {
        return type;
    }

    @Nothrow
    @Override
    public <T> T getPayload(final Class<T> clazz) {
        Objects.requireNonNull(payload);

        return clazz.cast(payload);
    }
}