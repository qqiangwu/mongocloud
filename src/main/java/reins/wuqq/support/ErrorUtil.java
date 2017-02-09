package reins.wuqq.support;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public abstract class ErrorUtil {
    public static final Supplier<RuntimeException> thrower(@Nonnull final String message) {
        return () -> new RuntimeException(message);
    }
}