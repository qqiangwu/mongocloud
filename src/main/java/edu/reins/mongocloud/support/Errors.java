package edu.reins.mongocloud.support;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public abstract class Errors {
    public static Supplier<RuntimeException> thrower(@Nonnull final String message) {
        return () -> new RuntimeException(message);
    }
}