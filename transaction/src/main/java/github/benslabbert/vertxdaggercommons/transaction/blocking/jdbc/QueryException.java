/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc;

public class QueryException extends RuntimeException {
  public QueryException(Throwable cause) {
    super(cause);
  }
}
