package com.dark.shade.internal;

import com.dark.shade.annotation.Column;
import com.dark.shade.annotation.Id;
import com.dark.shade.annotation.Table;
import com.dark.shade.exception.ShadeException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Entity manager.
 */
public final class EntityManager {

  private final class Tuple {

    private final String name;
    private final Object value;

    private Tuple(String name, Object value) {
      this.name = name;
      this.value = value;
    }
  }

  private final HashMap<Class, Class> primitiveTypesBinding;

  private static EntityManager manager;
  private Session session;

  private EntityManager() {
    primitiveTypesBinding = new HashMap<Class, Class>();
    primitiveTypesBinding.put(Boolean.class, boolean.class);
    primitiveTypesBinding.put(Byte.class, byte.class);
    primitiveTypesBinding.put(Short.class, short.class);
    primitiveTypesBinding.put(Integer.class, int.class);
    primitiveTypesBinding.put(Long.class, long.class);
    primitiveTypesBinding.put(Float.class, float.class);
    primitiveTypesBinding.put(Double.class, double.class);
  }

  // TODO: Hook to initialize Session: EntityManager.getManager().createSession(url, user, pass);
  public static EntityManager getManager() {
    if (manager == null) {
      manager = new EntityManager();
    }
    return manager;
  }

  public void persist(Object entity) {
    checkOpen();
    Class eClass = entity.getClass();
    checkEntity(eClass);
    String table = parseTable(eClass);
    Tuple id = parseId(eClass);
    try {
      Object value;
      ((AccessibleObject) id.value).setAccessible(true);
      if (id.value instanceof Field) {
        value = ((Field) id.value).get(entity);
      } else {
        value = ((Method) id.value).invoke(entity);
      }

      List<Tuple> columns = parseColumns(eClass);
      if (value == null) {
        String prefix = "";
        StringBuilder namesBuilder = new StringBuilder(String.format("INSERT INTO %s (", table));
        StringBuilder valuesBuilder = new StringBuilder(") VALUES (");
        for (Tuple column : columns) {
          if (!column.name.equals(id.name)) {
            namesBuilder.append(prefix).append(column.name);
            valuesBuilder.append(prefix).append("?");
            prefix = ", ";
          }
        }
        PreparedStatement statement = session.prepareStatement(
            namesBuilder.append(valuesBuilder).append(")").toString());

        int index = 1;
        for (Tuple column : columns) {
          if (!column.name.equals(id.name)) {
            Object columnValue;
            ((AccessibleObject) column.value).setAccessible(true);
            if (column.value instanceof Field) {
              columnValue = ((Field) column.value).get(entity);
            } else {
              columnValue = ((Method) column.value).invoke(entity);
            }
            setValue(statement, index++, columnValue);
          }
        }
        statement.executeUpdate();
      } else {
        String prefix = "";
        StringBuilder sqlBuilder = new StringBuilder(String.format("UPDATE %s SET ", table));
        for (Tuple column : columns) {
          if (!column.name.equals(id.name)) {
            sqlBuilder.append(prefix).append(column.name).append("=?");
            prefix = ", ";
          }
        }
        PreparedStatement statement = session.prepareStatement(
            sqlBuilder.append(String.format(" WHERE %s=?", id.name)).toString());

        int index = 1;
        for (Tuple column : columns) {
          if (!column.name.equals(id.name)) {
            Object columnValue;
            ((AccessibleObject) column.value).setAccessible(true);
            if (column.value instanceof Field) {
              columnValue = ((Field) column.value).get(entity);
            } else {
              columnValue = ((Method) column.value).invoke(entity);
            }
            setValue(statement, index++, columnValue);
          }
        }
        setValue(statement, index, value);
        statement.executeUpdate();
      }
      commit();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    }
  }

  public <T> T merge(T entity) {
//    checkOpen();
//    Class<T> tClass = (Class<T>) entity.getClass();
//    checkEntity(tClass);
//    return null;
    throw new ShadeException("This can hurt even a shade...");
  }

  //  TODO: remove this method later
  public <T> void removeAll(Class<T> entityClass) {
    checkOpen();
    checkEntity(entityClass);
    String table = parseTable(entityClass);
    try {
      PreparedStatement statement = session.prepareStatement(String.format("DELETE FROM %s", table));
      statement.executeUpdate();
      commit();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    }
  }

//  TODO: remove this method later
  public <T> void remove(Class<T> entityClass, Object primaryKey) {
    checkOpen();
    checkEntity(entityClass);
    String table = parseTable(entityClass);
    Tuple id = parseId(entityClass);
    try {
      PreparedStatement statement = session.prepareStatement(String.format(
          "DELETE FROM %s WHERE %s=?", table, id.name));
      setValue(statement, 1, primaryKey);

      statement.executeUpdate();
      commit();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    }
  }

