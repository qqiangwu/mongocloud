package edu.reins.mongocloud.exception.internal;

import edu.reins.mongocloud.exception.FrameworkError;

import javax.annotation.Nonnull;

public class NotImplementedError extends FrameworkError {
    public NotImplementedError(@Nonnull final String msg) {
        super(msg);
    }
}
