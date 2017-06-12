package edu.reins.mongocloud.cluster;

import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
public final class ConfigClusterMeta {
    private final String name;
    private final List<String> members;

    public String toID() {
        final String membersEncoded = members.stream().collect(Collectors.joining(","));

        return String.format("%s/%s", name, membersEncoded);
    }
}
