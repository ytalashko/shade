package com.dark.shade.internal.parser;

import com.dark.shade.annotation.Column;
import com.dark.shade.annotation.Entity;
import com.dark.shade.annotation.Id;
import com.dark.shade.annotation.Table;
import com.dark.shade.exception.ShadeException;
import com.dark.shade.internal.EntityMetadata;
import com.dark.shade.internal.accessor.Accessor;
import com.dark.shade.internal.accessor.member.FieldAccessor;
import com.dark.shade.internal.accessor.member.MethodAccessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity parser.
 */
public final class EntityParser {

  public static EntityMetadata parseEntity(Class entityClass)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    String entityName = parseAnnotationName(entityClass, Entity.class, entityClass.getSimpleName());
    if (entityName == null) {
      throw new ShadeException(String.format(
          "Entity class must be annotated with %s annotation", Entity.class.getCanonicalName()));
    }
    String tableName = parseAnnotationName(entityClass, Table.class, entityClass.getSimpleName());
    if (tableName == null) {
      tableName = entityClass.getSimpleName();
    }
    Map<String, Accessor> idAccessor = new HashMap<>(1);
    Map<String, Accessor> columnAccessors = parseColumns(entityClass, idAccessor);
    if (idAccessor.isEmpty()) {
      throw new ShadeException(String.format(
          "Entity class must have member with %s annotation", Id.class.getCanonicalName()));
    }
    return new EntityMetadata(entityName, tableName,
        idAccessor.keySet().iterator().next(), idAccessor.values().iterator().next(), columnAccessors);
  }

  private static <T> String parseAnnotationName(Class entityClass, Class<T> annotationClass, String defaultValue)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    if (entityClass.isAnnotationPresent(annotationClass)) {
      Annotation annotation = entityClass.getAnnotation(annotationClass);
      String name = (String) annotationClass.getDeclaredMethod("name").invoke(annotation);
      if (name.equals("")) {
        name = defaultValue;
      }
      return name;
    }
    return null;
  }

  private static Map<String, Accessor> parseColumns(Class entityClass,
                                                    Map<String, Accessor> idAccessor) throws NoSuchMethodException {
    Map<String, Accessor> columnAccessors = new HashMap<>();
    for (Field field : entityClass.getDeclaredFields()) {
      if (field.isAnnotationPresent(Id.class)) {
        if (!idAccessor.isEmpty()) {
          throw new ShadeException(String.format(
              "Entity class must have ONLY ONE %s annotation", Id.class.getCanonicalName()));
        }
        String idName = parseColumnFromMember(field, field.getName());
        idName = idName == null ? field.getName() : idName;
        idAccessor.put(idName, new FieldAccessor(field));
      } else {
        String columnName = parseColumnFromMember(field, field.getName());
        if (columnName != null && columnAccessors.put(columnName, new FieldAccessor(field)) != null) {
          throw new ShadeException(String.format(
              "Entity class must have ONLY ONE %s with name %s", Column.class.getCanonicalName(), columnName));
        }
      }
    }
    for (Method method : entityClass.getDeclaredMethods()) {
      String defaultValue = method.getName().replaceFirst("(?:get|is).",
          method.getName().replaceFirst("get|is", "").substring(0, 1).toLowerCase());
      if (method.isAnnotationPresent(Id.class)) {
        if (!idAccessor.isEmpty()) {
          throw new ShadeException(String.format(
              "Entity class must have ONLY ONE %s annotation", Id.class.getCanonicalName()));
        }
        String idName = parseColumnFromMember(method, defaultValue);
        idName = idName == null ? defaultValue : idName;
        idAccessor.put(idName, new MethodAccessor(method,
            entityClass.getDeclaredMethod(method.getName().replaceFirst("get|is", "set"), method.getReturnType())));
      } else {
        String columnName = parseColumnFromMember(method, defaultValue);
        if (columnName != null && columnAccessors.put(columnName,
            new MethodAccessor(method, entityClass.getDeclaredMethod(
                method.getName().replaceFirst("get|is", "set"), method.getReturnType()))) != null) {
          throw new ShadeException(String.format(
              "Entity class must have ONLY ONE %s with name %s", Column.class.getCanonicalName(), columnName));
        }
      }
    }
    return columnAccessors;
  }

  private static String parseColumnFromMember(AccessibleObject accessibleObject, String defaultValue) {
    if (accessibleObject.isAnnotationPresent(Column.class)) {
      Column columnAnnotation = accessibleObject.getAnnotation(Column.class);
      String column = columnAnnotation.name();
      if (column.equals("")) {
        column = defaultValue;
      }
      return column;
    }
    return null;
  }
}
