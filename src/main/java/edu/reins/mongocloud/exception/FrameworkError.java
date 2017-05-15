package edu.reins.mongocloud.exception;

import javax.annotation.Nonnull;

/**
 * Errors caused by programmers.
 */
public class FrameworkError extends Error {
    public FrameworkError(@Nonnull final String msg) {
        super(msg);
    }

    public FrameworkError(@Nonnull final String msg, @Nonnull Throwable e) {
        super(msg, e);
    }
}