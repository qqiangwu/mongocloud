package edu.reins.mongocloud.support;

import edu.reins.mongocloud.model.Instance;

public abstract class ClusterUtil {
    public static final String getTaskName(final Instance instance) {
        return String.format("%s.%s", instance.getName(), instance.getId());
    }
}
