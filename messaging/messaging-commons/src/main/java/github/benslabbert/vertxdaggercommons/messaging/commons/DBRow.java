/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging.commons;

import java.sql.ResultSet;
import java.sql.SQLException;

public record DBRow(long id, String address, String headers, String body) {

  public static DBRow map(ResultSet rs) throws SQLException {
    return new DBRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4));
  }

  static long mapInsert(ResultSet rs) throws SQLException {
    if (rs.next()) {
      return rs.getLong(1);
    }

    throw new SQLException("Failed to insert message");
  }
}
