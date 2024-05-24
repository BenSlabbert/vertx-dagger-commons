/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons;

import github.benslabbert.vertxdaggercommons.ioc.DaggerProvider;
import github.benslabbert.vertxdaggercommons.ioc.Provider;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public abstract class TestBase {

  protected Provider provider;

  @BeforeEach
  void before(Vertx vertx) {
    provider = DaggerProvider.builder().vertx(vertx).build();
  }
}
