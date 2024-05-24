/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.transaction.blocking.scope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import javax.inject.Scope;

@Scope
@Documented
@Retention(RUNTIME)
public @interface TransactionScope {}
