/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.transaction.blocking;

import dagger.Module;

@Module(includes = {SimpleTransactionProvider.class})
public interface TransactionManagerModule {}
