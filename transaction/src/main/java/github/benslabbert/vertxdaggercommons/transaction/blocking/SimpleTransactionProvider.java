/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.transaction.blocking;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.sql.DataSource;

@Module
final class SimpleTransactionProvider {

  private SimpleTransactionProvider() {}

  @Provides
  @Singleton
  static SimpleTransactionManager simpleTransactionManager(DataSource dataSource) {
    return new SimpleTransactionManager(dataSource);
  }
}
