package edu.reins.mongocloud;

import edu.reins.mongocloud.support.annotation.Nothrow;

public interface Fsm<S extends Enum<S>, E extends Event> extends Actor<E> {
    @Nothrow
    S getState();

    /**
     * @param event
     * @throws RuntimeException for any potential exceptions
     */
    void handle(E event);
}
