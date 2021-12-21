/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.cluster.logs

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic

import de.dkfz.tbi.util.TimeFormats

import java.nio.file.Path

/**
 * Service to create and return the log file used to save the result of the query for the cluster job state of all cluster jobs of a user.
 *
 * The directory of the file is created, if it doesn't exist yet.
 */
@CompileStatic
@Transactional
class ClusterLogQueryResultFileService extends AbstractLogDirectoryService {

    static final String CLUSTER_JOB_STATES_LOG_DIRECTORY = "cluster-jobs-state"

    @Override
    protected Path createAndGetLogDirectory(Date date) {
        return createAndGetLogDirectoryHelper(date, CLUSTER_JOB_STATES_LOG_DIRECTORY)
    }

    Path logFileWithCreatingDirectory() {
        Date date = configService.currentDate
        return createAndGetLogDirectory(date).resolve("${TimeFormats.TIME_DASHED.getFormattedDate(date)}\'.txt\'")
    }
}
