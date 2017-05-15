package edu.reins.mongocloud.exception.internal;

import edu.reins.mongocloud.exception.FrameworkError;

import javax.annotation.Nonnull;

public class ProgrammingError extends FrameworkError {
    public ProgrammingError(@Nonnull final String msg) {
        super(msg);
    }

    public ProgrammingError(@Nonnull final String msg, @Nonnull Throwable e) {
        super(msg, e);
    }
}
