package reins.wuqq.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Data
public class Instance {
    private InstanceType type;
    private InstanceState state = InstanceState.PREPARING;
    private String name;
    private String id;
    private String image;

    private double cpus;
    private long memory;
    private long disk;

    @Nullable
    private String hostIP;
    @Nullable
    private Integer port;

    private Map<String, String> env = new HashMap<>();
}
