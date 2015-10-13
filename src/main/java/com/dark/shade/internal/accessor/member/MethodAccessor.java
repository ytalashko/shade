package com.dark.shade.internal.accessor.member;

import com.dark.shade.internal.accessor.Accessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Method accessor.
 */
public final class MethodAccessor implements Accessor {

  private final Method getter;
  private final Method setter;

  public MethodAccessor(Method getter, Method setter) {
    getter.setAccessible(true);
    setter.setAccessible(true);
    this.getter = getter;
    this.setter = setter;
  }

  @Override
  public Class getType() {
    return getter.getReturnType();
  }

  @Override
  public Object get(Object entity) throws IllegalAccessException, InvocationTargetException {
    return getter.invoke(entity);
  }

  @Override
  public void set(Object entity, Object value) throws IllegalAccessException, InvocationTargetException {
    setter.invoke(entity, value);
  }
}
