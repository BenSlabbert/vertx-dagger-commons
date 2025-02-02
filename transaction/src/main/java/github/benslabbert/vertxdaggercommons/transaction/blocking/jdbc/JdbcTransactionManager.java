/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc;

import github.benslabbert.txmanager.TransactionManager;
import github.benslabbert.vertxdaggercommons.future.FutureUtil;
import java.sql.Connection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JdbcTransactionManager implements TransactionManager {

  private static final Logger log = LoggerFactory.getLogger(JdbcTransactionManager.class);
  private static final ThreadLocal<LocalState> threadLocalState = new ThreadLocal<>();

  private final AtomicBoolean closing = new AtomicBoolean(false);
  private final DataSource dataSource;

  @Inject
  JdbcTransactionManager(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Connection getConnection() {
    LocalState localState = threadLocalState.get();
    if (null == localState) {
      throw new IllegalStateException("cannot get connection: no transaction started");
    }

    Connection connection = localState.connections.peek();
    if (null == connection) {
      throw new IllegalStateException("cannot get connection: no transaction in progress");
    }

    return connection;
  }

  @Override
  public void begin() {
    if (closing.get()) {
      throw new IllegalStateException("cannot begin transaction: transaction manager is closing");
    }

    LocalState localState = threadLocalState.get();
    if (null == localState) {
      threadLocalState.set(LocalState.create());
      localState = threadLocalState.get();
    }

    try {
      Connection connection = dataSource.getConnection();
      log.debug("pushing connection to thread local state");
      localState.connections.push(connection);
    } catch (Exception e) {
      throw new JdbcTransactionException(e);
    }
  }

  @Override
  public void ensureActive() {
    LocalState localState = threadLocalState.get();
    ensureActiveTransaction(localState);
  }

  @Override
  public void commit() {
    LocalState localState = threadLocalState.get();
    if (null == localState || localState.connections.isEmpty()) {
      throw new IllegalStateException("cannot commit: no transaction started");
    }

    log.debug("commit: poll connection from thread local state");
    Connection connection = localState.connections.poll();
    if (null == connection) {
      throw new IllegalStateException("cannot commit: no transaction in progress");
    }

    log.debug("run before commit actions");
    localState.beforeCommitActions.forEach(Runnable::run);

    try {
      DbUtils.commitAndClose(connection);
      log.debug("schedule after commit actions");
      localState.afterCommitActions.forEach(FutureUtil::run);
    } catch (Exception e) {
      throw new JdbcTransactionException(e);
    } finally {
      if (localState.connections.isEmpty()) {
        log.debug("commit: clearing thread local state");
        threadLocalState.remove();
      }
    }
  }

  @Override
  public void beforeCommit(Runnable runnable) {
    LocalState localState = threadLocalState.get();
    ensureActiveTransaction(localState);
    localState.beforeCommitActions.push(runnable);
  }

  @Override
  public void afterCommit(Runnable runnable) {
    LocalState localState = threadLocalState.get();
    ensureActiveTransaction(localState);
    localState.afterCommitActions.push(runnable);
  }

  @Override
  public void rollback() {
    LocalState localState = threadLocalState.get();
    if (null == localState || localState.connections.isEmpty()) {
      throw new IllegalStateException("cannot rollback: no transaction started");
    }

    log.debug("rollback: poll connection from thread local state");
    Connection connection = localState.connections.poll();
    if (null == connection) {
      throw new IllegalStateException("cannot rollback: no transaction in progress");
    }

    try {
      DbUtils.rollbackAndClose(connection);
    } catch (Exception e) {
      throw new JdbcTransactionException(e);
    } finally {
      if (localState.connections.isEmpty()) {
        log.debug("rollback: clearing thread local state");
        threadLocalState.remove();
      }
    }
  }

  @Override
  public void close() {
    log.debug("closing transaction manager");
    closing.set(true);
  }

  private static void ensureActiveTransaction(LocalState localState) {
    if (null == localState || localState.connections.isEmpty()) {
      throw new IllegalStateException("no transaction in progress");
    }
  }

  private record LocalState(
      Deque<Connection> connections,
      Deque<Runnable> beforeCommitActions,
      Deque<Runnable> afterCommitActions) {

    private static LocalState create() {
      return new LocalState(new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
    }
  }
}
