package com.nuevo.zatca.utils;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class FileUtils {

    private static final String CSR_EXTENSION = ".csr";
    private static final String JSON_EXTENSION = ".json";
    private static final String TEXT_EXTENSION = ".txt";
    private static final String KEY_EXTENSION = ".key";
    private static final String ROOT_DIR = "."; // Current working directory (project root)
    private static final long POLL_INTERVAL_MS = 200;
    private static final long MAX_WAIT_TIME_MS = 1500;

    /**
     * Polls the root directory for the most recent .csr file (waits up to 1.5 seconds) and returns its content.
     *
     * @return content of the latest .csr file as a String
     * @throws IOException if no .csr file is found within the timeout or reading fails
     */
    public static String getLatestFileContentForFileType(String fileType, String filePath) throws IOException {
        long startTime = System.currentTimeMillis();
        String extension = switch (fileType) {
            case ("json") -> JSON_EXTENSION;
            case ("csr") -> CSR_EXTENSION;
            case ("txt") -> TEXT_EXTENSION;
            case ("key") -> KEY_EXTENSION;
            default -> throw new RuntimeException("valid filetype not specified");
        };

        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME_MS) {
            try (Stream<Path> files = Files.list(Paths.get(filePath))) {
                Optional<Path> latestCsrFile = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(extension))
                        .max(Comparator.comparingLong(path -> path.toFile().lastModified()));

                if (latestCsrFile.isPresent()) {
                    return Files.readString(latestCsrFile.get());
                }
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread interrupted while waiting for .csr file", e);
            }
        }

        throw new IOException("No .csr file found within the timeout period (" + MAX_WAIT_TIME_MS + " ms).");
    }
}
