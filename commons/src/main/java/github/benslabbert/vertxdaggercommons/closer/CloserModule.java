/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.closer;

import dagger.Module;

@Module
public interface CloserModule {

  ClosingService closingService();
}
