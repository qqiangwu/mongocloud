package edu.reins.mongocloud;

import edu.reins.mongocloud.exception.ServiceException;
import edu.reins.mongocloud.exception.internal.ProgrammingException;
import edu.reins.mongocloud.support.annotation.SoftState;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.mesos.state.State;
import org.apache.mesos.state.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
@SoftState
@Slf4j
public class StoreImpl implements Store {
    @Autowired
    State persistentState;

    Map<String, Variable> cache = new HashMap<>();

    @Override
    public synchronized <T extends Serializable> void put(@Nonnull final String key, @Nonnull final T value) {
        val v = cache.computeIfAbsent(key, k -> await(persistentState.fetch(key)));

        val bytes = writeValueAsBytes(value);
        val updated = v.mutate(bytes);

        // persist first
        await(persistentState.store(updated));

        cache.put(key, updated);
    }

    private byte[] writeValueAsBytes(final Serializable serializable) {
        try {
            @Cleanup
            val byteBuffer = new ByteArrayOutputStream();
            @Cleanup
            val objectOutput = new ObjectOutputStream(byteBuffer);

            objectOutput.writeObject(serializable);
            objectOutput.flush();

            return byteBuffer.toByteArray();
        } catch (IOException e) {
            log.error("writeFailed", e);

            throw new ProgrammingException("write object failed", e);
        }
    }

    @Override
    public synchronized void clear(@Nonnull final String key) {
        val v = cache.get(key);

        if (v != null) {
            cache.remove(key);
            await(persistentState.expunge(v));
        }
    }

    @Override
    public synchronized <T extends Serializable> Optional<T> get(@Nonnull final String key, @Nonnull final Class<T> clazz) {
        try {
            val v = cache.computeIfAbsent(key, k -> await(persistentState.fetch(key)));

            if (v != null) {
                val bytes = v.value();

                return Optional.of(readValue(bytes, clazz));
            } else {
                return Optional.empty();
            }
        } catch (IOException | ClassNotFoundException e) {
            log.warn("inconsistency(key: {}): remove it", key);

            clear(key);

            return Optional.empty();
        }
    }

    private <T> T readValue(final byte[] bytes, final Class<T> clazz) throws IOException, ClassNotFoundException {
        @Cleanup
        val inBuffer = new ByteArrayInputStream(bytes);
        @Cleanup
        val objectInput = new ObjectInputStream(inBuffer);
        val obj = objectInput.readObject();

        return clazz.cast(obj);
    }

    @Override
    public synchronized List<String> keys() {
        val names = await(persistentState.names());

        return IteratorUtils.toList(names);
    }

    private <T> T await(final Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ServiceException("State access failed", e);
        }
    }
}
