/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.jooq;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;

@Module
final class PreparedStatementDslContextConfig {

  private PreparedStatementDslContextConfig() {}

  @Provides
  @Singleton
  @Named("prepared")
  static DSLContext dslContext() {
    // settings for jdbc
    Settings settings =
        new Settings()
            .withParamType(ParamType.INDEXED)
            .withStatementType(StatementType.PREPARED_STATEMENT);
    return DSL.using(SQLDialect.POSTGRES, settings);
  }
}
