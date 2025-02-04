/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.test;

import static org.assertj.core.api.Assertions.assertThat;

import github.benslabbert.vertxdaggercommons.config.Config;
import github.benslabbert.vertxdaggercommons.config.Config.HttpConfig;
import github.benslabbert.vertxdaggercommons.config.Config.JdbcConfig;
import github.benslabbert.vertxdaggercommons.config.Config.PostgresConfig;
import github.benslabbert.vertxdaggercommons.config.Config.RedisConfig;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConfigEncoderTest {

  @Test
  void test() {
    Config config =
        Config.builder()
            .profile(Config.Profile.PROD)
            .httpConfig(HttpConfig.builder().port(123).build())
            .redisConfig(RedisConfig.builder().host("host").port(456).database(1).build())
            .postgresConfig(
                PostgresConfig.builder()
                    .host("host")
                    .port(1)
                    .username("username")
                    .password("password")
                    .database("database")
                    .build())
            .jdbcConfig(
                JdbcConfig.builder().fetchSize(1).queryTimeout(Duration.ofSeconds(1L)).build())
            .build();

    JsonObject encode = ConfigEncoder.encode(config);
    assertThat(encode.encode())
        .isEqualTo(
            "{\"profile\":\"PROD\",\"httpConfig\":{\"port\":123},\"redisConfig\":{\"host\":\"host\",\"port\":456,\"database\":1},\"postgresConfig\":{\"host\":\"host\",\"port\":1,\"username\":\"username\",\"password\":\"password\",\"database\":\"database\"},\"jdbcConfig\":{\"fetchSize\":1,\"queryTimeout\":1.0}}");
  }
}
