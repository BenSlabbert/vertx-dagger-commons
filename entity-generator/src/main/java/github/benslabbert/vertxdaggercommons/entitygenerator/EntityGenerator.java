/* Licensed under Apache-2.0 2023. */
package github.benslabbert.vertxdaggercommons.entitygenerator;

import github.benslabbert.vertxdaggercommons.dbmigration.FlywayProvider;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Logging;
import org.jooq.meta.jaxb.OnError;
import org.jooq.meta.jaxb.Strategy;
import org.jooq.meta.jaxb.Target;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class EntityGenerator {

  public static void main(String... args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("expecting 2 args: outputFolder and packageName");
    }

    var outputFolder = args[0];
    var packageName = args[1];
    System.out.println("generating jOOQ into dir " + outputFolder + " with package " + packageName);
    generate(outputFolder, packageName);
  }

  private static void generate(String folderPath, String packageName) throws Exception {
    var imageName =
        DockerImageName.parse("postgres:17-alpine")
            .asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE);

    try (var container = new PostgreSQLContainer<>(imageName)) {
      container.start();

      var flyway =
          FlywayProvider.get("127.0.0.1", container.getMappedPort(5432), "test", "test", "test");
      flyway.clean();
      flyway.migrate();

      var database =
          new Database()
              .withName("org.jooq.meta.postgres.PostgresDatabase")
              .withIncludes(".*")
              .withExcludes(String.join("|", flyway.getConfiguration().getTable()))
              .withInputSchema("public")
              .withOutputSchemaToDefault(true)
              .withRecordVersionFields("version");

      var generate =
          new Generate()
              .withComments(true)
              .withJavaTimeTypes(true)
              .withFluentSetters(true)
              .withJavaBeansGettersAndSetters(false)
              .withGeneratedAnnotation(true)
              .withGeneratedAnnotationDate(true)
              .withGeneratedAnnotationJooqVersion(true)
              .withNullableAnnotation(true)
              .withNullableAnnotationType("jakarta.annotation.Nullable")
              .withNonnullAnnotation(true)
              .withNonnullAnnotationType("jakarta.annotation.Nonnull")
              .withPojos(true)
              .withImmutablePojos(true)
              .withPojosEqualsAndHashCode(true)
              .withPojosAsJavaRecordClasses(true)
              .withPojosToString(true)
              .withDaos(true)
              .withGlobalObjectNames(true)
              .withIndentation("  ")
              .withUdts(true)
              .withUdtPaths(true)
              .withGlobalUDTReferences(true)
              .withCommentsOnUDTs(true);

      var generator =
          new Generator()
              .withDatabase(database)
              .withTarget(new Target().withPackageName(packageName).withDirectory(folderPath))
              .withGenerate(generate)
              .withStrategy(new Strategy());

      var jdbc =
          new Jdbc()
              .withDriver("org.postgresql.Driver")
              .withUrl("jdbc:postgresql://127.0.0.1:" + container.getMappedPort(5432) + "/test")
              .withUser("test")
              .withPassword("test");

      var configuration =
          new Configuration()
              .withJdbc(jdbc)
              .withGenerator(generator)
              .withOnError(OnError.FAIL)
              .withLogging(Logging.DEBUG);

      GenerationTool.generate(configuration);
    }
  }
}
