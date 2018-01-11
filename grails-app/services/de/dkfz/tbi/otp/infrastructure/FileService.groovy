package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.utils.*
import grails.util.*

import java.nio.file.*
import java.time.*

/**
 * Helper methods to work with file paths
 */
class FileService {

    static boolean isFileReadableAndNotEmpty(final Path file) {
        assert file.isAbsolute()
        try {
            waitUntilExists(file)
        } catch (AssertionError e) {}
        return Files.exists(file) &&  Files.isRegularFile(file) && Files.isReadable(file) && Files.size(file) > 0L
    }

    static void ensureFileIsReadableAndNotEmpty(final Path file) {
        assert file.isAbsolute()
        waitUntilExists(file)
        assert Files.isRegularFile(file)
        assert Files.isReadable(file)
        assert Files.size(file) > 0L
    }

    static void ensureDirIsReadableAndNotEmpty(final Path dir) {
        ensureDirIsReadable(dir)
        assert Files.list(dir).count() != 0
    }

    static void ensureDirIsReadable(final Path dir) {
        waitUntilExists(dir)
        assert Files.isDirectory(dir)
        assert Files.isReadable(dir)
    }

    /**
     * It looks like exists() is cached (in NFS?). The cache can be cleared by calling canRead() for files and
     * by calling list() (for directories canRead() does not clear the cache in all cases)
     * - at least in the cases that we observed.
     * To make the methods work with both files and directories, both are called.
     */

    /**
     * Waits until the specified file system object exists or the specified timeout elapsed.
     */
    static void waitUntilExists(Path file) {
        assert ThreadUtils.waitFor({
            try {
                Files.isDirectory(file) ? Files.list(file) : Files.isReadable(file)
            } catch (NoSuchFileException ignored) {}
            Files.exists(file)
        }, timeout.toMillis(), 50) : "${file} not found."
    }

    /**
     * Waits until the specified file system object does not exist or the specified timeout elapsed.
     */
    static void waitUntilDoesNotExist(Path file) {
        assert ThreadUtils.waitFor({
            try {
                Files.isDirectory(file) ? Files.list(file) : Files.isReadable(file)
            } catch (NoSuchFileException ignored) {
                return true
            }
            !Files.exists(file)
        }, timeout.toMillis(), 50) : "${file} still exists."
    }

    private static Duration getTimeout() {
        (Environment.current == Environment.TEST) ? Duration.ZERO : Duration.ofMinutes(2)
    }
}
