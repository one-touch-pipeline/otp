package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.infrastructure.*

/**
 * It looks like exists() is cached (in NFS?). The cache can be cleared by calling canRead() for files and
 * by calling list() (for directories canRead() does not clear the cache in all cases)
 * - at least in the cases that we observed.
 * To make the methods work with both files and directories, both are called.
 */
class WaitingFileUtils {
    /**
     * Waits until the specified file system object exists or the specified timeout elapsed.
     */
    @Deprecated
    public static void waitUntilExists(File file) {
        FileService.waitUntilExists(file.toPath())
    }

    /**
     * Waits until the specified file system object does not exist or the specified timeout elapsed.
     */
    @Deprecated
    public static void waitUntilDoesNotExist(File file) {
        FileService.waitUntilDoesNotExist(file.toPath())
    }
}
