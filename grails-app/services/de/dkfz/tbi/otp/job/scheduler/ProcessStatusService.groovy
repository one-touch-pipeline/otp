package de.dkfz.tbi.otp.job.scheduler

import static org.springframework.util.Assert.*

/**
 * This methods in this class are part of a solution to check, if the jobs on the cluster are finished successfully or not.
 * The procedure is the following: at the command, which will be submitted to the cluster, writes the name of the current job/class
 * in a specified file (statusLogFile). In the next job it is checked (statusSuccessful), if the logFile contains the name of the
 * previous job/class. If this is the case the cluster job was successful.
 *
 *
 * @deprecated This service is deprecated by the new {@link JobStatusLoggingService}.
 */
@Deprecated
class ProcessStatusService {

    /**
     * @param path, to the file where the status of the cluster job will be logged
     * @return the path to the file, where the status of the jobs is logged
     */
    String statusLogFile(String path) {
        notNull(path, "the import for the method statusLogFile is null")
        File dir = new File(path)
        return path + "/status.log"
    }

    /**
     * @param logfile the file where the successful jobs are logged
     * @param previousJob the class name of the previous job
     * @return true if previous job was logged
     */
    boolean statusSuccessful(String logfile, String previousJob) {
        notNull(logfile, "the import 'logfile' for the method statusSuccessful is null")
        notNull(previousJob, "the import 'previousJob' for the method statusSuccessful is null")
        File file = new File(logfile)
        isTrue(file.canRead(), "logfile ${file} is not readable")
        return file.readLines().contains(previousJob)
    }
}
