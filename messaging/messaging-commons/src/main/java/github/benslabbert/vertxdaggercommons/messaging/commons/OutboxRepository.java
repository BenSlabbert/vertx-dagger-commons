/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging.commons;

import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxRepository {

  private static final Logger log = LoggerFactory.getLogger(OutboxRepository.class);

  private static final String INSERT_SQL =
      """
      insert into outbox (address, headers, body)
      values (?, ?, ?)
      returning id
      """;

  private final JdbcQueryRunner jdbcQueryRunner;

  public OutboxRepository(JdbcQueryRunner jdbcQueryRunner) {
    this.jdbcQueryRunner = jdbcQueryRunner;
  }

  public long insert(String address, String headers, String body) {
    return jdbcQueryRunner.insert(INSERT_SQL, DBRow::mapInsert, address, headers, body);
  }

  public void delete(long id) {
    log.info("deleting message for id={}", id);
    int execute = jdbcQueryRunner.execute("delete from outbox where id = ?", id);
    if (1 != execute) {
      throw new IllegalStateException("Failed to delete message %d from the outbox".formatted(id));
    }
  }
}
