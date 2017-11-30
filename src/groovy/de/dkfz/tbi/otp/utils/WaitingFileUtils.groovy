package de.dkfz.tbi.otp.utils

import org.joda.time.Duration

import grails.util.Environment

/**
 * It looks like exists() is cached (in NFS?). The cache can be cleared by calling canRead() for files and
 * by calling list() (for directories canRead() does not clear the cache in all cases)
 * - at least in the cases that we observed.
 * To make the methods work with both files and directories, both are called.
 */
class WaitingFileUtils {

    public static Duration defaultTimeout = Duration.standardMinutes(2)

    static {
        if (Environment.current == Environment.TEST) {
            defaultTimeout = Duration.ZERO
        }
    }

    /**
     * Waits until the specified file system object exists or the specified timeout elapsed.
     */
    public static void waitUntilExists(File file, Duration timeout = defaultTimeout) {
        assert ThreadUtils.waitFor({ file.list() || file.canRead(); file.exists() }, timeout.millis, 50) :
        "${file} not found."
    }

    /**
     * Waits until the specified file system object does not exist or the specified timeout elapsed.
     */
    public static void waitUntilDoesNotExist(File file, Duration timeout = defaultTimeout) {
        assert ThreadUtils.waitFor({ file.list() || file.canRead(); !file.exists() }, timeout.millis, 50) :
        "${file} still exists."
    }
}
