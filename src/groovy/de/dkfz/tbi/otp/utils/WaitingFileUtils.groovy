package de.dkfz.tbi.otp.utils

class WaitingFileUtils {

    /**
     * It looks like exists() is cached (in NFS?). The cache can be cleared by calling canRead() for files and
     * by calling list() (for directories canRead() does not clear the cache in all cases)
     * - at least in the cases that we observed.
     * To make the methods work with both files and directories, both are called.
     */

    public static boolean confirmExists(File file) {
        return ThreadUtils.waitFor({ file.list() || file.canRead(); file.exists() }, 1000, 50)
    }
    public static boolean confirmDeleted(File file) {
        return ThreadUtils.waitFor({ file.list() || file.canRead(); !file.exists() }, 1000, 50)
    }
}
