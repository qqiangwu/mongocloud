package reins.wuqq.support;

import lombok.SneakyThrows;
import org.apache.mesos.state.State;
import org.apache.mesos.state.Variable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class PersistedState<A> {
    @Nonnull
    private final State state;

    @Nonnull
    private final A defaultValue;

    @Nonnull
    private final Function<byte[], A> deserializer;

    @Nonnull
    private final Function<A, byte[]> serializer;

    @Nonnull
    private Variable var;

    @Nullable
    private A parsedValue;


    public PersistedState(
            @Nonnull final String variableName,
            @Nonnull final State state,
            @Nonnull final Supplier<A> defaultValue,
            @Nonnull final Function<byte[], A> deserializer,
            @Nonnull final Function<A, byte[]> serializer
    ) {
        this.deserializer = deserializer;
        this.serializer = serializer;
        this.defaultValue = defaultValue.get();
        this.state = state;
        this.var = await(state.fetch(variableName));
    }

    @SneakyThrows
    private Variable await(Future<Variable> fetch) {
        return fetch.get();
    }

    public final A get() {
        if (parsedValue == null) {
            parsedValue = parseValue();
        }

        return checkNonnull(parsedValue);
    }

    private A checkNonnull(final A parsedValue) {
        if (parsedValue == null) {
            throw new NullPointerException();
        }

        return parsedValue;
    }

    private A parseValue() {
        final byte[] value = var.value();
        final A retVal;
        if (value.length > 0) {
            retVal = deserializer.apply(value);
        } else {
            final A newValue = defaultValue;
            var = store(newValue);
            retVal = newValue;
        }
        return retVal;
    }

    protected final void setValue(@Nonnull final A newValue) {
        var = store(newValue);
        parsedValue = newValue;
    }

    private Variable store(final A newValue) {
        return await(state.store(var.mutate(serializer.apply(newValue))));
    }
}
