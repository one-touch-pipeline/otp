package de.dkfz.tbi.otp.utils

class CreateFileHelper {

    static File createFile(File file, String content = "some content") {
        if (! file.parentFile.exists()) {
            assert file.parentFile.mkdirs()
        }
        file.text = content
        return file
    }

    static void createRoddyWorkflowConfig(File file, String label) {
        createFile(file, FileContentHelper.createXmlContentForRoddyWorkflowConfig(label))
    }
}
