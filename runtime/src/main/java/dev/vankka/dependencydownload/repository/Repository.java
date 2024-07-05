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

package dev.vankka.dependencydownload.repository;

import dev.vankka.dependencydownload.dependency.Dependency;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;

public interface Repository {

    /**
     * The maven repository host's address.
     * @return the host's address, without the path to the actual dependency and slash before the path
     */
    String getHost();

    /**
     * Creates an url for the given {@link Dependency} with the {@link #getHost()}.
     *
     * @param dependency the dependency to generate the url for
     * @return the url
     * @throws MalformedURLException if the url syntax is invalid
     */
    default URL createURL(Dependency dependency) throws MalformedURLException {
        return new URL(getHost() + '/' + dependency.getMavenPath());
    }

    /**
     * Opens a connection from an url generated with {@link #createURL(Dependency)}.
     *
     * @param dependency the dependency used to generate the url
     * @return the opened {@link HttpsURLConnection}
     * @throws IOException if opening connection fails
     */
    default URLConnection openConnection(Dependency dependency) throws IOException {
        URLConnection connection = createURL(dependency).openConnection();

        if (connection instanceof HttpURLConnection) {
            if (!(connection instanceof HttpsURLConnection)) {
                throw new RuntimeException("HTTP is not supported.");
            }
            connection.setRequestProperty("User-Agent", "DependencyDownload/@VERSION@");
        }
        return connection;
    }

    /**
     * Gets the default buffer size (in bytes) for downloading files from this repository, defaults to {@code 8192}.
     * @return the buffer size
     */
    default int getBufferSize() {
        return 8192;
    }
}
