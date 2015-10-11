package com.dark.shade.dao;

import com.dark.shade.internal.EntityManager;

/**
 * DAO factory.
 */
public final class DaoFactory {

  public static void init(String url, String user, String pass) {
    EntityManager.getManager().createSession(url, user, pass);
  }

  public static <T, ID> Dao<T, ID> createDao(Class<T> type) {
    return new GenericDao<>(type);
  }
}
