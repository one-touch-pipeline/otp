package de.dkfz.tbi.otp.utils

/**
 */
class CreateFileHelper {

    static File createFile(File file, String content = "some content") {
        if (! file.parentFile.exists()) {
            assert file.parentFile.mkdirs()
        }
        file << content
        return file
    }
}
