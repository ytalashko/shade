package com.dark.shade.internal.accessor;

import java.lang.reflect.InvocationTargetException;

/**
 * Accessor.
 */
public interface Accessor {

  Class getType();

  Object get(Object entity) throws IllegalAccessException, InvocationTargetException;

  void set(Object entity, Object value) throws IllegalAccessException, InvocationTargetException;
}
