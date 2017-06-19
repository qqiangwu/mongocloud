package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.Event;
import edu.reins.mongocloud.Fsm;
import edu.reins.mongocloud.model.ClusterID;
import org.squirrelframework.foundation.fsm.AnonymousCondition;
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

    public static <T extends Event> Condition<T> sizeIs(final List<?> list, final int size) {
        return new AnonymousCondition<T>() {
            @Override
            public boolean isSatisfied(T t) {
                return list.size() == size;
            }
        };
    }

    public static <T extends Event, S extends Enum<S>> Condition<T> stateIs(
            final List<? extends Fsm<S, ?>> list, final S state) {
        return new AnonymousCondition<T>() {
            @Override
            public boolean isSatisfied(T t) {
                return list.stream().allMatch(fsm -> fsm.getState() == state);
            }
        };
    }

    public static <T extends Event, S extends Enum<S>> Condition<T> statePartiallyIs(
            final List<? extends Fsm<S, ?>> list, final S state) {
        return new AnonymousCondition<T>() {
            @Override
            public boolean isSatisfied(T t) {
                return list.stream().anyMatch(fsm -> fsm.getState() != state);
            }
        };
    }
}
