package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Event;
import edu.reins.mongocloud.instance.Instance;
import edu.reins.mongocloud.instance.InstanceState;
import edu.reins.mongocloud.model.ClusterID;
import org.squirrelframework.foundation.fsm.Condition;

import java.util.List;

public abstract class Conditions {
    public static  <T extends Event> Condition<T> eventIsFrom(final Cluster cluster) {
        return new Condition<T>() {
            @Override
            public boolean isSatisfied(final T t) {
                return t.getPayload(ClusterID.class).equals(cluster.getID());
            }

            @Override
            public String name() {
                return String.format("Condition::eventFrom(clusterID: {})", cluster.getID());
            }
        };
    }

    public static <T extends Event> Condition<T> allInstancesRunning(final List<? extends Instance> instances) {
        return new Condition<T>() {
            @Override
            public boolean isSatisfied(T t) {
                return instances.stream().allMatch(instance -> instance.getState().equals(InstanceState.RUNNING));
            }

            @Override
            public String name() {
                return "Condition::allInstancesRunning";
            }
        };
    }

    public static <T extends Event> Condition<T> instancesNotFullyRunning(final List<? extends Instance> instances) {
        return new Condition<T>() {
            @Override
            public boolean isSatisfied(T t) {
                return instances.stream()
                        .anyMatch(instance -> !instance.getState().equals(InstanceState.RUNNING));
            }

            @Override
            public String name() {
                return "Condition::instancesNotFullyRunning";
            }
        };
    }
}
