package edu.reins.mongocloud.support;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

public abstract class Errors {
    public static <T extends Exception> Supplier<T> throwException(final Class<T> clazz, final String msg) {
        return () -> {
            try {
                return clazz.getConstructor(String.class).newInstance(msg);
            } catch (InstantiationException
                    | IllegalAccessException
                    | NoSuchMethodException
                    | InvocationTargetException e) {
                throw new AssertionError("create exception class failed");
            }
        };
    }
}