/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.auth;

import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;

/**
 * this AuthenticationProvider does no credential checks and always returns an authenticated user
 * with no permissions
 */
public final class NoAuthRequiredAuthenticationProvider implements AuthenticationProvider {

  private NoAuthRequiredAuthenticationProvider() {}

  public static NoAuthRequiredAuthenticationProvider create() {
    return new NoAuthRequiredAuthenticationProvider();
  }

  @Override
  public Future<User> authenticate(Credentials credentials) {
    return Future.succeededFuture(User.fromName("no-auth-user"));
  }
}
