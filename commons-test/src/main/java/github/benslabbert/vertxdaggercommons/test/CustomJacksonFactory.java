/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.test;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.spi.JsonFactory;
import io.vertx.core.spi.json.JsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomJacksonFactory implements JsonFactory {

  private static final Logger log = LoggerFactory.getLogger(CustomJacksonFactory.class);

  public CustomJacksonFactory() {
    log.info("loading CustomCodec");
  }

  @Override
  public JsonCodec codec() {
    DatabindCodec databindCodec = new DatabindCodec();
    DatabindCodec.mapper().registerModule(new JavaTimeModule());
    return databindCodec;
  }

  @Override
  public int order() {
    return 1;
  }
}
