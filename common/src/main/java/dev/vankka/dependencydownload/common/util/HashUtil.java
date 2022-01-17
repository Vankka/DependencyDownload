package dev.vankka.dependencydownload.common.util;

import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A helper class to get standard hashes from {@link MessageDigest}s and {@link File}s.
 */
@ApiStatus.Internal
public final class HashUtil {

    private HashUtil() {}

    /**
     * Gets the hash of the provided file
     * @param file the file
     * @param algorithm the hashing algorithm (used on {@link MessageDigest#getInstance(String)})
     * @return the file's hash in standard format
     * @throws NoSuchAlgorithmException if the provided algorithm couldn't be found
     * @throws IOException if reading the file was unsuccessful
     */
    public static String getFileHash(File file, String algorithm) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        try (InputStream inputStream = new FileInputStream(file)) {
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
