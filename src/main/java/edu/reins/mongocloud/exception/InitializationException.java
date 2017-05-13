package edu.reins.mongocloud.exception;

import javax.annotation.Nonnull;

public class InitializationException extends RuntimeException {
    public InitializationException(@Nonnull final String msg) {
        super(msg);
    }
}
