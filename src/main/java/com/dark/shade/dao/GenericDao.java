package com.dark.shade.dao;

import com.dark.shade.internal.EntityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic DAO.
 */
public class GenericDao<T, ID> implements Dao<T, ID> {

  private final Class<T> type;
  private final EntityManager em;

  public GenericDao(Class<T> type) {
    this.type = type;
    em = EntityManager.getManager();
  }

  public T save(T entity) {
    em.persist(entity);
    return entity;
  }

  public Iterable<T> save(Iterable<T> entities) {
    List<T> savedEntities = new ArrayList<T>();
    for (T entity : entities) {
      em.persist(entity);
      savedEntities.add(entity);
    }
    return savedEntities;
  }

  public T findOne(ID id) {
    return em.find(type, id);
  }

  public boolean exists(ID id) {
    return em.contains(type, id);
  }

  public Iterable<T> findAll() {
    return em.findAll(type);
  }

  public Iterable<T> findAll(Iterable<ID> ids) {
    List<T> foundEntities = new ArrayList<T>();
    for (ID id : ids) {
      foundEntities.add(em.find(type, id));
    }
    return foundEntities;
  }

  public void delete(ID id) {
    em.remove(type, id);
  }

  public void deleteAll() {
    em.removeAll(type);
  }
}
