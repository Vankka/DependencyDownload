/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Vankka
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

package dev.vankka.dependencydownload.classloader;

import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Utility {@link ClassLoader} to load classes onto a separate classpath as the main application.
 * Extends {@link ClasspathAppender} for use with {@link dev.vankka.dependencydownload.DependencyManager}.
 */
@SuppressWarnings("unused") // API
public class IsolatedClassLoader extends URLClassLoader implements ClasspathAppender {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatedClassLoader() {
        super(new URL[0], ClassLoader.getSystemClassLoader().getParent());
    }

    @Override
    public void appendFileToClasspath(@NotNull Path path) throws MalformedURLException {
        addURL(path.toUri().toURL());
    }
}
