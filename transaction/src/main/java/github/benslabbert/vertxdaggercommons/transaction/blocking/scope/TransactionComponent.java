/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.transaction.blocking.scope;

import dagger.Subcomponent;

@TransactionScope
@Subcomponent(modules = {ScopeModuleBindings.class})
public interface TransactionComponent {

  TransactionManager transactionManager();

  @Subcomponent.Builder
  interface Builder {

    TransactionComponent build();
  }
}
