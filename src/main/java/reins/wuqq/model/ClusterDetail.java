package reins.wuqq.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.util.Map;

@Data
public class ClusterDetail {
    String name;
    ClusterState state = ClusterState.PREPARING_CONFIG;

    @Nullable
    Instance configServer;
    @Nullable
    Instance proxyServer;

    int shardsNeeded;
    Map<String, Instance> shards;
}
