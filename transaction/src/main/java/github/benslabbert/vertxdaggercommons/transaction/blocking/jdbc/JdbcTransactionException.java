/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc;

public class JdbcTransactionException extends RuntimeException {
  public JdbcTransactionException(Throwable cause) {
    super(cause);
  }
}
