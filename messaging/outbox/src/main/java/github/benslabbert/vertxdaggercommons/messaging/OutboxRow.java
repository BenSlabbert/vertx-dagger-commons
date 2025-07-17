/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging;

import java.sql.ResultSet;
import java.sql.SQLException;

record OutboxRow(long id, String address, String headers, String body) {

  static OutboxRow map(ResultSet rs) throws SQLException {
    return new OutboxRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4));
  }
}
