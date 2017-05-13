package edu.reins.mongocloud.exception;

import javax.annotation.Nonnull;

public class ServiceException extends RuntimeException {
    public ServiceException(@Nonnull final String msg) {
        super(msg);
    }

    public ServiceException(@Nonnull final String msg, @Nonnull final Throwable cause) {
        super(msg, cause);
    }
}
