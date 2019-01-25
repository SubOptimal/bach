/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class JdkToolTests {

  private final Bach bach;

  JdkToolTests(Bach bach) {
    this.bach = bach;
  }

  @Nested
  class Javac {
    @Test
    void defaults() {
      var expectedLines =
          List.of("javac", "-deprecation", "-encoding", "  UTF-8", "-Werror", "-parameters");
      assertLinesMatch(expectedLines, dump(new JdkTool.Javac().toCommand(bach)));
    }

    @Test
    void basic() {
      var expectedLines = List.of("javac", "--module", "  foo");
      var javac = new JdkTool.Javac();
      javac.deprecation = false;
      javac.encoding = null;
      javac.failOnWarnings = false;
      javac.parameters = false;
      javac.module = "foo";
      assertLinesMatch(expectedLines, dump(javac.toCommand(bach)));
    }

    @Test
    void customized() {
      var expectedLines =
          List.of(
              "javac",
              "--class-path",
              "  classes",
              "-g",
              "-deprecation",
              "-d",
              "  out",
              "-encoding",
              "  US-ASCII",
              "-Werror",
              "--patch-module",
              "  foo=bar",
              "--module-path",
              "  mods",
              "--module-source-path",
              "  src",
              "--add-modules",
              "  mod.A,ALL-MODULE-PATH,mod.B",
              "-parameters",
              "-verbose",
              ">> many .java files >>");
      var javac = new JdkTool.Javac();
      javac.generateAllDebuggingInformation = true;
      javac.deprecation = true;
      javac.destination = Paths.get("out");
      javac.encoding = StandardCharsets.US_ASCII;
      javac.failOnWarnings = true;
      javac.parameters = true;
      javac.verbose = true;
      javac.classPath = List.of(Paths.get("classes"));
      javac.classSourcePath = List.of(Paths.get("src/build"));
      javac.moduleSourcePath = List.of(Paths.get("src"));
      javac.modulePath = List.of(Paths.get("mods"));
      javac.addModules = List.of("mod.A", "ALL-MODULE-PATH", "mod.B");
      javac.patchModule = Map.of("foo", List.of(Paths.get("bar")));
      assertLinesMatch(expectedLines, dump(javac.toCommand(bach)));
    }
  }

  @Test
  void java() {
    var expectedLines =
        List.of(
            "java",
            "--class-path",
            "  a.jar",
            "--dry-run",
            "-jar",
            "  application.jar",
            "--patch-module",
            "  com.greetings=xxx",
            "--module-path",
            "  mods",
            "--add-modules",
            "  mod.A,ALL-MODULE-PATH,mod.B",
            "--module",
            "  com.greetings/com.greetings.Main",
            "1",
            "2",
            "NEW");
    var java = new JdkTool.Java();
    java.classPath = List.of(Paths.get("a.jar"));
    java.dryRun = true;
    java.jar = Paths.get("application.jar");
    java.addModules = List.of("mod.A", "ALL-MODULE-PATH", "mod.B");
    java.patchModule = Map.of("com.greetings", List.of(Paths.get("xxx")));
    java.modulePath = List.of(Paths.get("mods"));
    java.module = "com.greetings/com.greetings.Main";
    java.args = List.of(1, "2", Thread.State.NEW);
    assertLinesMatch(expectedLines, dump(java.toCommand(bach)));
  }

  @Nested
  class Javadoc {

    @Test
    void basic() {
      var expectedLines = List.of("javadoc");
      var javadoc = new JdkTool.Javadoc();
      javadoc.quiet = false;
      javadoc.html5 = false;
      javadoc.keywords = false;
      javadoc.doclint = null;
      assertLinesMatch(expectedLines, dump(javadoc.toCommand(bach)));
    }

    @Test
    void defaults() {
      var expectedLines = List.of("javadoc", "-quiet", "-html5", "-keywords", "-Xdoclint");
      assertLinesMatch(expectedLines, dump(new JdkTool.Javadoc().toCommand(bach)));
    }

    @Test
    void customized() {
      var expectedLines =
          List.of(
              "javadoc",
              "-quiet",
              "-html5",
              "-keywords",
              "-link",
              "  one",
              "-link",
              "  two",
              "-linksource",
              "-Xdoclint:all,-missing",
              "--show-members",
              "  private",
              "--show-types",
              "  public");
      var javadoc = new JdkTool.Javadoc();
      javadoc.quiet = true;
      javadoc.html5 = true;
      javadoc.link = List.of("one", "two");
      javadoc.linksource = true;
      javadoc.keywords = true;
      javadoc.doclint = "all,-missing";
      javadoc.showTypes = JdkTool.Javadoc.Visibility.PUBLIC;
      javadoc.showMembers = JdkTool.Javadoc.Visibility.PRIVATE;
      assertLinesMatch(expectedLines, dump(javadoc.toCommand(bach)));
    }

    @Test
    void suppressUnusedWarnings() {
      var command = bach.command("suppressor");
      var javadoc = new JdkTool.Javadoc();
      javadoc.doclint(command);
      javadoc.showMembers(command);
      javadoc.showTypes(command);
    }
  }

  @Nested
  class Jar {
    @Test
    void defaults() {
      var expectedLines = List.of("jar", "--create", "--file", "  out.jar");
      assertLinesMatch(expectedLines, dump(new JdkTool.Jar().toCommand(bach)));
    }

    @Test
    void customized() {
      var expectedLines =
          List.of(
              "jar",
              "--list",
              "--file",
              "  fleet.jar",
              "--main-class",
              "  uss.Enterprise",
              "--module-version",
              "  1701",
              "--no-compress",
              "--verbose",
              "-C",
              "  classes",
              ".");
      var jar = new JdkTool.Jar();
      jar.mode = "--list";
      jar.file = Paths.get("fleet.jar");
      jar.mainClass = "uss.Enterprise";
      jar.moduleVersion = "1701";
      jar.noCompress = true;
      jar.verbose = true;
      jar.path = Paths.get("classes");
      assertLinesMatch(expectedLines, dump(jar.toCommand(bach)));
    }
  }

  @Test
  void jdeps() {
    var jdeps = new JdkTool.Jdeps();
    jdeps.classpath = List.of(Paths.get("classes"));
    jdeps.jdkInternals = true;
    jdeps.recursive = true;
    jdeps.profile = true;
    jdeps.apionly = true;
    jdeps.summary = true;
    jdeps.verbose = true;
    assertLinesMatch(
        List.of(
            "jdeps",
            "-classpath",
            "  classes",
            "-recursive",
            "--jdk-internals",
            "-profile",
            "-apionly",
            "-summary",
            "-verbose"),
        dump(jdeps.toCommand(bach)));
  }

  @Test
  void jlink() {
    var jlink = new JdkTool.Jlink();
    jlink.modulePath = List.of(Paths.get("mods"));
    jlink.output = Paths.get("target", "image");
    assertLinesMatch(
        List.of(
            "jlink", "--module-path", "  mods", "--output", "  target" + File.separator + "image"),
        dump(jlink.toCommand(bach)));
  }

  @TestFactory
  Stream<DynamicTest> checkFoundationJdkCommands() {
    return Arrays.stream(JdkTool.class.getDeclaredClasses())
        .map(type -> type.getSimpleName().toLowerCase())
        .map(name -> dynamicTest(name, () -> bach.util.getJdkCommand(name)));
  }

  @TestFactory
  Stream<DynamicTest> checkFoundationJdkTools() {
    return Arrays.stream(JdkTool.class.getDeclaredClasses())
        .map(type -> type.getSimpleName().toLowerCase())
        .filter(name -> !name.equals("java"))
        .map(name -> dynamicTest(name, () -> ToolProvider.findFirst(name).orElseThrow()));
  }

  private List<String> dump(Bach.Command command) {
    var lines = new ArrayList<String>();
    assertSame(command, command.dump(lines::add));
    return lines;
  }
}
