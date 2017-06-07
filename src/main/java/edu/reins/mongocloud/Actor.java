package edu.reins.mongocloud;

/**
 * @author wuqq
 */
public interface Actor<E extends Event> {
    /**
     * @param event
     * @throws RuntimeException for any potential exceptions
     */
    void handle(E event);
}