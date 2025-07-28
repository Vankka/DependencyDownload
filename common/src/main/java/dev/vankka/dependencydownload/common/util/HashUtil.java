/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Vankka
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

package dev.vankka.dependencydownload.common.util;

import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * A helper class to get standard hashes from {@link MessageDigest}s and {@link File}s.
 */
@ApiStatus.Internal
public final class HashUtil {

    private HashUtil() {}

    /**
     * Gets the hash of the provided file
     * @param file the file
     * @param digest the message digest to use to make the hash
     * @return the file's hash in standard format
     * @throws IOException if reading the file was unsuccessful
     */
    public static String getFileHash(Path file, MessageDigest digest) throws IOException {
        digest.reset();

        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024];
            int total;
            while ((total = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, total);
            }
        }

        return getHash(digest);
    }

    /**
     * Gets the hash from the provided {@link MessageDigest}.
     * @param digest the message digest
     * @return the hash in standard format
     */
    public static String getHash(MessageDigest digest) {
        StringBuilder result = new StringBuilder();
        for (byte b : digest.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
