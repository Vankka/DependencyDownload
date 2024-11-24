package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.resource.DependencyDownloadResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import static dev.vankka.dependencydownload.Helpers.REAL_DEPENDENCY;
import static dev.vankka.dependencydownload.Helpers.REAL_RELOCATION;
import static org.junit.jupiter.api.Assertions.*;

public class ResourceTest {

    private DependencyDownloadResource parseResource(String name) throws IOException {
        URL url = getClass().getClassLoader().getResource(name);
        assertNotNull(url, "resourceURL");
        return DependencyDownloadResource.parse(url);
    }

    @Test
    public void validResourceTest1() throws IOException {
        DependencyDownloadResource parsed = parseResource("valid1.txt");

        assertEquals(1, parsed.getDependencies().size());
        assertEquals(1, parsed.getRelocations().size());
        assertEquals(Collections.singletonList(REAL_DEPENDENCY), parsed.getDependencies());
        assertEquals(Collections.singletonList(REAL_RELOCATION), parsed.getRelocations());
    }

    @Test
    public void invalidResourcesTest() {
        assertThrows(IllegalArgumentException.class, () -> parseResource("invalid1.txt")); // No algorithm
        assertThrows(IllegalArgumentException.class, () -> parseResource("invalid2.txt")); // No hash
        assertThrows(IllegalArgumentException.class, () -> parseResource("invalid2.txt")); // No hash
        assertThrows(IllegalArgumentException.class, () -> parseResource("invalid3.txt")); // No version
        assertThrows(IllegalArgumentException.class, () -> parseResource("invalid4.txt")); // No relocation replacement
    }
}
