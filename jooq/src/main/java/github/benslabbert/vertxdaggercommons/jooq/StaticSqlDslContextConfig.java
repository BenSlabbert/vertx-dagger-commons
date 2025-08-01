/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.jooq;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;

@Module
final class StaticSqlDslContextConfig {

  private StaticSqlDslContextConfig() {}

  @Provides
  @Singleton
  @Named("static")
  static DSLContext dslContext() {
    Settings settings = new Settings().withStatementType(StatementType.STATIC_STATEMENT);
    return DSL.using(SQLDialect.POSTGRES, settings);
  }
}
