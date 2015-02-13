package de.dkfz.tbi.otp.utils

class WaitingFileUtils {

    /**
     * This property is needed to have the possibility to increase the waiting time until files exist.
     * This is needed since when the files are too small they make problems due to the NFS.
     */
    public static int extendedWaitingTime = 1000

    /**
     * It looks like exists() is cached (in NFS?). The cache can be cleared by calling canRead() for files and
     * by calling list() (for directories canRead() does not clear the cache in all cases)
     * - at least in the cases that we observed.
     * To make the methods work with both files and directories, both are called.
     */

    public static boolean confirmExists(File file, int waitingTime = 1000) {
        return ThreadUtils.waitFor({ file.list() || file.canRead(); file.exists() }, waitingTime, 50)
    }
    public static boolean confirmDoesNotExist(File file, int waitingTime = 1000) {
        return ThreadUtils.waitFor({ file.list() || file.canRead(); !file.exists() }, waitingTime, 50)
    }
}
