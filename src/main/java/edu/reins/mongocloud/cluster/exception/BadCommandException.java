package edu.reins.mongocloud.cluster.exception;

import edu.reins.mongocloud.exception.internal.ProgrammingException;

import javax.annotation.Nonnull;

public class BadCommandException extends ProgrammingException {
    public BadCommandException(@Nonnull String msg) {
        super(msg);
    }
}
