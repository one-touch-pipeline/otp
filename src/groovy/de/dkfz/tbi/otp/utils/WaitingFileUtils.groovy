package de.dkfz.tbi.otp.utils

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

    public static boolean confirmExists(File file, long timeoutMillis = defaultTimeoutMillis) {
        return ThreadUtils.waitFor({ file.list() || file.canRead(); file.exists() }, timeoutMillis, 50)
    }

    public static boolean confirmDoesNotExist(File file, long timeoutMillis = defaultTimeoutMillis) {
        return ThreadUtils.waitFor({ file.list() || file.canRead(); !file.exists() }, timeoutMillis, 50)
    }
}
