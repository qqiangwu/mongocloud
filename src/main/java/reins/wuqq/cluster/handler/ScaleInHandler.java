package reins.wuqq.cluster.handler;

import org.springframework.stereotype.Component;
import reins.wuqq.model.ClusterState;

@Component
public class ScaleInHandler extends AbstractStateHandler {
    @Override
    public ClusterState getState() {
        return ClusterState.SCALE_IN;
    }
}
