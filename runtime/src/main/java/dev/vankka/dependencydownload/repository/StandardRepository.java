package dev.vankka.dependencydownload.repository;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused") // API
public class StandardRepository implements Repository {

    private final String host;

    /**
     * Create a standard repository.
     * @param host the host address, if it ends with {@code /} it will automatically be removed
     */
    public StandardRepository(@NotNull String host) {
        this.host = host.endsWith("/")
                    ? host.substring(0, host.length() - 1)
                    : host;
    }

    @Override
    public String getHost() {
        return host;
    }
}
