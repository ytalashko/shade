package com.dark.shade.internal;

import com.dark.shade.exception.ShadeException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Entity manager.
 */
public final class EntityManager {

  private final HashMap<Class, Class> primitiveTypesBinding;

  private static EntityManager manager;
  private Session session;

  private EntityManager() {
    primitiveTypesBinding = new HashMap<>();
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

  public void persist(EntityMetadata metadata, Object entity) {
    checkOpen();
    PreparedStatement statement = null;
    try {
      Object id = metadata.getIdAccessor().get(entity);
      String prefix = "";
      List<String> columnNames = new ArrayList<>(metadata.getColumnAccessors().keySet());
      if (id == null) {
        StringBuilder namesBuilder = new StringBuilder(String.format("INSERT INTO %s (", metadata.getTableName()));
        StringBuilder valuesBuilder = new StringBuilder(") VALUES (");
        for (String columnName : columnNames) {
          namesBuilder.append(prefix).append(columnName);
          valuesBuilder.append(prefix).append("?");
          prefix = ", ";
        }
        statement = session.prepareStatement(namesBuilder.append(valuesBuilder).append(")").toString(), Statement.RETURN_GENERATED_KEYS);

        int index = 1;
        for (String columnName : columnNames) {
          setValue(statement, index++, metadata.getColumnAccessors().get(columnName).get(entity));
        }
      } else {
        StringBuilder sqlBuilder = new StringBuilder(String.format("UPDATE %s SET ", metadata.getTableName()));
        for (String columnName : columnNames) {
          sqlBuilder.append(prefix).append(columnName).append("=?");
          prefix = ", ";
        }
        statement = session.prepareStatement(
            sqlBuilder.append(String.format(" WHERE %s=?", metadata.getIdName())).toString());

        int index = 1;
        for (String columnName : columnNames) {
          setValue(statement, index++, metadata.getColumnAccessors().get(columnName).get(entity));
        }
        setValue(statement, index, id);
      }
      statement.executeUpdate();
      commit();
      ResultSet generatedKeysResultSet = statement.getGeneratedKeys();
      if (generatedKeysResultSet.next()) {
//        TODO: need to check, maybe better use generatedKeysResultSet.getLong(1)
        metadata.getIdAccessor().set(entity, generatedKeysResultSet.getObject(1));
      }
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException ignored) {
        }
      }
    }
  }

  public void removeAll(EntityMetadata metadata) {
    checkOpen();
    PreparedStatement statement = null;
    try {
      statement = session.prepareStatement(String.format("DELETE FROM %s", metadata.getTableName()));
      statement.executeUpdate();
      commit();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException ignored) {
        }
      }
    }
  }

  public void remove(EntityMetadata metadata, Object primaryKey) {
    checkOpen();
    PreparedStatement statement = null;
    try {
      statement = session.prepareStatement(String.format(
          "DELETE FROM %s WHERE %s=?", metadata.getTableName(), metadata.getIdName()));
      setValue(statement, 1, primaryKey);

      statement.executeUpdate();
      commit();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException ignored) {
        }
      }
    }
  }

  public <T> List<T> findAll(EntityMetadata metadata, Class<T> entityClass) {
    checkOpen();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      statement = session.prepareStatement(String.format("SELECT * FROM %s", metadata.getTableName()));
      resultSet = statement.executeQuery();
      return parseResultSet(metadata, entityClass, resultSet);
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException ignored) {
        }
      }
      if (resultSet != null) {
        try {
          resultSet.close();
        } catch (SQLException ignored) {
        }
      }
    }
  }

  public <T> T find(EntityMetadata metadata, Class<T> entityClass, Object primaryKey) {
    checkOpen();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      statement = session.prepareStatement(String.format(
          "SELECT * FROM %s WHERE %s=?", metadata.getTableName(), metadata.getIdName()));
      setValue(statement, 1, primaryKey);
      resultSet = statement.executeQuery();
      List<T> entities = parseResultSet(metadata, entityClass, resultSet);
      if (entities.isEmpty()) {
        return null;
      }
      return entities.iterator().next();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException ignored) {
        }
      }
      if (resultSet != null) {
        try {
          resultSet.close();
        } catch (SQLException ignored) {
        }
      }
    }
  }

  public boolean contains(EntityMetadata metadata, Object primaryKey) {
    checkOpen();
    PreparedStatement statement = null;
    try {
//      TODO: improve query to use (COUNT(*) > 0)
      statement = session.prepareStatement(String.format(
          "SELECT * FROM %s WHERE %s=?", metadata.getTableName(), metadata.getIdName()));
      setValue(statement, 1, primaryKey);
      return statement.executeQuery().next();
    } catch (Exception e) {
      throw new ShadeException(e.getMessage(), e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException ignored) {
        }
      }
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
      throw new ShadeException(String.format("Cannot close database connection: %s", e.getMessage()), e);
    }
  }

  private <T> List<T> parseResultSet(EntityMetadata metadata, Class<T> entityClass, ResultSet resultSet)
      throws SQLException, IllegalAccessException, InstantiationException,
      InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
    List<T> entities = new ArrayList<>();
    while (resultSet.next()) {
      T entity = entityClass.newInstance();
      metadata.getIdAccessor().set(entity, getValue(
          resultSet, metadata.getIdName(), metadata.getIdAccessor().getType()));
      for (String columnName : metadata.getColumnAccessors().keySet()) {
        metadata.getColumnAccessors().get(columnName).set(entity, getValue(
            resultSet, columnName,  metadata.getColumnAccessors().get(columnName).getType()));
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
      } catch (SQLException ignored) {
      }
      throw new ShadeException(String.format("Failed to commit changes: %s", e.getMessage()), e);
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
}
