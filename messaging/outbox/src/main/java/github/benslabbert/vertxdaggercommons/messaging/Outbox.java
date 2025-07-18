/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging;

import github.benslabbert.txmanager.annotation.AfterCommit;
import github.benslabbert.txmanager.annotation.Transactional;
import github.benslabbert.txmanager.annotation.Transactional.Propagation;
import github.benslabbert.vertxdaggercommons.messaging.commons.DBRow;
import github.benslabbert.vertxdaggercommons.messaging.commons.MultiMapConverter;
import github.benslabbert.vertxdaggercommons.messaging.commons.OutboxRepository;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunnerFactory;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcUtils;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcUtilsFactory;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.stream.Stream;
import org.apache.commons.dbutils.StatementConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Outbox {

  private static final Logger log = LoggerFactory.getLogger(Outbox.class);

  private final OutboxRepository outboxRepository;
  private final JdbcUtils jdbcUtils;
  private final EventBus eventBus;

  @Inject
  Outbox(
      Vertx vertx,
      JdbcUtilsFactory jdbcUtilsFactory,
      JdbcQueryRunnerFactory jdbcQueryRunnerFactory) {
    StatementConfiguration statementConfiguration =
        new StatementConfiguration.Builder()
            .fetchSize(16)
            .queryTimeout(Duration.ofMillis(500L))
            .build();
    this.eventBus = vertx.eventBus();
    this.jdbcUtils = jdbcUtilsFactory.create(statementConfiguration);
    this.outboxRepository =
        new OutboxRepository(jdbcQueryRunnerFactory.create(statementConfiguration));
  }

  @Inject
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void afterCreated() {
    log.info("publish all messages in the outbox");
    try (Stream<DBRow> stream =
        jdbcUtils.stream(
            "select id, address, headers, body from outbox order by id asc", DBRow::map)) {
      stream.forEach(
          outboxRow -> {
            MultiMap headers = MultiMapConverter.decodeHeaders(new JsonObject(outboxRow.headers()));
            String address = outboxRow.address();
            String body = outboxRow.body();
            eventBus.send(address, body, new DeliveryOptions().setHeaders(headers));
            outboxRepository.delete(outboxRow.id());
          });
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_EXISTING)
  public void send(String address, MultiMap headers, JsonObject body) {
    log.info("saving message for address={} to outbox", address);

    long id =
        outboxRepository.insert(
            address, MultiMapConverter.encodeHeaders(headers).encode(), body.encode());

    sendOnEventBusAfterCommit(id, address, headers, body);
  }

  /** keep this in a separate method */
  @AfterCommit
  private void sendOnEventBusAfterCommit(
      long id, String address, MultiMap headers, JsonObject body) {
    log.info("after commit");
    sendOnEventBusAndDeleteInTx(id, address, headers, body);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  private void sendOnEventBusAndDeleteInTx(
      long id, String address, MultiMap headers, JsonObject body) {
    log.info("Sending message={} on the event bus", id);
    eventBus.send(address, body, new DeliveryOptions().setHeaders(headers));
    outboxRepository.delete(id);
  }
}
