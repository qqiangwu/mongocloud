package reins.wuqq;

import org.apache.mesos.Protos.TaskStatus;
import reins.wuqq.model.Instance;

import javax.annotation.Nonnull;

/**
 *
 * 接受来自资源层的节点的状态更新。
 */
public interface ResourceStatusListener {
    void onPlatformPrepared();
    void onNodeLaunched(@Nonnull Instance instance);
    void onNodeStarted(@Nonnull TaskStatus status);
    void onNodeLost(@Nonnull TaskStatus status);
}
