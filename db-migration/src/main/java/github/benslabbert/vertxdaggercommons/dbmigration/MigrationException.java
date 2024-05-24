/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.dbmigration;

public class MigrationException extends RuntimeException {
  public MigrationException(Throwable cause) {
    super(cause);
  }
}
