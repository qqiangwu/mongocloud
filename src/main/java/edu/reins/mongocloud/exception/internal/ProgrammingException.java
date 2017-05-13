package edu.reins.mongocloud.exception.internal;

import edu.reins.mongocloud.exception.InternalException;

import javax.annotation.Nonnull;

public class ProgrammingException extends InternalException {
    public ProgrammingException(@Nonnull final String msg) {
        super(msg);
    }

    public ProgrammingException(@Nonnull final String msg, @Nonnull Throwable e) {
        super(msg, e);
    }
}
