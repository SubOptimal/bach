import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

class UtilitiesTests {

  private final CollectingLogger logger = new CollectingLogger("*");
  private final Bach bach = new Bach(logger, Path.of("."), List.of());

  @Nested
  @DisabledIfSystemProperty(named = "bach.offline", matches = "true")
  class Download {
    @Test
    void relativeUriThrows() {
      var e =
          assertThrows(
              Exception.class, () -> bach.utilities.download(Path.of("."), URI.create("void")));
      assertTrue(e.getMessage().contains("URI is not absolute"));
    }

    @Test
    void https(@TempDir Path temp) throws Exception {
      var uri = URI.create("https://junit.org/junit5/index.html");
      var path = bach.utilities.download(temp, uri);
      var text = Files.readString(path);
      assertTrue(text.contains("<title>JUnit 5</title>"));
    }

    @Test
    void defaultFileSystem(@TempDir Path tempRoot) throws Exception {
      var content = List.of("Lorem", "ipsum", "dolor", "sit", "amet");
      var tempFile = Files.createFile(tempRoot.resolve("source.txt"));
      Files.write(tempFile, content);
      var tempPath = Files.createDirectory(tempRoot.resolve("target"));
      var name = tempFile.getFileName().toString();
      var actual = tempPath.resolve(name);

      // initial download
      bach.utilities.download(tempPath, tempFile.toUri());
      assertTrue(Files.exists(actual));
      assertLinesMatch(content, Files.readAllLines(actual));
      assertLinesMatch(
          List.of(
              "Downloading " + tempFile.toUri() + "...",
              "Transferring " + tempFile.toUri() + "...",
              "Downloaded source.txt successfully.",
              " o Size -> .. bytes", // 32 on Windows, 27 on Linux/Mac
              " o Last Modified .+"),
          logger.getLines());

      // reload
      logger.clear();
      bach.utilities.download(tempPath, tempFile.toUri());
      assertLinesMatch(
          List.of(
              "Downloading " + tempFile.toUri() + "...",
              "Local file exists. Comparing attributes to remote file...",
              "Local and remote file attributes seem to match."),
          logger.getLines());

      // offline mode
      logger.clear();
      bach.var.offline = true;
      bach.utilities.download(tempPath, tempFile.toUri());
      assertLinesMatch(
          List.of(
              "Downloading " + tempFile.toUri() + "...",
              "Offline mode is active and target already exists."),
          logger.getLines());

      // offline mode with error
      logger.clear();
      Files.delete(actual);
      var e =
          assertThrows(Exception.class, () -> bach.utilities.download(tempPath, tempFile.toUri()));
      assertEquals("Target is missing and being offline: " + actual, e.getMessage());
      assertLinesMatch(List.of("Downloading " + tempFile.toUri() + "..."), logger.getLines());
      // online but different file
      logger.clear();
      bach.var.offline = false;
      Files.write(actual, List.of("Hello world!"));
      bach.utilities.download(tempPath, tempFile.toUri());
      assertLinesMatch(content, Files.readAllLines(actual));
      assertLinesMatch(
          List.of(
              "Downloading " + tempFile.toUri() + "...",
              "Local file exists. Comparing attributes to remote file...",
              "Local file differs from remote -- replacing it...",
              "Transferring " + tempFile.toUri() + "...",
              "Downloaded source.txt successfully.",
              " o Size -> .. bytes", // 32 on Windows, 27 on Linux/Mac
              " o Last Modified .+"),
          logger.getLines());
    }
  }

  @Nested
  class Trees {

