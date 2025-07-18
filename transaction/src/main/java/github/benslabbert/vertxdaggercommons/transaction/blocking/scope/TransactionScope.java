/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.transaction.blocking.scope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Scope;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

@Scope
@Documented
@Retention(RUNTIME)
public @interface TransactionScope {}
