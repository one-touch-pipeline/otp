package de.dkfz.tbi.otp.dataprocessing.roddy

import de.dkfz.tbi.otp.utils.WaitingFileUtils
import groovy.io.FileType
import groovy.transform.Immutable

public class JobStateLogFiles {
    public static final String JOBSTATE_LOGFILE_REGEX = /(.+?)\.(.+?):(.+?):(.+?):(.*)/

    private final String path

    final Map<String, List<LogFileEntry>> logFileEntries

    private JobStateLogFiles(String pathToJobStateLogFiles, Map<String, List<LogFileEntry>> entries) {
        path = pathToJobStateLogFiles
        logFileEntries = entries
    }

    static JobStateLogFiles create(String pathToJobStateLogFiles) {
        return new JobStateLogFiles(pathToJobStateLogFiles, parseJobStateLogFile(pathToJobStateLogFiles))
    }

    static Map<String, List<LogFileEntry>> parseJobStateLogFile(String pathToJobStateLogFile) {
        Map<String, List<LogFileEntry>> entries = [:]

        File jobStateLogFiles = new File(pathToJobStateLogFile)
        if (!WaitingFileUtils.confirmExists(jobStateLogFiles)) { return [:] }

        jobStateLogFiles.eachFile(FileType.FILES) { file ->
            file.eachLine {
                def matcher = it =~ JOBSTATE_LOGFILE_REGEX
                if (matcher) {
                    LogFileEntry logFileEntry = new LogFileEntry(
                        pbsId: matcher[0][1],
                        host: matcher[0][2],
                        exitCode: matcher[0][3],
                        timeStamp: matcher[0][4],
                        jobClass: matcher[0][5]
                    )
                    entries[logFileEntry.pbsId] ?: entries.put(logFileEntry.pbsId, [])
                    entries[logFileEntry.pbsId] << logFileEntry
                }
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
        return getPropertyFromLatestLogFileEntry(pbsId, "exitCode") == "0"
    }

    public getPropertyFromLatestLogFileEntry(String pbsId, String property) {
        return logFileEntries.get(pbsId)?.max { it.timeStamp as long }?."${property}"
    }

    @Immutable
    public static class LogFileEntry {
        String pbsId
        String host
        String exitCode
        String timeStamp
        String jobClass

        public String toString() {
            return "${pbsId}.${host}:${exitCode}:${timeStamp}:${jobClass}"
        }
    }
}
