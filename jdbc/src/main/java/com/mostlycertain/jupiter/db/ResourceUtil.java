package com.mostlycertain.jupiter.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

final class ResourceUtil {
    private ResourceUtil() {
        // Private so instances can not be created
    }

    /**
     * Read a resource file as UTF-8 text.
     *
     * @param testClass    Class to use to search for the resource name.
     * @param resourceName Name of the resource to read.
     * @return Resource text or {@link Optional#empty() empty} if the resource file was not found.
     */
    static Optional<String> loadTextResource(
            final Class<?> testClass,
            final String resourceName
    ) {
        final InputStream resource = testClass.getResourceAsStream(resourceName);

        if (resource == null) {
            return Optional.empty();
        }

        try (final Reader stream = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            final char[] readBuffer = new char[1024];
            final StringBuilder output = new StringBuilder();
            int readLen;

            while ((readLen = stream.read(readBuffer)) >= 0) {
                output.append(readBuffer, 0, readLen);
            }

            return Optional.of(output.toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
