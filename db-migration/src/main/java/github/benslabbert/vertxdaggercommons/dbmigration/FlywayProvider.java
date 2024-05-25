/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.dbmigration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

public final class FlywayProvider {

  private FlywayProvider() {}

  public static Flyway get(
      String host, int port, String username, String password, String database) {

    return Flyway.configure()
        .failOnMissingLocations(true)
        .table("schema_version")
        .locations("classpath:/migration")
        .sqlMigrationPrefix("V")
        .sqlMigrationSeparator("__")
        .connectRetries(2)
        .cleanDisabled(false)
        .validateMigrationNaming(true)
        .dataSource(
            "jdbc:postgresql://%s:%d/%s".formatted(host, port, database), username, password)
        .baselineVersion(MigrationVersion.fromVersion("0"))
        .target(MigrationVersion.LATEST)
        .load();
  }
}
