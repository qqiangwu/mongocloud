package edu.reins.mongocloud.exception.internal;

import edu.reins.mongocloud.exception.InternalException;

import javax.annotation.Nonnull;

public class NotImplementedException extends InternalException {
    public NotImplementedException(@Nonnull final String msg) {
        super(msg);
    }
}
