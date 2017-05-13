package edu.reins.mongocloud.cluster;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ClusterConfiguration {
    @Bean
    public Map<ClusterState, ClusterPipelineProcessor> clusterPipelineProcessors(
            final List<ClusterPipelineProcessor> processors) {
        return processors.stream()
                .collect(Collectors.toMap(ClusterPipelineProcessor::getState, Function.identity()));
    }
}
