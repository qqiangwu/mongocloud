package edu.reins.mongocloud.resourceprovider.support;

import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.apache.mesos.Protos;

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

    @Nothrow
    public ResourceDescriptor(final List<Protos.Resource> resources) {
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

    private double getDoubleOrDefault(final Map<String, Protos.Resource> resources,
                                      final String key,
                                      double defaultValue) {
        val r = resources.get(key);
        return r == null? defaultValue: r.getScalar().getValue();
    }

    private long getLongOrDefault(final Map<String, Protos.Resource> resources,
                                      final String key,
                                      long defaultValue) {
        val r = resources.get(key);
        return r == null? defaultValue: (long) r.getScalar().getValue();
    }
}
