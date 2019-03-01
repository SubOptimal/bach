/*
 * Java Shell Builder
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

import java.net.URI;
import java.nio.file.Paths;
import java.util.logging.Level;

public class Demo {

  static void basic() throws Exception {
    Bach.builder()
        .name("basic")
        .log(Level.FINE)
        .override(Folder.SOURCE, Paths.get("demo/basic"))
        .override(Folder.TARGET, Paths.get("target/bach/demo/basic"))
        .peek(builder -> System.out.println(builder.name))
        .build()
        .execute("java", "--version")
        .format()
        .compile()
        .run("com.greetings", "com.greetings.Main");
  }

  static void common() throws Exception {
    Bach.builder()
        .name("common")
        .override(Folder.SOURCE, Paths.get("demo/common"))
        .override(Folder.TARGET, Paths.get("target/bach/demo/common"))
        .peek(builder -> System.out.println(builder.name))
        .build()
        .format()
        .compile()
        .runCompiled("com.greetings");
  }

  static void idea() throws Exception {
    Bach.Visitor jdeps =
        bach ->
            bach.execute(
                "jdeps",
                "-profile",
                // "--dot-output", bach.path(Folder.TARGET),
                "--module-path",
                bach.path(Folder.TARGET_MAIN_JAR),
                "--module",
                "com.greetings");
    Bach.builder()
        .name("idea")
        .log(Level.FINE)
        .version("0.9")
        .main("com.greetings", "com.greetings.Main")
        .override(Folder.SOURCE, Paths.get("demo/idea"))
        .override(Folder.TARGET, Paths.get("target/bach/demo/idea"))
        .peek(builder -> System.out.println(builder.name))
        .build()
        .format()
        .load(
            "org.junit.jupiter.api",
            URI.create(
                "http://central.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.0.0-M4/junit-jupiter-api-5.0.0-M4.jar"))
        .load(
            "org.junit.platform.commons",
            URI.create(
                "http://central.maven.org/maven2/org/junit/platform/junit-platform-commons/1.0.0-M4/junit-platform-commons-1.0.0-M4.jar"))
        .compile()
        .test()
        .jar()
        .visit(jdeps)
        .runJar("com.greetings")
        .link("com.greetings", "greetings");
  }

  public static void main(String... args) throws Exception {
    basic();
    common();
    idea();
  }
}
