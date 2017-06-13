package edu.reins.mongocloud.model;

import lombok.Value;

import java.util.Map;

@Value
public final class InstanceLaunchRequest {
    private final InstanceID instanceID;
    private final InstanceDefinition definition;
    private final Map<String, String> env;
}
