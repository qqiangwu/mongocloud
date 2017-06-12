package edu.reins.mongocloud.model;

import lombok.Value;

@Value
public final class InstanceLaunchRequest {
    private final InstanceID instanceID;
    private final InstanceDefinition definition;
}
