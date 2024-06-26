/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons;

import java.io.IOException;
import java.net.ServerSocket;

public final class FreePortUtility {

  private FreePortUtility() {}

  /**
   * @deprecated use 0 to get a random free port
   */
  @Deprecated(forRemoval = true, since = "1.19.0")
  public static int getPort() {
    for (int i = 0; i < 1_000; i++) {
      try (var serverSocket = new ServerSocket(0)) {
        return serverSocket.getLocalPort();
      } catch (IOException e) {
        // do nothing
      }
    }
    throw new IllegalArgumentException("unable to find port");
  }
}
