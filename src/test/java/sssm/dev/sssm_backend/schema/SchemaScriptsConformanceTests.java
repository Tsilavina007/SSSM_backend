package sssm.dev.sssm_backend.schema;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.projectRoot;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.read;
import static sssm.dev.sssm_backend.schema.SchemaTestSupport.sqlFiles;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Vérifie que les scripts SQL de {@code docs/database} restent cohérents entre eux et que le
 * schéma livré correspond à ce que l'application attend.
 *
 * <p>Ces tests lisent les fichiers du dépôt : ils ne nécessitent pas de base de données et
 * s'exécutent donc très rapidement.
 */
class SchemaScriptsConformanceTests {

  private static final Pattern INCLUDED_FILE = Pattern.compile("\\\\i\\s+([^\\s]+)");

  @Test
  @DisplayName("Le script principal inclut tous les scripts de schéma")
  void schemaRunnerIncludesEveryMigrationFile() {
    Path schemaDirectory = projectRoot().resolve("docs/database/schema");
    Path runner = projectRoot().resolve("docs/database/sssm_schema2_v01.sql");

    Set<String> included = new LinkedHashSet<>();
    Matcher matcher = INCLUDED_FILE.matcher(read(runner));
    while (matcher.find()) {
      included.add(matcher.group(1).replace('\\', '/'));
    }

    List<String> missing = new java.util.ArrayList<>();
    for (Path file : sqlFiles(schemaDirectory)) {
      String relative =
          projectRoot().relativize(file).toString().replace('\\', '/');
      if (!included.contains(relative)) {
        missing.add(relative);
      }
    }
    assertTrue(missing.isEmpty(), () -> "Scripts non inclus dans sssm_schema2_v01.sql : " + missing);
  }

  @Test
  @DisplayName("Les scripts de schéma n'effectuent aucune opération destructive")
  void migrationScriptsAreAdditiveOnly() {
    Pattern destructive =
        Pattern.compile(
            "\\b(DROP\\s+TABLE|DROP\\s+COLUMN|DROP\\s+CONSTRAINT|TRUNCATE|DELETE\\s+FROM)\\b",
            Pattern.CASE_INSENSITIVE);
    for (Path file : sqlFiles(projectRoot().resolve("docs/database/schema"))) {
      String content = read(file);
      Matcher matcher = destructive.matcher(content);
      assertFalse(
          matcher.find(),
          () -> "Opération destructive détectée dans " + file.getFileName() + " : " + content);
    }
  }

  @Test
  @DisplayName("Les scripts de migration sont ordonnés et sans doublon")
  void migrationFilesAreOrderedAndUnique() {
    List<Path> files = sqlFiles(projectRoot().resolve("docs/database/schema"));
    Set<String> names = new LinkedHashSet<>();
    int previous = -1;
    for (Path file : files) {
      String name = file.getFileName().toString();
      assertTrue(names.add(name), () -> "Script dupliqué : " + name);
      int prefix = Integer.parseInt(name.substring(0, 2));
      assertTrue(prefix > previous, () -> "Script hors séquence : " + name);
      previous = prefix;
    }
  }

  @Test
  @DisplayName("Les données de démonstration ciblent des tables créées par le schéma")
  void mockDataTargetsExistingTables() {
    Set<String> created = createdTables();
    Set<String> targeted = new LinkedHashSet<>();
    Pattern target = Pattern.compile("INSERT\\s+INTO\\s+([a-z_][a-z0-9_]*)", Pattern.CASE_INSENSITIVE);
    for (Path file : sqlFiles(projectRoot().resolve("docs/database/mock_data"))) {
      Matcher matcher = target.matcher(read(file));
      while (matcher.find()) {
        targeted.add(matcher.group(1).toLowerCase(Locale.ROOT));
      }
    }
    assertFalse(targeted.isEmpty(), "aucune table de démonstration détectée");

    List<String> unknown = targeted.stream().filter(name -> !created.contains(name)).sorted().toList();
    assertTrue(
        unknown.isEmpty(), () -> "Tables de démonstration absentes du schéma : " + unknown);
  }

  /** Tables créées par les scripts de schéma. */
  private static Set<String> createdTables() {
    return SchemaTestSupport.createdTables(projectRoot().resolve("docs/database/schema"));
  }

  @Test
  @DisplayName("Les migrations du Lot 4 sont additives et idempotentes")
  void lot4MigrationIsAdditiveAndIdempotent() {
    Path migration = projectRoot().resolve("docs/database/schema/20_lot4_assignments.sql");
    String content = read(migration);

    assertTrue(content.contains("ADD COLUMN IF NOT EXISTS source_warehouse_id"));
    assertTrue(content.contains("ADD COLUMN IF NOT EXISTS voided_at"));
    assertTrue(content.contains("ADD COLUMN IF NOT EXISTS voided_by"));
    assertTrue(content.contains("ADD COLUMN IF NOT EXISTS void_reason"));
    assertTrue(content.contains("ADD COLUMN IF NOT EXISTS archived_by"));
    assertTrue(content.contains("CREATE INDEX IF NOT EXISTS"));
    assertTrue(content.contains("IF NOT EXISTS (\n        SELECT 1 FROM pg_constraint"));
    assertTrue(content.contains("BEGIN;"), "la migration doit être transactionnelle");
    assertTrue(content.contains("COMMIT;"), "la migration doit être validée explicitement");
  }
}
