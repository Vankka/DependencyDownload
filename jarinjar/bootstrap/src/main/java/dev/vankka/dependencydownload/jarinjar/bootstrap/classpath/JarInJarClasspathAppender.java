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

package dev.vankka.dependencydownload.jarinjar.bootstrap.classpath;

import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.nio.file.Path;

/**
 * A {@link ClasspathAppender} for {@link JarInJarClassLoader}.
 */
@SuppressWarnings("unused")
public class JarInJarClasspathAppender implements ClasspathAppender {

    private final JarInJarClassLoader classLoader;

    /**
     * Creates a new instance of this classpath appender.
     * @param classLoader the {@link JarInJarClassLoader}
     */
    public JarInJarClasspathAppender(JarInJarClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void appendFileToClasspath(@NotNull Path path) throws MalformedURLException {
        classLoader.addURL(path);
    }
}
