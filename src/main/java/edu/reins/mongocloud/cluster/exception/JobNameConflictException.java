package edu.reins.mongocloud.cluster.exception;

import edu.reins.mongocloud.exception.ClientException;

import javax.annotation.Nonnull;

public class JobNameConflictException extends ClientException {
    public JobNameConflictException(@Nonnull final String msg) {
        super(msg);
    }
}
