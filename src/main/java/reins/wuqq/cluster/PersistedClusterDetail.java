package reins.wuqq.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.mesos.state.ZooKeeperState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reins.wuqq.model.ClusterDetail;
import reins.wuqq.model.ClusterState;
import reins.wuqq.model.Instance;
import reins.wuqq.support.PersistedState;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class PersistedClusterDetail extends PersistedState<ClusterDetail> {
    private static final String CLUSTER_DETAIL_VAR = "clusterDetail";

    private static ObjectMapper mapper = new ObjectMapper();

    private static final Supplier<ClusterDetail> DEFAULT_VALUE_GENERATOR = () -> {
        final ClusterDetail detail = new ClusterDetail();

        detail.setName("Mongo-M");
        detail.setShardsNeeded(3);

        return detail;
    };
    private static final Function<ClusterDetail, byte[]> DEFAULT_SERIALIZER = input -> {
        try {
            return mapper.writeValueAsBytes(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
    private static final Function<byte[], ClusterDetail> DEFAULT_DESERIALIZER = input -> {
        try {
            return mapper.readValue(input, ClusterDetail.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    @Autowired
    public PersistedClusterDetail(final ZooKeeperState state) {
        super(CLUSTER_DETAIL_VAR, state, DEFAULT_VALUE_GENERATOR, DEFAULT_DESERIALIZER, DEFAULT_SERIALIZER);
    }

    public ClusterState getState() {
        return get().getState();
    }

    public void setState(@Nonnull final ClusterState state) {
        val v = get();

        v.setState(state);

        setValue(v);
    }

    public void addInstance(@Nonnull final Instance instance) {
        val v = get();

        switch (instance.getType()) {
            case PROXY_SERVER:
                v.setProxyServer(instance);
                break;

            case CONFIG_SERVER:
                v.setConfigServer(instance);
                break;

            case SHARD:
                v.getShards().put(instance.getId(), instance);
                v.setShardsNeeded(v.getShardsNeeded() - 1);
                break;
        }

        setValue(v);
    }

    public Optional<Instance> getConfigServer() {
        return Optional.ofNullable(get().getConfigServer());
    }

    public Optional<Instance> getProxyServer() {
        return Optional.ofNullable(get().getProxyServer());
    }

    public Optional<Instance> getShardServer(@Nonnull final String instanceID) {
        return Optional.ofNullable(get().getShards().get(instanceID));
    }

    public Collection<Instance> getShards() {
        return get().getShards().values();
    }

    public void updateInstance(@Nonnull Instance instance) {
        switch (instance.getType()) {
            case CONFIG_SERVER:
                get().setConfigServer(instance);
                break;

            case PROXY_SERVER:
                get().setProxyServer(instance);
                break;

            case SHARD:
                get().getShards().put(instance.getId(), instance);
                break;
        }

        setValue(get());
    }

    public boolean needMoreShard() {
        return get().getShardsNeeded() > 0;
    }

    public boolean hasSuperfluousShards() {
        return getShards().size() > get().getShardsNeeded();
    }

    public void incrementShardNumber(final int count) {
        val v = get();

        v.setShardsNeeded(v.getShardsNeeded() + count);

        setValue(v);
    }

    public void decrementShardNumber(final int count) {
        val v = get();

        v.setShardsNeeded(v.getShardsNeeded() - count);

        setValue(v);
    }

    public void removeInstance(@Nonnull final Instance instance) {
        switch (instance.getType()) {
            case CONFIG_SERVER:
                get().setConfigServer(null);
                break;

            case PROXY_SERVER:
                get().setProxyServer(null);
                break;

            case SHARD:
                get().getShards().remove(instance.getId());
                break;
        }

        setValue(get());
    }
}
