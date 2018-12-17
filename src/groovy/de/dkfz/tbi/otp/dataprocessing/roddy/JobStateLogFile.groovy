package de.dkfz.tbi.otp.dataprocessing.roddy

import groovy.transform.*

import de.dkfz.tbi.otp.utils.WaitingFileUtils

import java.util.regex.Matcher

/**
 * represents a jobStateLogfile.txt file generated by Roddy in
 * roddyExecution directory.
 */
class JobStateLogFile {
    /**
     * the name of jobStateLog file in the roddyExecution directory
     */
    private static final String JOB_STATE_LOG_FILE_NAME = "jobStateLogfile.txt"

    /**
     * each line in the file must satisfy this expression
     * the hostname is optional and will be ignored, it occurs only on PBS systems
     */
    private static final String JOB_STATE_LOGFILE_REGEX =
            /^(?<id>.+?)(\.(?<hostname>.+?))?:(?<statusCode>.+?):(?<timeStamp>.+?):(?<jobClass>.*)$/
    /**
     * represents content of the file; key is the cluster job id, only the latest entry is stored
     */
    private final Map<String, LogFileEntry> logFileEntries

    /**
     * represents JobStateLogFile on the file-system
     */
    private final File file

    /**
     * @param roddyExecutionDirectory subdirectory of
     * {@link de.dkfz.tbi.otp.dataprocessing.RoddyBamFile#getWorkExecutionStoreDirectory} corresponding to a roddy call
     */
    private JobStateLogFile(File roddyExecutionDirectory) {
        file = new File(roddyExecutionDirectory, JOB_STATE_LOG_FILE_NAME)
        validateFile()
        logFileEntries = parseJobStateLogFile().asImmutable()
    }

    static JobStateLogFile getInstance(File roddyExecutionDirectory) {
        return new JobStateLogFile(roddyExecutionDirectory)
    }

    private validateFile() {
        try {
            WaitingFileUtils.waitUntilExists(file)
        } catch (AssertionError e) {
            throw new RuntimeException("${JOB_STATE_LOG_FILE_NAME} is not found in ${file.parentFile}", e)
        }
        if (!file.canRead()) {
            throw new RuntimeException("file ${file} exists, but is not readable")
        }
    }

    private Map<String, LogFileEntry> parseJobStateLogFile() {
        Map<String, LogFileEntry> entries = [:]

        file.eachLine { line ->
            Matcher matcher = line =~ JOB_STATE_LOGFILE_REGEX
            if (matcher) {
                LogFileEntry logFileEntry = new LogFileEntry(
                        clusterJobId: matcher.group("id"),
                        statusCode: matcher.group("statusCode"),
                        timeStamp: Long.parseLong(matcher.group("timeStamp")),
                        jobClass: matcher.group("jobClass")
                )

                LogFileEntry existingLoFileEntry = entries[logFileEntry.clusterJobId]
                // only the latest entry is stored (i.e. the entry with the highest timestamp)
                if (!existingLoFileEntry ||
                        logFileEntry.timeStamp > existingLoFileEntry.timeStamp) {
                        entries.put(logFileEntry.clusterJobId, logFileEntry)
                }
            } else {
                throw new RuntimeException("${file} contains non-matching entry: ${line}")
            }
        }
        return entries
    }

    boolean containsClusterJobId(String clusterJobId) {
        return logFileEntries.containsKey(clusterJobId)
    }

    boolean isClusterJobFinishedSuccessfully(String clusterJobId) {
        return getPropertyFromLatestLogFileEntry(clusterJobId, "statusCode") == "0"
    }

    String getPropertyFromLatestLogFileEntry(String clusterJobId, String property) {
        return logFileEntries.get(clusterJobId)?."${property}"
    }

    // only used for tests
    private boolean isEmpty() {
        return file.length() == 0
    }

    @Immutable
    @EqualsAndHashCode
    @ToString(includeNames = true)
    static class LogFileEntry {
        String clusterJobId
        String statusCode
        long timeStamp
        String jobClass
    }
}
