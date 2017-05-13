package edu.reins.mongocloud;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * All objects stored must be serializable!
 */
@ThreadSafe
public interface Store {
    String META_PREFIX = ".";

    <T extends Serializable> void put(@Nonnull String key, @Nonnull T value);
    void clear(@Nonnull String key);
    <T extends Serializable> Optional<T> get(@Nonnull String key, @Nonnull Class<T> clazz);
    List<String> keys();
}
