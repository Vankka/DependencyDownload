package dev.vankka.dependencydownload.repository;

@SuppressWarnings("unused")
public class StandardRepository implements Repository {

    private final String host;

    public StandardRepository(String host) {
        this.host = host;
    }

    @Override
    public String getHost() {
        return host;
    }
}
