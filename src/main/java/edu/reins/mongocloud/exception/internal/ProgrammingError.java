package edu.reins.mongocloud.exception.internal;

import edu.reins.mongocloud.exception.FrameworkError;

public class ProgrammingError extends FrameworkError {
    public ProgrammingError(final String msg) {
        super(msg);
    }

    public ProgrammingError(final String msg, Throwable e) {
        super(msg, e);
    }
}
