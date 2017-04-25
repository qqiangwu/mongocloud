package reins.wuqq;

import org.apache.mesos.Protos.TaskStatus;
import reins.wuqq.model.Instance;

import javax.annotation.Nonnull;

/**
 *
 * 接受来自资源层的实例的状态更新。
 */
public interface ResourceStatusListener {
    void onPlatformPrepared();
    void onClusterDestroyed();
    void onInstanceLaunched(@Nonnull Instance instance);
    void onInstanceStarted(@Nonnull TaskStatus status);
    void onInstanceLost(@Nonnull TaskStatus status);
}
