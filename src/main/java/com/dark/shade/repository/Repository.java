package com.dark.shade.repository;

import java.util.List;

/**
 * Repository.
 */
public interface Repository<T, ID> {

  T save(T entity);

  List<T> save(Iterable<T> entities);

  T findOne(ID id);

  boolean exists(ID id);

  List<T> findAll();

  List<T> findAll(Iterable<ID> ids);

  void delete(ID id);

  void deleteAll();
}
