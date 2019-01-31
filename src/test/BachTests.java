/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

@ExtendWith(BachContext.class)
class BachTests {

  @Test
  void mainWithEmptyStringArray() {
    var level = "bach.log.level";
    try {
      System.setProperty(level, "OFF");
      assertDoesNotThrow((Executable) Bach::main);
    } finally {
      System.clearProperty(level);
    }
  }

  @Test
  void mainFailsWithCustomErrorCode() {
    var level = "bach.log.level";
    var error = "bach.fail.code";
    try {
      System.setProperty(level, "OFF");
      System.setProperty(error, "123");
      Error e = assertThrows(Error.class, () -> Bach.main("fail"));
      assertEquals("Bach finished with exit code 123", e.getMessage());
    } finally {
      System.clearProperty(level);
      System.clearProperty(error);
    }
  }

  @Test
  void mainWithEmptyListOfString(Bach bach) {
    assertEquals(0, bach.main(List.of()));
  }

  @Test
  void mainWithHelpReturnsZero(Bach bach) {
    assertEquals(0, bach.main(List.of("help")));
  }

  @Test
  void mainFailsWithDefaultCode(Bach bach) {
    var expected = Integer.valueOf(Property.FAIL_CODE.defaultValue);
    assertEquals(expected, bach.main(List.of("help", "fail")));
  }

  @Test
  void runExecutable(BachContext context) {
    assertThrows(Error.class, () -> context.bach.run("executable", "a", "b", "3"));
    assertLinesMatch(
        List.of(
            "[run] executable [a, b, 3]",
            "running executable with 3 argument(s)",
            "executable\n" + "  a\n" + "  b\n" + "  3"),
        context.recorder.all);
  }

  @Test
  void runStreamSequentially(BachContext context) {
    var tasks = context.tasks(3);
    var result = context.bach.run("run stream sequentially", tasks);
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run stream sequentially...",
            "1 begin",
            "1 done. .+",
            "2 begin",
            "2 done. .+",
            "3 begin",
            "3 done. .+",
            "[run] run stream sequentially done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  @DisabledIfSystemProperty(named = "junit.jupiter.execution.parallel.enabled", matches = "true")
  void runStreamParallel(BachContext context) {
    var tasks = context.tasks(3).parallel();
    var result = context.bach.run("run stream in parallel", tasks);
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run stream in parallel...",
            ". begin",
            ". begin",
            ". begin",
            ". done. .+",
            ". done. .+",
            ". done. .+",
            "[run] run stream in parallel done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  @DisabledIfSystemProperty(named = "junit.jupiter.execution.parallel.enabled", matches = "true")
  void runVarArgs(BachContext context) {
    var result = context.bach.run("run varargs", () -> context.task("A"), () -> context.task("B"));
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run varargs...",
            ". begin",
            ". begin",
            ". done. .+",
            ". done. .+",
            "[run] run varargs done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runCommand(BachContext context) {
    var bach = context.bach;
    var command = new Command("java").add("--version");
    var result = bach.run("java --version", command);
    assertEquals(0, result);
    assertTrue(context.bytesOut.toString().contains(Runtime.version().toString()));
    var expected =
        List.of(
            "[run] java --version...",
            "running java with 1 argument(s)",
            "java\n--version",
            "replaced executable `java` with program `" + Util.getJdkCommand("java") + "`",
            "[run] java --version done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runFunction(BachContext context) {
    var bach = context.bach;
    Function<Bach, Integer> function = new Command("java").add("--version");
    var result = bach.run(function);
    assertEquals(0, result);
    assertTrue(context.bytesOut.toString().contains(Runtime.version().toString()));
    var expected =
        List.of(
            "[run] " + function.getClass().getSimpleName() + "...",
            "running java with 1 argument(s)",
            "java\n--version",
            "replaced executable `java` with program `" + Util.getJdkCommand("java") + "`",
            "[run] " + function.getClass().getSimpleName() + " done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runFunctionWithCaption(BachContext context) {
    var bach = context.bach;
    Function<Bach, Integer> function = new Command("java").add("--version");
    var result = bach.run("Java Version Function", function);
    assertEquals(0, result);
    assertTrue(context.bytesOut.toString().contains(Runtime.version().toString()));
    var expected =
        List.of(
            "[run] Java Version Function...",
            "running java with 1 argument(s)",
            "java\n--version",
            "replaced executable `java` with program `" + Util.getJdkCommand("java") + "`",
            "[run] Java Version Function done.");
    assertLinesMatch(expected, context.recorder.all);
  }
}
