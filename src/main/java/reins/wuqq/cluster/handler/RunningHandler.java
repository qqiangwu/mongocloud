package reins.wuqq.cluster.handler;

import org.springframework.stereotype.Component;
import reins.wuqq.model.ClusterState;

@Component
public class RunningHandler extends AbstractStateHandler {
    @Override
    public ClusterState getState() {
        return ClusterState.RUNNING;
    }
}
