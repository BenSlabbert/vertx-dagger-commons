/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.config;

import io.vertx.core.json.JsonObject;

public class ConfigEncoder {

  public static JsonObject encode(Config config) {
    String encode = new JsonObject().put("empty", config).encode();
    return new JsonObject(encode).getJsonObject("empty");
  }
}
