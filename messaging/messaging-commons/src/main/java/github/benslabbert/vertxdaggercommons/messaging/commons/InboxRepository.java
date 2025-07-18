/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging.commons;

import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InboxRepository {

  private static final Logger log = LoggerFactory.getLogger(InboxRepository.class);

  private static final String INSERT_SQL =
      """
      insert into inbox (address, headers, body)
      values (?, ?, ?)
      returning id
      """;

  private final JdbcQueryRunner jdbcQueryRunner;

  public InboxRepository(JdbcQueryRunner jdbcQueryRunner) {
    this.jdbcQueryRunner = jdbcQueryRunner;
  }

  public long insert(String address, String headers, String body) {
    return jdbcQueryRunner.insert(INSERT_SQL, DBRow::mapInsert, address, headers, body);
  }

  public void delete(long id) {
    log.info("deleting message for id={}", id);
    int execute = jdbcQueryRunner.execute("delete from inbox where id = ?", id);
    if (1 != execute) {
      throw new IllegalStateException("Failed to delete message %d from the inbox".formatted(id));
    }
  }
}
