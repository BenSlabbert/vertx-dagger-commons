/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.transaction.blocking;

import java.util.function.Function;
import org.jooq.Configuration;
import org.jooq.DSLContext;

public abstract class TransactionBoundary {

  private final DSLContext dslContext;

  protected TransactionBoundary(DSLContext dslContext) {
    this.dslContext = dslContext;
  }

  public <T> T doInTransaction(Function<Configuration, T> function) {
    return dslContext.transactionResult(function::apply);
  }
}
