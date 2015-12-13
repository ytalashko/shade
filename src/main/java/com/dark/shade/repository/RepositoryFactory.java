package com.dark.shade.repository;

import com.dark.shade.exception.ShadeException;
import com.dark.shade.internal.EntityManager;

import java.lang.reflect.InvocationTargetException;

/**
 * Repository factory.
 */
public final class RepositoryFactory {

  public static void init(String url, String user, String pass) {
    EntityManager.getManager().createSession(url, user, pass);
  }

  public static <T, ID> Repository<T, ID> createDao(Class<T> type) {
    try {
      return new GenericRepository<>(type);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new ShadeException(
          String.format("Failed to create repository for entity class %s: %s", type, e.getMessage()), e);
    }
  }
}
