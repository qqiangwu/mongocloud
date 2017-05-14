package edu.reins.mongocloud.resource.impl;

import edu.reins.mongocloud.Store;
import edu.reins.mongocloud.resource.model.FrameworkDetail;
import lombok.val;
import org.apache.mesos.Protos.FrameworkID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Optional;

@Component
public class FrameworkStore {
    private static final String FRAMEWORK_CONFIGURATION_VAR = Store.META_PREFIX + "frameworkConfx";

    @Autowired
    Store persistentStore;

    public Optional<String> getFrameworkId() {
        return persistentStore
                .get(FRAMEWORK_CONFIGURATION_VAR, FrameworkDetail.class)
                .map(FrameworkDetail::getFrameworkId);
    }

    public void setFrameworkId(final @Nonnull FrameworkID id) {
        val detail = persistentStore
                .get(FRAMEWORK_CONFIGURATION_VAR, FrameworkDetail.class)
                .orElse(new FrameworkDetail());

        detail.setFrameworkId(id.getValue());

        persistentStore.put(FRAMEWORK_CONFIGURATION_VAR, detail);
    }

    public void reset() {
        persistentStore.clear(FRAMEWORK_CONFIGURATION_VAR);
    }
}