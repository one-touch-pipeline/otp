package de.dkfz.tbi.otp.utils

class WaitingFileUtils {
    public static waitForFile(File file) {
        assert ThreadUtils.waitFor({ file.exists() }, 60000, 1000)
        return true
    }
}
