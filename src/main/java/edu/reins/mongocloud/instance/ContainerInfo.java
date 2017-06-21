package edu.reins.mongocloud.instance;

import lombok.Value;

@Value
public class ContainerInfo {
    private final String name;
    private final String ip;
}