    @Test
    void tree() throws Exception {
      Path root = Files.createTempDirectory("tree-root-");
      assertTrue(Files.exists(root));
      assertEquals(1, Files.walk(root).count());
      assertTreeWalkMatches(root, root.toString(), ".");

      Util.createFiles(root, 3);
      assertEquals(1 + 3, Files.walk(root).count());
      assertTreeWalkMatches(root, root.toString(), ".", "./file-0", "./file-1", "./file-2");

      Util.createFiles(Files.createDirectory(root.resolve("a")), 3);
      Util.createFiles(Files.createDirectory(root.resolve("b")), 3);
      Util.createFiles(Files.createDirectory(root.resolve("x")), 4);
      assertTrue(Files.exists(root));
      assertTreeWalkMatches(
          root,
          root.toString(),
          ".",
          "./a",
          "./a/file-0",
          "./a/file-1",
          "./a/file-2",
          "./b",
          "./b/file-0",
          "./b/file-1",
          "./b/file-2",
          "./file-0",
          "./file-1",
          "./file-2",
          "./x",
          "./x/file-0",
          "./x/file-1",
          "./x/file-2",
          "./x/file-3");

      bach.utilities.treeDelete(root, path -> path.startsWith(root.resolve("b")));
      assertTreeWalkMatches(
          root,
          root.toString(),
          ".",
          "./a",
          "./a/file-0",
          "./a/file-1",
          "./a/file-2",
          "./file-0",
          "./file-1",
          "./file-2",
          "./x",
          "./x/file-0",
          "./x/file-1",
          "./x/file-2",
          "./x/file-3");

      bach.utilities.treeDelete(root, path -> path.endsWith("file-0"));
      assertTreeWalkMatches(
          root,
          root.toString(),
          ".",
          "./a",
          "./a/file-1",
          "./a/file-2",
          "./file-1",
          "./file-2",
          "./x",
          "./x/file-1",
          "./x/file-2",
          "./x/file-3");

      bach.utilities.treeCopy(root.resolve("x"), root.resolve("a/b/c"));
      assertTreeWalkMatches(
          root,
          root.toString(),
          ".",
          "./a",
          "./a/b",
          "./a/b/c",
          "./a/b/c/file-1",
          "./a/b/c/file-2",
          "./a/b/c/file-3",
          "./a/file-1",
          "./a/file-2",
          "./file-1",
          "./file-2",
          "./x",
          "./x/file-1",
          "./x/file-2",
          "./x/file-3");

      bach.utilities.treeCopy(root.resolve("x"), root.resolve("x/y"));
      assertTreeWalkMatches(
          root,
          root.toString(),
          ".",
          "./a",
          "./a/b",
          "./a/b/c",
          "./a/b/c/file-1",
          "./a/b/c/file-2",
          "./a/b/c/file-3",
          "./a/file-1",
          "./a/file-2",
          "./file-1",
          "./file-2",
          "./x",
          "./x/file-1",
          "./x/file-2",
          "./x/file-3",
          "./x/y",
          "./x/y/file-1",
          "./x/y/file-2",
          "./x/y/file-3");

      bach.utilities.treeDelete(root);
      assertTrue(Files.notExists(root));
    }

    @Test
    void copyNonExistingDirectoryFails() {
      var root = Path.of("does not exist");
      assertThrows(
          IllegalArgumentException.class, () -> bach.utilities.treeCopy(root, Path.of(".")));
    }

    @Test
    void copyAndItsPreconditions(@TempDir Path temp) throws Exception {
      var regular = Util.createFiles(temp, 2).get(0);
      assertThrows(Throwable.class, () -> bach.utilities.treeCopy(regular, Path.of(".")));
      var directory = Files.createDirectory(temp.resolve("directory"));
      Util.createFiles(directory, 3);
      assertThrows(Throwable.class, () -> bach.utilities.treeCopy(directory, regular));
      bach.utilities.treeCopy(directory, directory);
      assertThrows(Throwable.class, () -> bach.utilities.treeCopy(temp, directory));
      var forbidden = Files.createDirectory(temp.resolve("forbidden"));
      try {
        Util.chmod(forbidden, false, false, true);
        assertThrows(Throwable.class, () -> bach.utilities.treeCopy(directory, forbidden));
      } finally {
        Util.chmod(forbidden, true, true, true);
      }
    }

    @Test
    void deleteEmptyDirectory() throws Exception {
      var empty = Files.createTempDirectory("deleteEmptyDirectory");
      assertTrue(Files.exists(empty));
      bach.utilities.treeDelete(empty);
      assertTrue(Files.notExists(empty));
    }

    @Test
    void deleteNonExistingPath() {
      var root = Path.of("does not exist");
      assertDoesNotThrow(() -> bach.utilities.treeDelete(root));
    }

    @Test
    void walkFailsForNonExistingPath() {
      var root = Path.of("does not exist");
      assertThrows(Error.class, () -> bach.utilities.treeWalk(root, System.out::println));
    }

    private void assertTreeWalkMatches(Path root, String... expected) {
      var actualLines = new ArrayList<String>();
      bach.utilities.treeWalk(root, actualLines::add);
      assertLinesMatch(List.of(expected), actualLines);
    }
  }
}
