/* Licensed under Apache-2.0 2025. */
package github.benslabbert.vertxdaggercommons.entitygenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EntityGeneratorTest {

  @Test
  void test() throws Exception {
    Path tempDirectory = Files.createTempDirectory("");
    EntityGenerator.main(tempDirectory.toAbsolutePath().toString(), "com.example");
  }
}
