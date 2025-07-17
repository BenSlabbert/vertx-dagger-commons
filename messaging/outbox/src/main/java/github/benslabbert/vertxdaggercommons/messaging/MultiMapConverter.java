/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;

final class MultiMapConverter {

  private MultiMapConverter() {}

  static MultiMap decodeHeaders(JsonObject json) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();

    for (String name : json.fieldNames()) {
      JsonArray jsonArray = json.getJsonArray(name);
      List<String> values = jsonArray.stream().map(s -> (String) s).toList();
      map.add(name, values);
    }

    return map;
  }

  static JsonObject encodeHeaders(MultiMap map) {
    JsonObject json = new JsonObject();

    for (String name : map.names()) {
      List<String> all = map.getAll(name);
      JsonArray arr = new JsonArray();
      all.forEach(arr::add);
      json.put(name, arr);
    }

    return json;
  }
}
