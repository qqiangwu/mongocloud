package edu.reins.mongocloud.cluster.exception;

import edu.reins.mongocloud.exception.internal.ProgrammingException;

import javax.annotation.Nonnull;

public class JobDetachedException extends ProgrammingException {
    public JobDetachedException(@Nonnull final String msg) {
        super(msg);
    }
}
