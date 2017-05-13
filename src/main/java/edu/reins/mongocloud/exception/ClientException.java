package edu.reins.mongocloud.exception;

import javax.annotation.Nonnull;

public class ClientException extends RuntimeException {
    public ClientException(@Nonnull final String msg) {
        super(msg);
    }
}
