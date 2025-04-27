/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.launcher;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.vertx.core.ThreadingModel;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplicationHooks;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomApplicationHooks implements VertxApplicationHooks {

  private static final Logger log = LoggerFactory.getLogger(CustomApplicationHooks.class);

  static {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");
  }

  @Override
  public void beforeDeployingVerticle(HookContext context) {
    context
        .deploymentOptions()
        .setThreadingModel(ThreadingModel.VIRTUAL_THREAD)
        .setWorkerPoolSize(1)
        .setWorkerPoolName("worker-pool");
  }

  @Override
  public void beforeStartingVertx(HookContext context) {
    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().build();
    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();

    context
        .vertxOptions()
        .setWorkerPoolSize(1)
        .setEventLoopPoolSize(1)
        .setInternalBlockingPoolSize(1)
        .setTracingOptions(new OpenTelemetryOptions(openTelemetry))
        .setPreferNativeTransport(true);
  }

  @Override
  public JsonObject afterConfigParsed(JsonObject config) {

    if (!config.isEmpty()) return config;

    try (var input = getClass().getClassLoader().getResourceAsStream("application.json")) {
      if (null == input) {
        throw VertxException.noStackTrace("application.json not found");
      }

      log.info("loading default config");

      byte[] bytes = input.readAllBytes();
      JsonObject entries = new JsonObject(new String(bytes, StandardCharsets.UTF_8));
      return config.mergeIn(entries, true);
    } catch (IOException e) {
      throw VertxException.noStackTrace(e);
    }
  }
}
