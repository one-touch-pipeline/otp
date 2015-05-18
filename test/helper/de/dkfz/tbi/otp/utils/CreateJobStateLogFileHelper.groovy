package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFiles

class CreateJobStateLogFileHelper {

    public static JobStateLogFiles createJobStateLogFiles(List<Map<String, String>> listOfLogFileEntryValues) {
        Map<Long, List<JobStateLogFiles.LogFileEntry>> jobStateLogFilesMap = [:]

        listOfLogFileEntryValues.each {
            JobStateLogFiles.LogFileEntry logFileEntry = createLogFileEntry(it)
            jobStateLogFilesMap[logFileEntry.pbsId] ?: jobStateLogFilesMap.put(logFileEntry.pbsId, [])
            jobStateLogFilesMap[logFileEntry.pbsId] << logFileEntry
        }

        JobStateLogFiles.metaClass.static.parseJobStateLogFile = { String p ->
            return jobStateLogFilesMap
        }

        JobStateLogFiles jobStateLogFiles = JobStateLogFiles.create("")

        JobStateLogFiles.metaClass = null

        return jobStateLogFiles
    }

    public static JobStateLogFiles.LogFileEntry createLogFileEntry(Map<String, String> logFileEntryValues) {
        return new JobStateLogFiles.LogFileEntry(
                pbsId: logFileEntryValues.pbsId,
                host: logFileEntryValues.host,
                exitCode: logFileEntryValues.exitCode,
                timeStamp: logFileEntryValues.timeStamp,
                jobClass: logFileEntryValues.jobClass
        )
    }
}
