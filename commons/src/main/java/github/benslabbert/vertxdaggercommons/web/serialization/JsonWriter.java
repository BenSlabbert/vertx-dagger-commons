/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.web.serialization;

import io.vertx.core.json.JsonObject;

public interface JsonWriter {

  JsonObject toJson();
}