  public <T> void remove(T entity) {
    checkOpen();
    Class<T> tClass = (Class<T>) entity.getClass();
    checkEntity(tClass);
    String table = parseTable(tClass);
    Tuple id = parseId(tClass);
    try {
      Object value;
      ((AccessibleObject) id.value).setAccessible(true);
      if (id.value instanceof Field) {
        value = ((Field) id.value).get(entity);
      } else {
        value = ((Method) id.value).invoke(entity);
      }

      PreparedStatement statement = session.prepareStatement(String.format(
          "DELETE FROM %s WHERE %s=?", table, id.name));
      setValue(statement, 1, value);

      statement.executeUpdate();
      commit();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    }
  }

//  TODO: remove this method later
  public <T> List<T> findAll(Class<T> entityClass) {
    checkOpen();
    checkEntity(entityClass);
    String table = parseTable(entityClass);
    try {
      PreparedStatement statement = session.prepareStatement(String.format("SELECT * FROM %s", table));
      ResultSet resultSet = statement.executeQuery();
      return parseResultSet(entityClass, resultSet);
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    }
  }

  public <T> T find(Class<T> entityClass, Object primaryKey) {
    checkOpen();
    checkEntity(entityClass);
    String table = parseTable(entityClass);
    Tuple id = parseId(entityClass);
    try {
      PreparedStatement statement = session.prepareStatement(String.format(
          "SELECT * FROM %s WHERE %s=?", table, id.name));
      setValue(statement, 1, primaryKey);
      ResultSet resultSet = statement.executeQuery();
      List<T> entities = parseResultSet(entityClass, resultSet);
      if (entities.isEmpty()) {
        throw new ShadeException(String.format("Entity with id=%s not found", primaryKey));
      }
      return entities.iterator().next();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    }
  }

//  TODO: remove this method later
  public <T> boolean contains(Class<T> entityClass, Object primaryKey) {
    checkOpen();
    checkEntity(entityClass);
    String table = parseTable(entityClass);
    Tuple id = parseId(entityClass);
    try {
      PreparedStatement statement = session.prepareStatement(String.format(
          "SELECT * FROM %s WHERE %s=?", table, id.name));
      setValue(statement, 1, primaryKey);
      return statement.executeQuery().next();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    }
  }

  public boolean contains(Object entity) {
    checkOpen();
    Class eClass = entity.getClass();
    checkEntity(eClass);
    String table = parseTable(eClass);
    Tuple id = parseId(eClass);
    try {
      Object value;
      ((AccessibleObject) id.value).setAccessible(true);
      if (id.value instanceof Field) {
        value = ((Field) id.value).get(entity);
      } else {
        value = ((Method) id.value).invoke(entity);
      }

      PreparedStatement statement = session.prepareStatement(String.format(
          "SELECT * FROM %s WHERE %s=?", table, id.name));
      setValue(statement, 1, value);
      return statement.executeQuery().next();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    }
  }

  public void createSession(String url, String user, String pass) {
    if (session != null) {
      throw new ShadeException("Session already exists");
    }
    try {
      session = new Session(url, user, pass);
    } catch (SQLException e) {
      throw new ShadeException(String.format(
          "Cannot create database connection to %s with user %s and pass %s: %s", url, user, pass, e.getMessage()), e);
    }
  }

  public void closeSession() {
    checkOpen();
    try {
      session.close();
    } catch (SQLException e) {
      throw new ShadeException(String.format("Cannot close connection: %s", e.getMessage()), e);
    }
  }

