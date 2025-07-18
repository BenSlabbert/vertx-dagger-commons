/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging;

import dagger.BindsInstance;
import dagger.Component;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunnerFactory;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcTransactionManager;
import io.vertx.core.Vertx;
import jakarta.inject.Singleton;

@Singleton
@Component
public interface Provider {

  MyInbox myInbox();

  JdbcQueryRunnerFactory jdbcQueryRunnerFactory();

  @Component.Builder
  interface Builder {

    @BindsInstance
    Builder vertx(Vertx vertx);

    @BindsInstance
    Builder jdbcTransactionManager(JdbcTransactionManager jdbcTransactionManager);

    Provider build();
  }
}
