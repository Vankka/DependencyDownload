/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Vankka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.vankka.dependencydownload.logger;

import dev.vankka.dependencydownload.dependency.Dependency;

/**
 * Logger for DependencyDownload, override methods you want to log.
 */
@SuppressWarnings("unused")
public interface Logger {

    default void downloadStart() {}
    default void downloadEnd() {}

    default void downloadDependency(Dependency dependency) {}
    default void downloadSuccess(Dependency dependency) {}
    default void downloadFailed(Dependency dependency, Throwable throwable) {}

    default void relocateStart() {}
    default void relocateEnd() {}

    default void relocateDependency(Dependency dependency) {}
    default void relocateSuccess(Dependency dependency) {}
    default void relocateFailed(Dependency dependency, Throwable throwable) {}

    default void loadStart() {}
    default void loadEnd() {}

    default void loadDependency(Dependency dependency) {}
    default void loadSuccess(Dependency dependency) {}
    default void loadFailed(Dependency dependency, Throwable throwable) {}

    class NOOP implements Logger {
        public static NOOP INSTANCE = new NOOP();

        private NOOP() {}
    }
}
