/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.dbutils.AbstractQueryRunner;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JdbcUtils extends AbstractQueryRunner {

  private static final Logger log = LoggerFactory.getLogger(JdbcUtils.class);

  private final JdbcTransactionManager jdbcTransactionManager;

  @Inject
  JdbcUtils(JdbcTransactionManager jdbcTransactionManager) {
    // we want an empty super constructor on purpose
    super();
    this.jdbcTransactionManager = jdbcTransactionManager;
  }

  @FunctionalInterface
  public interface DoInTransaction<T> {
    T apply(Connection conn) throws SQLException;
  }

  @FunctionalInterface
  public interface RunInTransaction {
    void accept(Connection conn) throws SQLException;
  }

  @FunctionalInterface
  public interface UseTransaction<T> {
    T apply(Connection conn) throws SQLException;
  }

  @FunctionalInterface
  public interface ResultSetMapper<T> {
    T apply(ResultSet rs) throws SQLException;
  }

  private static class Wrapper {
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    private void wrap(PreparedStatement statement) {
      this.statement = statement;
    }

    private void wrap(ResultSet resultSet) {
      this.resultSet = resultSet;
    }

    private void close() {
      DbUtils.closeQuietly(resultSet);
      DbUtils.closeQuietly(statement);
    }
  }

  /** Stream the results of a query in a dedicated transaction. */
  public <T> Stream<T> streamInTransaction(
      String sql, ResultSetMapper<T> mapper, Object... params) {
    try {
      jdbcTransactionManager.begin();
      return stream(sql, mapper, params).onClose(jdbcTransactionManager::commit);
    } catch (Exception e) {
      jdbcTransactionManager.rollback();
      throw new QueryException(e);
    }
  }

  /** Stream the results of a query in an existing transaction. */
  public <T> Stream<T> stream(String sql, ResultSetMapper<T> mapper, Object... params) {
    Wrapper wrapper = new Wrapper();
    try {
      Connection connection = jdbcTransactionManager.getConnection();
      PreparedStatement statement = prepareStatement(connection, sql);
      fillStatement(statement, params);
      wrapper.wrap(statement);
      ResultSet rs = statement.executeQuery();
      wrapper.wrap(rs);
      return Stream.generate(
              () -> {
                try {
                  if (rs.isClosed()) {
                    log.debug("stream: result set is closed, returning null");
                    return null;
                  }

                  if (rs.next()) {
                    log.debug("stream: next element in result set");
                    return mapper.apply(rs);
                  }

                  log.debug("stream: no more elements in result set");
                  return null;
                } catch (Exception e) {
                  throw new QueryException(e);
                }
              })
          .takeWhile(Objects::nonNull)
          .onClose(wrapper::close);
    } catch (Exception e) {
      log.atDebug().setMessage("stream: exception while streaming results").setCause(e).log();
      wrapper.close();
      throw new QueryException(e);
    }
  }

  /** use an existing transaction */
  public <T> T useTransaction(UseTransaction<T> function) {
    try {
      Connection conn = jdbcTransactionManager.getConnection();
      return function.apply(conn);
    } catch (Exception e) {
      throw new QueryException(e);
    }
  }

  /** execute in dedicated transaction */
  public <T> T doInTransaction(DoInTransaction<T> function) {
    try {
      jdbcTransactionManager.begin();
      Connection conn = jdbcTransactionManager.getConnection();
      T res = function.apply(conn);
      jdbcTransactionManager.commit();
      return res;
    } catch (Exception e) {
      jdbcTransactionManager.rollback();
      throw new QueryException(e);
    }
  }

  /** execute in dedicated transaction */
  public void runInTransaction(RunInTransaction function) {
    try {
      jdbcTransactionManager.begin();
      Connection conn = jdbcTransactionManager.getConnection();
      function.accept(conn);
      jdbcTransactionManager.commit();
    } catch (Exception e) {
      jdbcTransactionManager.rollback();
      throw new QueryException(e);
    }
  }
}