  private <T> List<T> parseResultSet(Class<T> entityClass, ResultSet resultSet)
      throws SQLException, IllegalAccessException, InstantiationException,
      InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
    List<T> entities = new ArrayList<T>();
    List<Tuple> columns = parseColumns(entityClass);
    while (resultSet.next()) {
      T entity = entityClass.newInstance();
      for (Tuple tuple : columns) {
        ((AccessibleObject) tuple.value).setAccessible(true);
        if (tuple.value instanceof Field) {
          Class fClass = ((Field) tuple.value).getType();
          ((Field) tuple.value).set(entity, getValue(resultSet, tuple.name, fClass));
        } else {
          Class mClass = ((Method) tuple.value).getReturnType();
          Method setter = entityClass.getMethod("set" + ((Method) tuple.value).getName().substring(3), mClass);
          setter.setAccessible(true);
          setter.invoke(entity, getValue(resultSet, tuple.name, mClass));
        }
      }
      entities.add(entity);
    }
    return entities;
  }

  private Object getValue(ResultSet resultSet, String columnLabel, Class mClass)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
    Class<ResultSet> resultSetClass = ResultSet.class;
    String type = mClass.getSimpleName();
    type = type.substring(0, 1).toUpperCase() + type.substring(1);
    type = type.equals(Integer.class.getSimpleName()) ? Integer.class.getSimpleName().substring(0, 3) : type;
    Method getter = resultSetClass.getMethod("get" + type, String.class);
    return getter.invoke(resultSet, columnLabel);
  }

  private void setValue(PreparedStatement statement, int index, Object value)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
    Class<PreparedStatement> statementClass = PreparedStatement.class;
    String type = value.getClass().getSimpleName();
    type = type.equals(Integer.class.getSimpleName()) ? Integer.class.getSimpleName().substring(0, 3) : type;
    Method setter = statementClass.getMethod("set" + type, int.class, castToSimpleType(value.getClass()));
    setter.invoke(statement, index, value);
  }

  private Class castToSimpleType(Class vClass) {
    return primitiveTypesBinding.get(vClass) != null ? primitiveTypesBinding.get(vClass) : vClass;
  }

  private void commit() {
    try {
      session.commit();
    } catch (SQLException e) {
      try {
        session.rollback();
        throw new ShadeException(String.format("Failed to commit changes: %s", e.getMessage()), e);
      } catch (SQLException e1) {
        throw new ShadeException(String.format(
            "Failed to commit changes: %s\nRollback fails: %s", e.getMessage(), e1.getMessage()), e1);
      }
    }
  }

  private void checkOpen() {
    checkSession();
    try {
      if (session.isClosed()) {
        throw new ShadeException("Database connection is closed.");
      }
    } catch (SQLException e) {
      throw new ShadeException(String.format(
          "Cannot check database connection for open/closed state: %s", e.getMessage()), e);
    }
  }

  private void checkSession() {
    if (session == null) {
      throw new ShadeException("Database connection is not created.");
    }
  }

  private <T> void checkEntity(Class<T> tClass) {
    if (tClass.isAnnotationPresent(Table.class)) {
      boolean found = checkMembersForId(tClass.getDeclaredFields());
      if (checkMembersForId(tClass.getDeclaredMethods())) {
        if (found) {
          throw new ShadeException(String.format(
              "Entity class must have ONLY ONE %s annotation", Id.class.getCanonicalName()));
        }
      } else {
        if (!found) {
          throw new ShadeException(String.format(
              "Entity class must have member with %s annotation", Id.class.getCanonicalName()));
        }
      }
    } else {
      throw new ShadeException(String.format(
          "Entity class must be annotated with %s annotation", Table.class.getCanonicalName()));
    }
  }

  private <T extends AccessibleObject & Member> boolean checkMemberForId(T object) {
    return object.isAnnotationPresent(Id.class);
  }

  private <T extends AccessibleObject & Member> boolean checkMembersForId(T[] objects) {
    boolean found = false;
    for (T member : objects) {
      if (checkMemberForId(member)) {
        if (found) {
          throw new ShadeException(String.format(
              "Entity class must have ONLY ONE %s annotation", Id.class.getCanonicalName()));
        }
        found = true;
      }
    }
    return found;
  }

  private <T> String parseTable(Class<T> tClass) {
//    if (tClass.isAnnotationPresent(Table.class)) {
      Table tableAnnotation = tClass.getAnnotation(Table.class);
      String table = tableAnnotation.name();
      if (table.equals("")) {
        table = tClass.getSimpleName();
      }
      return table;
//    } else {
//      throw new ShadeException(String.format(
//          "Entity class must be annotated with %s annotation", Table.class.getCanonicalName()));
//    }
  }

  private <T extends AccessibleObject & Member> Tuple parseIdFromMember(T object) {
    String id = null;
    if (object.isAnnotationPresent(Id.class)) {
      if (object.isAnnotationPresent(Column.class)) {
        Column columnAnnotation = object.getAnnotation(Column.class);
        id = columnAnnotation.name();
        if (id.equals("")) {
          id = object.getName();
        }
      } else {
        id = object.getName();
      }
    }
    return id == null ? null : new Tuple(id, object);
  }

  private <T extends AccessibleObject & Member> Tuple parseIdFromMembers(T[] objects) {
    Tuple id;
    for (T member : objects) {
      id = parseIdFromMember(member);
      if (id != null) {
        return id;
      }
    }
    return null;
  }

  private <T> Tuple parseId(Class<T> tClass) {
    Tuple id = parseIdFromMembers(tClass.getDeclaredFields());
    if (id == null) {
      id = parseIdFromMembers(tClass.getDeclaredMethods());
//      if (id == null) {
//        throw new ShadeException(String.format(
//            "Entity class must have member with %s annotation", Id.class.getCanonicalName()));
//      }
    }
    return id;
  }

  private <T extends AccessibleObject & Member> Tuple parseColumnFromMember(T object) {
    String column = null;
    if (object.isAnnotationPresent(Column.class)) {
      Column columnAnnotation = object.getAnnotation(Column.class);
      column = columnAnnotation.name();
      if (column.equals("")) {
        column = object.getName();
      }
    } else if (object.isAnnotationPresent(Id.class)) {
      column = object.getName();
    }
    return column == null ? null : new Tuple(column, object);
  }

  private <T extends AccessibleObject & Member> List<Tuple> parseColumnFromMembers(T[] objects) {
    List<Tuple> columns = new ArrayList<Tuple>();
    for (T member : objects) {
      Tuple column = parseColumnFromMember(member);
      if (column != null) {
        columns.add(column);
      }
    }
    return columns;
  }

  private <T> List<Tuple> parseColumns(Class<T> tClass) {
    List<Tuple> columns = parseColumnFromMembers(tClass.getDeclaredFields());
    columns.addAll(parseColumnFromMembers(tClass.getDeclaredMethods()));
    return columns;
  }
}
