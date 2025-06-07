/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.dbmigration;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;

public final class FlywayProvider {

  private FlywayProvider() {}

  public static Flyway get(DataSource dataSource) {
    return baseConfiguration().dataSource(dataSource).load();
  }

  public static Flyway get(
      String host, int port, String username, String password, String database) {
    String url = "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
    return baseConfiguration().dataSource(url, username, password).load();
  }

  private static FluentConfiguration baseConfiguration() {
    return Flyway.configure()
        .failOnMissingLocations(true)
        .table("schema_version")
        .locations("classpath:/migration")
        .sqlMigrationPrefix("V")
        .sqlMigrationSeparator("__")
        .connectRetries(2)
        .cleanDisabled(false)
        .validateMigrationNaming(true)
        .baselineVersion(MigrationVersion.fromVersion("0"))
        .target(MigrationVersion.LATEST);
  }
}
