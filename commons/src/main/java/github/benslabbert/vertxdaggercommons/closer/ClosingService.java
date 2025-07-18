/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.closer;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;

@Singleton
public class ClosingService {

  private final Set<AutoCloseable> closeables;

  @Inject
  ClosingService(Set<AutoCloseable> closeables) {
    this.closeables = closeables;
  }

  public Set<AutoCloseable> closeables() {
    return closeables;
  }
}
