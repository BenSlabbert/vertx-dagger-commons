/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.test;

import github.benslabbert.vertxdaggercommons.config.Config;
import io.vertx.core.json.JsonObject;

public final class ConfigEncoder {

  private ConfigEncoder() {}

  public static JsonObject encode(Config config) {
    String key = "__emptyPlaceholder";
    String encode = new JsonObject().put(key, config).encode();
    return new JsonObject(encode).getJsonObject(key);
  }
}
