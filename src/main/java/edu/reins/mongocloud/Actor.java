package edu.reins.mongocloud;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface Actor<E extends Event> {
    /**
     * @param event
     * @throws RuntimeException for any potential exceptions
     */
    void handle(E event);
}