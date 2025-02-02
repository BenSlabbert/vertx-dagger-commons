/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.saga;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.Vertx;
import javax.inject.Singleton;

@Singleton
@Component(modules = {SagaModule.class})
public interface Provider {

  SagaBuilder sagaBuilder();

  @Component.Builder
  interface Builder {

    @BindsInstance
    Builder vertx(Vertx vertx);

    Provider build();
  }
}
