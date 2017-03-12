package reins.wuqq.support;

import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.apache.mesos.Protos;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@ToString
public final class ResourceDescriptor {
    private final double cpus;
    private final long memory;
    private final long disk;
    private final List<Integer> ports = new ArrayList<>();

    public ResourceDescriptor(@Nonnull final List<Protos.Resource> resources) {
        val resourceMap = resources
                .stream()
                .collect(Collectors.toMap(x -> x.getName(), Function.identity()));

        cpus = getDoubleOrDefault(resourceMap, "cpus", 0.0D);
        memory = getLongOrDefault(resourceMap, "mem", 0L);
        disk = getLongOrDefault(resourceMap, "disk", 0L);

        for (val r: resources) {
            if (r.getName().equals("ports")) {
                ports.add((int) r.getRanges().getRange(0).getBegin());
            }
        }
    }

    private double getDoubleOrDefault(@Nonnull final Map<String, Protos.Resource> resources,
                                      @Nonnull final String key,
                                      @Nonnull double defaultValue) {
        val r = resources.get(key);
        return r == null? defaultValue: r.getScalar().getValue();
    }

    private long getLongOrDefault(@Nonnull final Map<String, Protos.Resource> resources,
                                      @Nonnull final String key,
                                      @Nonnull long defaultValue) {
        val r = resources.get(key);
        return r == null? defaultValue: (long) r.getScalar().getValue();
    }
}
