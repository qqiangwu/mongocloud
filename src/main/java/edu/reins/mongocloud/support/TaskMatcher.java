package edu.reins.mongocloud.support;

import edu.reins.mongocloud.model.InstanceDefinition;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.val;
import org.apache.mesos.Protos;

import java.util.List;

public abstract class TaskMatcher {
    @Nothrow
    public static boolean matches(final Protos.Offer offer, final InstanceDefinition instance) {
        return hasSufficientResource(offer.getResourcesList(), instance);
    }

    @Nothrow
    private static boolean hasSufficientResource(
            final List<Protos.Resource> offeredResources, final InstanceDefinition instance) {
        val offeredResourceDesc = new ResourceDescriptor(offeredResources);

        if (offeredResourceDesc.getCpus() < instance.getCpus()) {
            return false;
        }
        if (offeredResourceDesc.getMemory() < instance.getMemory()) {
            return false;
        }
        if (offeredResourceDesc.getDisk() < instance.getDisk()) {
            return false;
        }
        if (offeredResourceDesc.getPorts().isEmpty()) {
            return false;
        }

        return true;
    }
}
