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

package dev.vankka.dependencydownload.jarinjar.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Loads a jar from a resource into a temporary file to avoid illegal reflection.
 * You can find the {@code JarInJarClasspathAppender} in the bootstrap module.
 */
public class JarInJarClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public JarInJarClassLoader(String tempFilePrefix, URL resourceURL, ClassLoader parent) throws IOException {
        super(new URL[] {asTempFileURL(tempFilePrefix, resourceURL)}, parent);
    }

    private static URL asTempFileURL(String filePrefix, URL resourceURL) throws IOException {
        if (filePrefix == null) {
            throw new NullPointerException("filePrefix");
        } else if (resourceURL == null) {
            throw new NullPointerException("resourceURL");
        }

        Path tempFile = Files.createTempFile(filePrefix, ".jar.tmp");
        try (InputStream inputStream = resourceURL.openStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        tempFile.toFile().deleteOnExit();
        return tempFile.toUri().toURL();
    }

    @Override
    public void close() throws IOException {
        super.close();

        URL url = getURLs()[0];
        if (url != null) {
            Path path;
            try {
                path = Paths.get(url.toURI());
            } catch (URISyntaxException ignored) {
                return;
            }
            Files.deleteIfExists(path);
        }
    }

    public void addURL(Path path) throws MalformedURLException {
        addURL(path.toUri().toURL());
    }

}
