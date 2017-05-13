package edu.reins.mongocloud;

import edu.reins.mongocloud.model.Instance;
import org.apache.mesos.Protos.TaskStatus;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 *
 * 接受来自资源层的实例的状态更新。
 *
 * 要求全部幂等
 *
 */
@ThreadSafe
public interface ResourceStatusListener {
    void onPlatformPrepared();      // scheduler registered
    void onClusterDestroyed();      // framework removed
    void onFailover();              // framework failover to another instance

    /**
     * The instance is submitted to Mesos. The ip and slaveID is properly setup
     */
    void onInstanceLaunched(@Nonnull Instance instance);

    /**
     * The instance is running
     */
    void onInstanceRunning(@Nonnull TaskStatus status);

    /**
     * The instance is lost due to slave failure.
     */
    void onInstanceFailed(@Nonnull TaskStatus status);

    /**
     * The instance is killed by the executor.
     */
    void onInstanceKilled(@Nonnull TaskStatus status);

    /**
     * There are some errors to prevent the framework to run the instance:
     * 1. The instance description is ill-formed
     * 2. Authentication failed
     * 3. The instance is launched by the executor, but failed (TaskStatus.TASK_FAIL)
     */
    void onInstanceError(@Nonnull TaskStatus status);
}
