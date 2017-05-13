package edu.reins.mongocloud.exception;

import javax.annotation.Nonnull;

public class InternalException extends RuntimeException {
    public InternalException(@Nonnull final String msg) {
        super(msg);
    }

    public InternalException(@Nonnull final String msg, @Nonnull Throwable e) {
        super(msg, e);
    }
}
