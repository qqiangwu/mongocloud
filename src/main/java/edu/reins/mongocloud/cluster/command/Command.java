package edu.reins.mongocloud.cluster.command;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nonnull;

@ToString
@EqualsAndHashCode
public final class Command {
    private final CommandType type;
    private final Object payload;
    private boolean processed = false;
    private RuntimeException exception;

    public Command(@Nonnull final CommandType type) {
        this(type, null);
    }

    public Command(@Nonnull final CommandType type, final Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public CommandType getType() {
        return type;
    }

    public <T> T getPayload(@Nonnull final Class<T> clazz) {
        return clazz.cast(payload);
    }

    public synchronized RuntimeException getException() {
        return exception;
    }

    public void setException(@Nonnull final RuntimeException e) {
        exception = e;
    }

    public synchronized void finished() {
        processed = true;

        notifyAll();
    }

    public void waitForFinish() {
        while (!processed) {
            try {
                wait();
            } catch (InterruptedException e) {
                    /* ignore */
            }
        }
    }
}
