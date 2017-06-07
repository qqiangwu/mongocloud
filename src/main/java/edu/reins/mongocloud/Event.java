package edu.reins.mongocloud;

/**
 * @author wuqq
 */
public interface Event<EventType extends Enum<EventType>> {
    EventType getType();
}
