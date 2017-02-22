package reins.wuqq.support;

import lombok.Getter;
import lombok.val;
import org.apache.mesos.Protos;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public final class ResourceDescriptor {
    private final double cpus;
    private final long memory;
    private final long disk;
    private final List<Integer> ports = new ArrayList<>();

    public ResourceDescriptor(@Nonnull final List<Protos.Resource> resources) {
        val resouceMap = resources
                .stream()
                .collect(Collectors.toMap(x -> x.getName(), Function.identity()));

        cpus = getOrDefault(resouceMap, "cpus", 0.0D, Double.class);
        memory = getOrDefault(resouceMap, "mem", 0L, Long.class);
        disk = getOrDefault(resouceMap, "disk", 0L, Long.class);
    }

    private <T> T getOrDefault(@Nonnull final Map<String, Protos.Resource> resources,
                               @Nonnull final String key,
                               @Nonnull final T defaultValue,
                               @Nonnull final Class<T> clazz) {
        val r = resources.get(key);

        return (r == null) ? defaultValue :  clazz.cast(r.getScalar());
    }
}
