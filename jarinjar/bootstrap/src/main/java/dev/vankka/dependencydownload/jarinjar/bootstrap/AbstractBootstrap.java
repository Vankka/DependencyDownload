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

package dev.vankka.dependencydownload.jarinjar.bootstrap;

import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;

/**
 * A bootstrap that is loaded in by a {@code ILoader} from the loader module.
 */
@SuppressWarnings("unused") // API
public abstract class AbstractBootstrap {

    private final JarInJarClassLoader classLoader;

    /**
     * The constructor.
     * @param classLoader the class that loaded in this bootstrap
     */
    public AbstractBootstrap(JarInJarClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns the {@link JarInJarClassLoader} that loaded in this bootstrap.
     * @return the {@link JarInJarClassLoader}
     */
    @SuppressWarnings("unused") // API
    public JarInJarClassLoader getClassLoader() {
        return classLoader;
    }
}
