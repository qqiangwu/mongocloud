package reins.wuqq.cluster.handler;

import org.springframework.stereotype.Component;
import reins.wuqq.model.ClusterState;

@Component
public class ScaleOutHandler extends AbstractStateHandler {
    @Override
    public ClusterState getState() {
        return ClusterState.SCALE_OUT;
    }
}
