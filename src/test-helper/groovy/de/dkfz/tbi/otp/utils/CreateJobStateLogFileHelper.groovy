/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile.LogFileEntry

class CreateJobStateLogFileHelper {

    static JobStateLogFile createJobStateLogFile(File tempDir, List<LogFileEntry> listOfLogFileEntryValues) {
        File file = new File(tempDir, JobStateLogFile.JOB_STATE_LOG_FILE_NAME)
        file.createNewFile()

        file << listOfLogFileEntryValues.collect { convertLogFileEntryToString(it) }.join("\n")

        return JobStateLogFile.getInstance(tempDir)
    }

    static LogFileEntry createJobStateLogFileEntry(Map properties = [:]) {
        return new LogFileEntry([
                clusterJobId: "testJobId",
                statusCode: "0",
                timeStamp: 0L,
                jobClass: "testJobClass",
        ] + properties as HashMap)
    }

    static String convertLogFileEntryToString(LogFileEntry entry) {
        return "${entry.clusterJobId}:${entry.statusCode}:${entry.timeStamp}:${entry.jobClass}"
    }

    // when using PBS, the cluster job ID also contains a host name
    static String convertLogFileEntryToStringIncludingHost(LogFileEntry entry) {
        return "${entry.clusterJobId}.host.name:${entry.statusCode}:${entry.timeStamp}:${entry.jobClass}"
    }
}
