package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile.LogFileEntry
import org.junit.rules.TemporaryFolder

class CreateJobStateLogFileHelper {

    public static JobStateLogFile createJobStateLogFile(File tempDir, List<LogFileEntry> listOfLogFileEntryValues) {
        File file = new File(tempDir, JobStateLogFile.JOB_STATE_LOG_FILE_NAME)
        file.createNewFile()

        file << listOfLogFileEntryValues.join("\n")

        return JobStateLogFile.getInstance(tempDir)
    }


    public static void withTmpRoddyExecutionDir(TemporaryFolder tmpDir, Closure code, String tmpRoddyExecutionStoreName = "exec_890420_133730004_user_analysis") {
        File tmpRoddyExecutionDirectory = tmpDir.newFolder(tmpRoddyExecutionStoreName)
        withTmpRoddyExecutionDir(tmpRoddyExecutionDirectory, code)
    }

    public static void withJobStateLogFile(TemporaryFolder tmpDir, List<LogFileEntry> listOfLogFileEntryValues, Closure code, String tmpRoddyExecutionDirName = "exec_890420_133730004_user_analysis") {
        File tmpRoddyExecutionDirectory = tmpDir.newFolder(tmpRoddyExecutionDirName)
        createJobStateLogFile(tmpRoddyExecutionDirectory, listOfLogFileEntryValues)

        withTmpRoddyExecutionDir(tmpRoddyExecutionDirectory, code)
    }

    public static void withTmpRoddyExecutionDir(File tmpRoddyExecutionDirectory, Closure code) {
        RoddyBamFile.metaClass.getTmpRoddyExecutionStoreDirectory = { ->
            return tmpRoddyExecutionDirectory.parentFile
        }
        try {
            code(tmpRoddyExecutionDirectory)
        } finally {
            GroovySystem.metaClassRegistry.removeMetaClass(RoddyBamFile)
        }
    }

    public static LogFileEntry createJobStateLogFileEntry(Map properties = [:]) {
        return new LogFileEntry([
                pbsId: "testPbsId",
                host: "testHost",
                statusCode: "0",
                timeStamp: 0L,
                jobClass: "testJobClass"
        ] + properties as HashMap)
    }

}
