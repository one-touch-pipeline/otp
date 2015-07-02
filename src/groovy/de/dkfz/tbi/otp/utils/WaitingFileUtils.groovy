package de.dkfz.tbi.otp.utils

import grails.util.Environment

/**
 * It looks like exists() is cached (in NFS?). The cache can be cleared by calling canRead() for files and
 * by calling list() (for directories canRead() does not clear the cache in all cases)
 * - at least in the cases that we observed.
 * To make the methods work with both files and directories, both are called.
 */
class WaitingFileUtils {

    /**
     * This property is needed to have the possibility to increase the waiting time until files exist.
     * This is needed since when the files are too small they make problems due to the NFS.
     */
    public static int extendedWaitingTime = 60000

    public static long defaultTimeoutMillis = 1000L

    static {
        if (Environment.current == Environment.TEST) {
            defaultTimeoutMillis = 0L
            extendedWaitingTime = 0L
        }
    }

    /**
     * Waits until the specified file system object exists or the specified number of milliseconds elapsed.
     * @return true if the file system object exists; false if timed out.
     */
    public static boolean waitUntilExists(File file, long timeoutMillis = defaultTimeoutMillis) {
        return ThreadUtils.waitFor({ file.list() || file.canRead(); file.exists() }, timeoutMillis, 50)
    }

    /**
     * Waits until the specified file system object does not exist or the specified number of milliseconds elapsed.
     * @return true if the file system object does not exist; false if timed out.
     */
    public static boolean waitUntilDoesNotExist(File file, long timeoutMillis = defaultTimeoutMillis) {
        return ThreadUtils.waitFor({ file.list() || file.canRead(); !file.exists() }, timeoutMillis, 50)
    }
}
