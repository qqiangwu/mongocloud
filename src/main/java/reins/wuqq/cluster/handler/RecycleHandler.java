package reins.wuqq.cluster.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reins.wuqq.model.ClusterState;

@Component
@Slf4j(topic = "cluster.Recycle")
public class RecycleHandler extends AbstractStateHandler {
    @Override
    public ClusterState getState() {
        return ClusterState.RECYCLE;
    }

    @Override
    public void enter() {
        super.enter();

        log.info("enter");

        ensureShardCount();
    }

    private void ensureShardCount() {

    }
}
