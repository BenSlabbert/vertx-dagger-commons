/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.messaging;

import static github.benslabbert.vertxdaggercommons.thread.VirtualThreadFactory.THREAD_FACTORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import github.benslabbert.txmanager.PlatformTransactionManager;
import github.benslabbert.vertxdaggercommons.config.Config;
import github.benslabbert.vertxdaggercommons.test.DockerContainers;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunner;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunnerFactory;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunnerFactory_Impl;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcQueryRunner_Factory;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcTransactionManager;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcTransactionManager_Factory;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcUtilsFactory;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcUtilsFactory_Impl;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcUtils_Factory;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.inject.Provider;
import org.apache.commons.dbutils.StatementConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;

@ExtendWith(VertxExtension.class)
class OutboxIT {

  protected static final GenericContainer<?> postgres = DockerContainers.POSTGRES;

  static {
    postgres.start();
  }

  static {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private JdbcTransactionManager jdbcTransactionManager;
  private HikariDataSource dataSource;

  @BeforeEach
  void before() {
    Config.PostgresConfig psqlCfg =
        Config.PostgresConfig.builder()
            .host("127.0.0.1")
            .port(postgres.getMappedPort(5432))
            .password("postgres")
            .username("postgres")
            .database("postgres")
            .build();

    HikariConfig hikariConfig = getHikariConfig(psqlCfg);
    dataSource = new HikariDataSource(hikariConfig);

    try (var conn = dataSource.getConnection()) {
      Statement statement = conn.createStatement();
      statement.execute(
          "create table outbox(id serial8 primary key, address text, headers text, body text)");
      conn.commit();
    } catch (Exception e) {
      fail("should create table successfully", e);
    }

    jdbcTransactionManager = JdbcTransactionManager_Factory.newInstance(dataSource);
    PlatformTransactionManager.setTransactionManager(jdbcTransactionManager);
  }

  @AfterEach
  void after() throws Exception {
    if (null != dataSource) {
      dataSource.close();
    }

    PlatformTransactionManager.close();
  }

  @Test
  void send(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Provider<JdbcUtilsFactory> jdbcUtilsFactoryProvider =
        JdbcUtilsFactory_Impl.create(JdbcUtils_Factory.create(() -> jdbcTransactionManager));
    Provider<JdbcQueryRunnerFactory> jdbcQueryRunnerFactoryProvider =
        JdbcQueryRunnerFactory_Impl.create(
            JdbcQueryRunner_Factory.create(() -> jdbcTransactionManager));

    vertx
        .eventBus()
        .consumer(
            "addr",
            (Message<JsonObject> msg) -> {
              MultiMap headers = msg.headers();
              JsonObject body = msg.body();
              assertThat(body).isNotNull();
              assertThat(body.getString("address")).isEqualTo("test");
              assertThat(headers.get("key")).isEqualTo("value");
              checkpoint.flag();
            });

    PlatformTransactionManager.begin();

    Outbox outbox =
        Outbox_Factory.newInstance(
            vertx, jdbcUtilsFactoryProvider.get(), jdbcQueryRunnerFactoryProvider.get());

    outbox.afterCreated();
    MultiMap headers = HeadersMultiMap.httpHeaders().add("key", "value");
    outbox.send("addr", headers, new JsonObject().put("address", "test"));

    JdbcQueryRunnerFactory jdbcQueryRunnerFactory = jdbcQueryRunnerFactoryProvider.get();
    JdbcQueryRunner jdbcQueryRunner =
        jdbcQueryRunnerFactory.create(new StatementConfiguration.Builder().build());

    assertThat(getOutboxRows(jdbcQueryRunner))
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.id()).isPositive();
              assertThat(r.address()).isEqualTo("addr");
              assertThat(r.headers()).isEqualTo("{\"key\":[\"value\"]}");
              assertThat(r.body()).isEqualTo("{\"address\":\"test\"}");
            });

    PlatformTransactionManager.commit();

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              PlatformTransactionManager.begin();
              assertThat(getOutboxRows(jdbcQueryRunner)).isEmpty();
              PlatformTransactionManager.commit();
              checkpoint.flag();
            });
  }

  private static List<Row> getOutboxRows(JdbcQueryRunner jdbcQueryRunner) {
    return jdbcQueryRunner.query(
        "select * from outbox",
        rs -> {
          List<Row> objects = new ArrayList<>();
          while (rs.next()) {
            objects.add(
                new Row(
                    rs.getLong("id"),
                    rs.getString("address"),
                    rs.getString("headers"),
                    rs.getString("body")));
          }

          return objects;
        });
  }

  private record Row(long id, String address, String headers, String body) {}

  private HikariConfig getHikariConfig(Config.PostgresConfig postgres) {
    HikariConfig cfg = new HikariConfig();

    cfg.setUsername(postgres.username());
    cfg.setPassword(postgres.password());
    cfg.setJdbcUrl(
        "jdbc:postgresql://%s:%d/%s"
            .formatted(postgres.host(), postgres.port(), postgres.database()));
    cfg.setThreadFactory(THREAD_FACTORY);
    cfg.setConnectionTestQuery("select 1");
    cfg.setPoolName("hikari-pool");
    cfg.setMaximumPoolSize(10);
    cfg.setAutoCommit(false);
    cfg.setConnectionTimeout(Duration.ofSeconds(5L).toMillis());
    cfg.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

    // https://github.com/brettwooldridge/HikariCP#frequently-used
    var executor = new ScheduledThreadPoolExecutor(1, THREAD_FACTORY);
    executor.setRemoveOnCancelPolicy(true);
    cfg.setScheduledExecutor(executor);

    return cfg;
  }
}
