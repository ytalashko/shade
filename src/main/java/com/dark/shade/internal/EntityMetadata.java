package com.dark.shade.internal;

import com.dark.shade.internal.accessor.Accessor;

import java.util.Map;

/**
 * Entity metadata.
 */
public final class EntityMetadata {

  private final String entityName;
  private final String tableName;
  private final String idName;
  private final Accessor idAccessor;
  private final Map<String, Accessor> columnAccessors;

  public EntityMetadata(String entityName, String tableName,
                        String idName, Accessor idAccessor, Map<String, Accessor> columnAccessors) {
    this.entityName = entityName;
    this.tableName = tableName;
    this.idName = idName;
    this.idAccessor = idAccessor;
    this.columnAccessors = columnAccessors;
  }

  public String getEntityName() {
    return entityName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getIdName() {
    return idName;
  }

  public Accessor getIdAccessor() {
    return idAccessor;
  }

  public Map<String, Accessor> getColumnAccessors() {
    return columnAccessors;
  }
}
