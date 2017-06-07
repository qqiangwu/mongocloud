package edu.reins.mongocloud.support;

import edu.reins.mongocloud.instance.Instance;
import lombok.val;
import org.apache.mesos.Protos;

import java.util.List;

public abstract class TaskMatcher {
    public static boolean matches(final Protos.Offer offer, final Instance instance) {
        return hasSufficientResource(offer.getResourcesList(), instance);
    }

    private static boolean hasSufficientResource(final List<Protos.Resource> offeredResources, final Instance instance) {
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
