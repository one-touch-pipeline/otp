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

import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

/**
 * Service to create and return the checkpoint log file pattern.
 *
 * That file is used to indicate, that cluster jobs has run till the end and is only used for cluster jobs sent by OTP directly.
 *
 * The directory of the file is created, if it doesn't exist yet.
 */
@CompileStatic
@Transactional
class JobStatusLoggingFileService extends AbstractLogDirectoryService {

    final static String CHECKPOINT_LOG_DIR = 'cluster-checkpoint-log'

    final static String LOGFILE_EXTENSION = '.log'

    ClusterJobManagerFactoryService clusterJobManagerFactoryService

    private String shellSnippetForClusterJobId(Realm realm) {
        return "\$(echo \${${clusterJobManagerFactoryService.getJobManager(realm).jobIdVariable}} | cut -d. -f1)"
    }

    @Override
    protected Path createAndGetLogDirectory(Date date) {
        return createAndGetLogDirectoryHelper(date, CHECKPOINT_LOG_DIR)
    }

    /**
     * Creates the status file name.
     *
     * If clusterJobId is given, the final file name is returned, otherwise a file pattern is returned using the cluster JOB_ID environment variable.
     *
     * @param realm the realm the job runs in
     * @param workflowStep the workflowStep of the clusterJob
     * @param clusterJobId an optional cluster job ID. If <code>null</code>, shell code to retrieve the numeric part
     *          of the cluster job id is returned.
     *          (Read: pass the job ID except if the returned string is used in a cluster job shell script)
     * @return the location of the status log file
     */
    String constructLogFileLocation(Realm realm, WorkflowStep workflowStep, String clusterJobId = null) {
        assert realm
        assert workflowStep

        Path logDirectory = createAndGetLogDirectory(workflowStep.dateCreated)

        String fileName = [
                "joblog",
                workflowStep.id,
                clusterJobId ?: shellSnippetForClusterJobId(realm),
                realm.id,
        ].join("_") + LOGFILE_EXTENSION

        return "${logDirectory}/${fileName}"
    }

    /**
     * Constructs a logging message for the status logging from a workflow step.
     * The message is a comma-separated list of elements.
     */
    String constructMessage(Realm realm, WorkflowStep workflowStep, String clusterJobId = null) {
        assert workflowStep
        return [
                workflowStep.workflowRun.workflow.name,
                workflowStep.beanName,
                workflowStep.id,
                clusterJobId ?: shellSnippetForClusterJobId(realm),
        ].join(',')
    }
}
