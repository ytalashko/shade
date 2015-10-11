package com.dark.shade.internal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Session.
 */
public final class Session {

  private final Connection connection;

  Session(String url, String user, String pass) throws SQLException {
    connection = DriverManager.getConnection(url, user, pass);
    connection.setReadOnly(false);
    connection.setAutoCommit(false);
  }

  PreparedStatement prepareStatement(String sql) throws SQLException {
    return connection.prepareStatement(sql);
  }

  boolean isClosed() throws SQLException {
    return connection.isClosed();
  }

  void commit() throws SQLException {
    connection.commit();
  }

  void rollback() throws SQLException {
    connection.rollback();
  }

  void close() throws SQLException {
    connection.close();
  }
}
