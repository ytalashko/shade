package com.dark.shade.repository;

import com.dark.shade.internal.EntityManager;
import com.dark.shade.internal.EntityMetadata;
import com.dark.shade.internal.parser.EntityParser;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic Repository.
 */
public class GenericRepository<T, ID> implements Repository<T, ID> {

  private final Class<T> type;
  private final EntityManager em;
  private final EntityMetadata metadata;

  GenericRepository(Class<T> type)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    this.type = type;
    em = EntityManager.getManager();
    metadata = EntityParser.parseEntity(type);
  }

  public T save(T entity) {
    em.persist(metadata, entity);
    return entity;
  }

  public List<T> save(Iterable<T> entities) {
    List<T> savedEntities = new ArrayList<>();
    for (T entity : entities) {
      em.persist(metadata, entity);
      savedEntities.add(entity);
    }
    return savedEntities;
  }

  public T findOne(ID id) {
    return em.find(metadata, type, id);
  }

  public boolean exists(ID id) {
    return em.contains(metadata, id);
  }

  public List<T> findAll() {
    return em.findAll(metadata, type);
  }

  public List<T> findAll(Iterable<ID> ids) {
    List<T> foundEntities = new ArrayList<>();
    for (ID id : ids) {
      foundEntities.add(em.find(metadata, type, id));
    }
    return foundEntities;
  }

  public void delete(ID id) {
    em.remove(metadata, id);
  }

  public void deleteAll() {
    em.removeAll(metadata);
  }
}
