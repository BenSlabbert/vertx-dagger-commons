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
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcTransactionManager;
import github.benslabbert.vertxdaggercommons.transaction.blocking.jdbc.JdbcTransactionManager_Factory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

@ExtendWith(VertxExtension.class)
class InboxIT {

  private static final Logger log = LoggerFactory.getLogger(InboxIT.class);

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

  private HikariDataSource dataSource;
  private Provider provider;

  @BeforeEach
  void before(Vertx vertx) {
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
          "create table inbox(id serial8 primary key, address text, headers text, body text)");
      conn.commit();
    } catch (Exception e) {
      fail("should create table successfully", e);
    }

    JdbcTransactionManager jdbcTransactionManager =
        JdbcTransactionManager_Factory.newInstance(dataSource);
    PlatformTransactionManager.setTransactionManager(jdbcTransactionManager);

    provider =
        DaggerProvider.builder()
            .vertx(vertx)
            .jdbcTransactionManager(jdbcTransactionManager)
            .build();
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

    try (var conn = dataSource.getConnection()) {
      Statement statement = conn.createStatement();
      statement.execute("insert into inbox(address, headers, body) values ('address', '{}', '{}')");
      conn.commit();
    } catch (Exception e) {
      fail("should insert into table successfully", e);
    }

    PlatformTransactionManager.begin();
    List<Row> inboxRows = getInboxRows(provider.jdbcQueryRunnerFactory().create());
    assertThat(inboxRows).hasSize(1);
    PlatformTransactionManager.commit();

    provider.myInbox();
    PlatformTransactionManager.begin();
    inboxRows = getInboxRows(provider.jdbcQueryRunnerFactory().create());
    PlatformTransactionManager.commit();
    assertThat(inboxRows).isEmpty();

    vertx.eventBus().publish("address", new JsonObject());

    await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              PlatformTransactionManager.begin();
              var rows = getInboxRows(provider.jdbcQueryRunnerFactory().create());
              log.info("before delay {}", rows);
              assertThat(rows).hasSize(1);
              PlatformTransactionManager.commit();
              checkpoint.flag();
            });

    await()
        .pollDelay(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(4))
        .untilAsserted(
            () -> {
              PlatformTransactionManager.begin();
              var rows = getInboxRows(provider.jdbcQueryRunnerFactory().create());
              log.info("after delay {}", rows);
              assertThat(rows).isEmpty();
              PlatformTransactionManager.commit();
              checkpoint.flag();
            });
  }

  private static List<Row> getInboxRows(JdbcQueryRunner jdbcQueryRunner) {
    return jdbcQueryRunner.query(
        "select * from inbox",
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
