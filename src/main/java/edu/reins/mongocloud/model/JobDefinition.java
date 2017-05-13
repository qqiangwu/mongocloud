package edu.reins.mongocloud.model;

import lombok.Value;

import java.io.Serializable;

@Value
public final class JobDefinition implements Serializable {
    private final String name;
    private final int count;
}
