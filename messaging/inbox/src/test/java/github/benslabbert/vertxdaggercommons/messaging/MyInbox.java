/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging;

import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunnerFactory;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcUtilsFactory;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MyInbox extends Inbox {

  private static final Logger log = LoggerFactory.getLogger(MyInbox.class);

  @Inject
  MyInbox(
      Vertx vertx,
      JdbcUtilsFactory jdbcUtilsFactory,
      JdbcQueryRunnerFactory jdbcQueryRunnerFactory) {
    super(vertx, jdbcUtilsFactory, jdbcQueryRunnerFactory);
  }

  @Override
  protected String address() {
    return "address";
  }

  @Override
  protected void handle(Message<JsonObject> msg) {
    log.info("handle message {}", msg.body());
    try {
      Thread.sleep(Duration.ofSeconds(2L));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
