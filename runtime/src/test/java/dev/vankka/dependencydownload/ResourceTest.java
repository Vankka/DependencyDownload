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
