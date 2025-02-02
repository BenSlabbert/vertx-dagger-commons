/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.saga;

import dagger.Module;

@Module
public interface SagaModule {

  SagaBuilder sagaBuilder();
}
