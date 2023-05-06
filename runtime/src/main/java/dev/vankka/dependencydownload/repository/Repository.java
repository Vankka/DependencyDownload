package dev.vankka.dependencydownload.repository;

import dev.vankka.dependencydownload.dependency.Dependency;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
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
    default HttpsURLConnection openConnection(Dependency dependency) throws IOException {
        URLConnection connection = createURL(dependency).openConnection();
        if (!(connection instanceof HttpsURLConnection)) {
            throw new RuntimeException("HTTP is not supported.");
        }

        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
        httpsConnection.setRequestProperty("User-Agent", "DependencyDownload/@VERSION@");
        return httpsConnection;
    }

    /**
     * Gets the default buffer size (in bytes) for downloading files from this repository, defaults to {@code 8192}.
     * @return the buffer size
     */
    default int getBufferSize() {
        return 8192;
    }
}
