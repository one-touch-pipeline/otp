package de.dkfz.tbi.otp.dataprocessing.roddy

import de.dkfz.tbi.otp.utils.WaitingFileUtils
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable

/**
 * represents a jobStateLogfile.txt file generated by Roddy in
 * roddyExecution directory.
 */
public class JobStateLogFile {
    /**
     * the name of jobStateLog file in the roddyExecution directory
     */
    private static final String JOB_STATE_LOG_FILE_NAME = "jobStateLogfile.txt"

    /**
     * each line in the file must satisfy this expression
     */
    private static final String JOB_STATE_LOGFILE_REGEX = /^(.+?)\.(.+?):(.+?):(.+?):(.*)$/

    /**
     * represents content of the file; entries of the files are grouped by PBS job id
     */
    private final Map<String, List<LogFileEntry>> logFileEntries

    /**
     * represents JobStateLogFile on the file-system
     */
    private final File file

    /**
     * @param roddyExecutionDirectory subdirectory of
     * {@link de.dkfz.tbi.otp.dataprocessing.RoddyBamFile#getTmpRoddyExecutionStoreDirectory} corresponding to a roddy call
     */
    private JobStateLogFile(File roddyExecutionDirectory) {
        file = new File(roddyExecutionDirectory, JOB_STATE_LOG_FILE_NAME)
        validateFile()
        logFileEntries = parseJobStateLogFile().asImmutable()
    }

    public static JobStateLogFile getInstance(File roddyExecutionDirectory) {
        return new JobStateLogFile(roddyExecutionDirectory)
    }

    private validateFile() {
        if (!WaitingFileUtils.confirmExists(file)) {
            throw new RuntimeException("${JOB_STATE_LOG_FILE_NAME} is not found in ${file.parentFile}")
        }
        if (!file.canRead()) {
            throw new RuntimeException("file ${file} exists, but is not readable")
        }
    }

    private Map<String, List<LogFileEntry>> parseJobStateLogFile() {
        Map<String, List<LogFileEntry>> entries = [:]

        file.eachLine { line ->
            def matcher = line =~ JOB_STATE_LOGFILE_REGEX
            if (matcher) {
                LogFileEntry logFileEntry = new LogFileEntry(
                        pbsId: matcher[0][1],
                        host: matcher[0][2],
                        statusCode: matcher[0][3],
                        timeStamp: Long.parseLong(matcher[0][4]),
                        jobClass: matcher[0][5]
                )

                List<LogFileEntry> logFileEntries = entries[logFileEntry.pbsId]
                if (logFileEntries) {
                    if (logFileEntry.timeStamp < logFileEntries.last().timeStamp) {
                        throw new RuntimeException("Later JobStateLogFile entry with pbsID: ${logFileEntry.pbsId} " +
                                "has timestamp which is less than one for previous entries.")
                    }
                } else {
                    logFileEntries = []
                    entries.put(logFileEntry.pbsId, logFileEntries)
                }
                logFileEntries << logFileEntry
            } else {
                throw new RuntimeException("${file} contains non-matching entry: ${line}")
            }
        }
        return entries
    }

    public boolean containsPbsId(String pbsId) {
        return logFileEntries.containsKey(pbsId)
    }

    public boolean isClusterJobInProgress(String pbsId) {
        return logFileEntries.get(pbsId)?.size() == 1
    }

    public boolean isClusterJobFinishedSuccessfully(String pbsId) {
        return getPropertyFromLatestLogFileEntry(pbsId, "statusCode") == "0"
    }

    public String getPropertyFromLatestLogFileEntry(String pbsId, String property) {
        return logFileEntries.get(pbsId)?.last()?."${property}"
    }

    public boolean isEmpty() {
        return file.length() == 0
    }

    public String getFilePath() {
        return file.absolutePath
    }

    @Immutable
    @EqualsAndHashCode
    public static class LogFileEntry {
        String pbsId
        String host
        String statusCode
        long timeStamp
        String jobClass

        public String toString() {
            return "${pbsId}.${host}:${statusCode}:${timeStamp}:${jobClass}"
        }
    }
}
