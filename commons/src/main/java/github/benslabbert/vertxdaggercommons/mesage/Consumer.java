/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.mesage;

import io.vertx.core.Future;

public interface Consumer {

  void register();

  Future<Void> unregister();
}
