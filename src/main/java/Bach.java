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

// default package

/** JShell Builder. */
@SuppressWarnings({
        "WeakerAccess",
        "RedundantIfStatement",
        "UnusedReturnValue",
        "SameParameterValue",
        "SimplifiableIfStatement"
})
public interface Bach {

    default void build() {
        clean();
        compile();
        test();
    }

    static Builder builder() {
        return new Builder();
    }

    default void clean() {
    }

    default void compile() {
    }

    default void test() {
    }

    class Builder {
        public Bach build() {
            return new Impl(this);
        }
    }

    class Impl implements Bach {
        public Impl(Builder builder) {
        }
    }
}
