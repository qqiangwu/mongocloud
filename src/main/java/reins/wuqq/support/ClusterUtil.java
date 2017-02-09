package reins.wuqq.support;

import reins.wuqq.model.Instance;

public abstract class ClusterUtil {
    public static final String getTaskName(final Instance instance) {
        return String.format("%s.%s", instance.getName(), instance.getId());
    }
}
