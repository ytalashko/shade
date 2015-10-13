package com.dark.shade.internal.accessor.member;

import com.dark.shade.internal.accessor.Accessor;

import java.lang.reflect.Field;

/**
 * Field accessor.
 */
public final class FieldAccessor implements Accessor {

 private final Field field;

  public FieldAccessor(Field field) {
    field.setAccessible(true);
    this.field = field;
  }

  @Override
  public Class getType() {
    return field.getType();
  }

  @Override
  public Object get(Object entity) throws IllegalAccessException {
    return field.get(entity);
  }

  @Override
  public void set(Object entity, Object value) throws IllegalAccessException {
    field.set(entity, value);
  }
}
