/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.scheduler

import grails.gorm.transactions.Transactional

import static org.springframework.util.Assert.isTrue
import static org.springframework.util.Assert.notNull

/**
 * This methods in this class are part of a solution to check, if the jobs on the cluster are finished successfully or not.
 * The procedure is the following: at the command, which will be submitted to the cluster, writes the name of the current job/class
 * in a specified file (statusLogFile). In the next job it is checked (statusSuccessful), if the logFile contains the name of the
 * previous job/class. If this is the case the cluster job was successful.
 *
 * @deprecated This service is deprecated by the new {@link JobStatusLoggingService}.
 */
@Deprecated
@Transactional
class ProcessStatusService {

    /**
     * @param path, to the file where the status of the cluster job will be logged
     * @return the path to the file, where the status of the jobs is logged
     */
    String statusLogFile(String path) {
        notNull(path, "the import for the method statusLogFile is null")
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
