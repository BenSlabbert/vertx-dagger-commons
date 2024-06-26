/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class FutureUtilTest {

  @Test
  void awaitTermination(VertxTestContext testContext) {
    FutureUtil.awaitTermination()
        .onComplete(
            testContext.succeeding(
                success ->
                    testContext.verify(
                        () -> {
                          assertThat(success).isTrue();
                          testContext.completeNow();
                        })));
  }

  @Test
  void virtualThreadVoidReturn(VertxTestContext testContext) {
    List<Future<Void>> tasks =
        Stream.generate(() -> FutureUtil.run(this::getTaskReturningVoid)).limit(10_000L).toList();

    assertThat(tasks).hasSizeLessThanOrEqualTo(10_000);

    Future.all(tasks).onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void virtualThreadValueReturn(VertxTestContext testContext) {
    List<Future<String>> tasks =
        Stream.generate(() -> FutureUtil.run(this::getTaskReturningValue)).limit(10_000L).toList();

    assertThat(tasks).hasSizeLessThanOrEqualTo(10_000);

    Future.all(tasks).onComplete(testContext.succeedingThenComplete());
  }

  private void getTaskReturningVoid() {
    assertThat(Thread.currentThread().isVirtual()).isTrue();
  }

  private String getTaskReturningValue() {
    assertThat(Thread.currentThread().isVirtual()).isTrue();
    return "from virtual thread: %d".formatted(Thread.currentThread().threadId());
  }
}
