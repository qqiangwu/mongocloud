package edu.reins.mongocloud.support;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;

@UtilityClass
public final class Beans {
    public static <T> void overrideIfNonNull(final T source, final T target) {
        try {
            for (final Field field : source.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                final Object value = field.get(source);

                if (value != null) {
                    field.set(target, value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
