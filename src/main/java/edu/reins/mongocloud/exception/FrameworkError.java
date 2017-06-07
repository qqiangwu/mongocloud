package edu.reins.mongocloud.exception;

/**
 * Errors caused by programmers.
 */
public class FrameworkError extends Error {
    public FrameworkError(final String msg) {
        super(msg);
    }

    public FrameworkError(final String msg, Throwable e) {
        super(msg, e);
    }
}