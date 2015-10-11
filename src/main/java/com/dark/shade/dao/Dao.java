package com.dark.shade.dao;

/**
 * DAO.
 */
public interface Dao<T, ID> {

  T save(T entity);

  Iterable<T> save(Iterable<T> entities);

  T findOne(ID id);

  boolean exists(ID id);

  Iterable<T> findAll();

  Iterable<T> findAll(Iterable<ID> ids);

  void delete(ID id);

  void deleteAll();
}
