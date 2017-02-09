package reins.wuqq.cluster;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reins.wuqq.model.ClusterState;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ClusterConfiguration {
    @Bean
    public Map<ClusterState, StateHandler> stateHandlers(final List<StateHandler> handlers) {
        return handlers.stream()
                .collect(Collectors.toMap(x -> x.getState(), Function.identity()));
    }
}
