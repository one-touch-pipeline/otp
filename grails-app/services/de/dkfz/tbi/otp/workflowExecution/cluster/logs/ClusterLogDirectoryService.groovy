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

import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

/**
 * Service to create and return the directory used by the cluster to write the log files (stdout/stderr).
 *
 * It is only used for cluster jobs sent directly by OTP.
 *
 * The directory of the file is created, if it doesn't exist yet.
 */
@CompileStatic
@Transactional
class ClusterLogDirectoryService extends AbstractLogDirectoryService {

    final static String CLUSTER_LOG_BASE_DIR = 'cluster-log'

    @Override
    protected Path createAndGetLogDirectory(Date date) {
        return createAndGetLogDirectoryHelper(date, CLUSTER_LOG_BASE_DIR)
    }

    Path createAndGetLogDirectory(WorkflowStep workflowStep) {
        return createAndGetLogDirectory(workflowStep.dateCreated)
    }
}
