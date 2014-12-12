package de.dkfz.tbi.otp.utils

class WaitingFileUtils {
    public static waitForFile(File file) {
        int i = 0
        while (!file.exists()) {
            if(i > 60) {
                assert file.exists()
            }
            sleep(1000)
            i++
        }
        return true
    }
}
