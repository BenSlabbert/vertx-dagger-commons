/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging;

import github.benslabbert.txmanager.annotation.AfterCommit;
import github.benslabbert.txmanager.annotation.Transactional;
import github.benslabbert.vertxdaggercommons.messaging.commons.DBRow;
import github.benslabbert.vertxdaggercommons.messaging.commons.InboxRepository;
import github.benslabbert.vertxdaggercommons.messaging.commons.MultiMapConverter;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunnerFactory;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcUtils;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcUtilsFactory;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.impl.EventBusImpl;
import io.vertx.core.eventbus.impl.MessageImpl;
import io.vertx.core.eventbus.impl.codecs.JsonObjectMessageCodec;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.stream.Stream;
import org.apache.commons.dbutils.StatementConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Inbox implements Handler<Message<JsonObject>> {

  private static final Logger log = LoggerFactory.getLogger(Inbox.class);

  private final InboxRepository inboxRepository;
  private final JdbcUtils jdbcUtils;
  private final EventBus eventBus;

  @Nullable protected MessageConsumer<JsonObject> consumer;

  protected Inbox(
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
    this.inboxRepository =
        new InboxRepository(jdbcQueryRunnerFactory.create(statementConfiguration));
  }

  protected abstract String address();

  @Inject
  void listen() {
    if (null != this.consumer) {
      throw new IllegalStateException("consumer already created");
    }
    this.consumer =
        eventBus
            .consumer(address(), this::saveToDB)
            .endHandler(ignore -> log.info("stream ended"))
            .exceptionHandler(err -> log.error("stream error", err));
  }

  @Inject
  @Transactional(propagation = Transactional.Propagation.REQUIRES_NEW)
  void afterCreated() {
    log.info("publish all messages in the inbox");
    try (Stream<DBRow> stream =
        jdbcUtils.stream(
            "select id, address, headers, body from inbox where address = ? order by id asc",
            DBRow::map,
            address())) {
      stream.forEach(
          inboxRow -> {
            MultiMap headers = MultiMapConverter.decodeHeaders(new JsonObject(inboxRow.headers()));
            String address = inboxRow.address();
            String body = inboxRow.body();
            callHandlerAndDeleteInTx(
                inboxRow.id(),
                new MessageImpl<>(
                    address,
                    headers,
                    new JsonObject(body),
                    new JsonObjectMessageCodec(),
                    true,
                    (EventBusImpl) eventBus));
          });
    }
  }

  @Transactional(propagation = Transactional.Propagation.REQUIRES_NEW)
  private void saveToDB(Message<JsonObject> msg) {
    long id =
        inboxRepository.insert(
            address(),
            MultiMapConverter.encodeHeaders(msg.headers()).encode(),
            msg.body().encode());

    callHandlerAfterCommit(id, msg);
  }

  @AfterCommit
  private void callHandlerAfterCommit(long id, Message<JsonObject> msg) {
    callHandlerAndDeleteInTx(id, msg);
  }

  @Transactional(propagation = Transactional.Propagation.REQUIRES_NEW)
  private void callHandlerAndDeleteInTx(long id, Message<JsonObject> msg) {
    log.info("calling handler id={}", id);
    handle(msg);
    inboxRepository.delete(id);
  }
}
