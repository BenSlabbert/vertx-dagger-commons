/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.saga;

import static github.benslabbert.vertxdaggercommons.saga.Headers.SAGA_ID_HEADER;
import static github.benslabbert.vertxdaggercommons.saga.Headers.SAGA_ROLLBACK_HEADER;

import com.google.auto.value.AutoValue;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoValue
abstract class SagaStage {

  private static final Logger log = LoggerFactory.getLogger(SagaStage.class);

  SagaStage() {}

  abstract SagaStageHandler handler();

  abstract String commandAddress();

  abstract EventBus eventBus();

  Future<Message<JsonObject>> sendCommand(String sagaId) {
    log.info("{}: sending command to: {}", sagaId, commandAddress());

    return handler()
        .getCommand(sagaId)
        .compose(
            message ->
                eventBus()
                    .request(
                        commandAddress(),
                        message,
                        new DeliveryOptions()
                            .setSendTimeout(Duration.ofSeconds(5L).toMillis())
                            .addHeader(SAGA_ID_HEADER, sagaId)));
  }

  Future<Message<Void>> sendRollbackCommand(String sagaId) {
    log.info("{}: sending rollback command to: {}", sagaId, commandAddress());

    return handler()
        .onRollBack(sagaId)
        .compose(
            ignore ->
                eventBus()
                    .request(
                        commandAddress(),
                        null,
                        new DeliveryOptions()
                            .addHeader(SAGA_ID_HEADER, sagaId)
                            .addHeader(SAGA_ROLLBACK_HEADER, Boolean.TRUE.toString())));
  }

  Future<Boolean> handleResult(String sagaId, Message<JsonObject> result) {
    log.info("{}: handle result", sagaId);
    return handler().handleResult(sagaId, result);
  }

  static Builder builder() {
    return new AutoValue_SagaStage.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder handler(SagaStageHandler handler);

    abstract Builder commandAddress(String commandAddress);

    abstract Builder eventBus(EventBus eventBus);

    abstract SagaStage build();
  }
}
