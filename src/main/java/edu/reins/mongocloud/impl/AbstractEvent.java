package edu.reins.mongocloud.impl;

import edu.reins.mongocloud.Event;

/**
 * @author wuqq
 */
public abstract class AbstractEvent<EventType extends Enum<EventType>> implements Event<EventType> {
    private final EventType type;

    protected AbstractEvent(final EventType type) {
        this.type = type;
    }

    @Override
    public EventType getType() {
        return type;
    }
}