package dev.vankka.dependencydownload.repository;

import dev.vankka.dependencydownload.dependency.Dependency;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public interface Repository {

    /**
     * The maven repository host's address.
     * @return the host's address, without the path to the actual dependency and slash before the path
     */
    String getHost();

    /**
     * Creates a url for the given {@link Dependency} with the {@link #getHost()}.
     *
     * @param dependency the dependency to generate the url for
     * @return the url
     * @throws MalformedURLException if the url syntax is invalid
     */
    default URL createURL(Dependency dependency) throws MalformedURLException {
        return new URL(getHost() + '/' + dependency.getMavenPath());
    }

    /**
     * Opens a connection from a url generated with {@link #createURL(Dependency)}.
     *
     * @param dependency the dependency used to generate the url
     * @return the opened {@link HttpsURLConnection}
     * @throws IOException if opening connection fails
     */
    default HttpsURLConnection openConnection(Dependency dependency) throws IOException {
        return (HttpsURLConnection) createURL(dependency).openConnection();
    }
}
