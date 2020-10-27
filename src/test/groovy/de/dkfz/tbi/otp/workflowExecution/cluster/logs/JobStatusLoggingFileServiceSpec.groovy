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

import grails.testing.services.ServiceUnitTest

import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager
import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class JobStatusLoggingFileServiceSpec extends AbstractLogDirectoryServiceSpec implements ServiceUnitTest<JobStatusLoggingFileService> {

    static final String CLUSTER_JOB_ID_VARIABLE = "JOB_ID"

    private Realm realm

    private WorkflowStep workflowStep

    @Override
    Class[] getDomainClassesToMock() {
        [
                Realm,
                WorkflowStep,
        ]
    }

    @Override
    void setup() {
        realm = createRealm()
        workflowStep = createWorkflowStep()
    }

    private void mockClusterJobManagerFactoryService() {
        service.clusterJobManagerFactoryService = Mock(ClusterJobManagerFactoryService) {
            1 * getJobManager(realm) >> { Realm realm ->
                return Mock(BatchEuphoriaJobManager) {
                    1 * getJobIdVariable() >> CLUSTER_JOB_ID_VARIABLE
                    0 * _
                }
            }
            0 * _
        }
    }

    void "constructLogFileLocation, when path not exist and no cluster id is given, then create path and return file pattern"() {
        given:
        Path expected = expectedPath(workflowStep)
        String file = expectedFilePattern(workflowStep)

        mockClusterJobManagerFactoryService()
        mockPathDoesNotExist(expected)

        when:
        String path = service.constructLogFileLocation(realm, workflowStep)

        then:
        path == file
    }

    void "constructLogFileLocation, when path exist and no cluster id is given, then reuse path and return file pattern"() {
        given:
        Path expected = expectedPath(workflowStep)
        String file = expectedFilePattern(workflowStep)
        Files.createDirectories(expected)

        mockClusterJobManagerFactoryService()
        mockPathExist()

        when:
        String path = service.constructLogFileLocation(realm, workflowStep)

        then:
        path == file
    }

    void "constructLogFileLocation, when path not exist and id is given, then create path and return file name"() {
        given:
        String clusterId = "${nextId}"
        WorkflowStep workflowStep = createWorkflowStep()
        Path expected = expectedPath(workflowStep)
        String file = expectedFileName(workflowStep, clusterId)

        mockPathDoesNotExist(expected)

        when:
        String path = service.constructLogFileLocation(realm, workflowStep, clusterId)

        then:
        path == file
    }

    void "constructLogFileLocation, when path exist and id is given, then reuse path and return file name"() {
        given:
        String clusterId = "${nextId}"
        WorkflowStep workflowStep = createWorkflowStep()
        Path expected = expectedPath(workflowStep)
        String file = expectedFileName(workflowStep, clusterId)
        Files.createDirectories(expected)

        mockPathExist()

        when:
        String path = service.constructLogFileLocation(realm, workflowStep, clusterId)

        then:
        path == file
    }

    void "constructLogFileLocation, when cluster id is not given, then return message using the env for cluster id"() {
        given:
        mockClusterJobManagerFactoryService()

        String expected = [
                workflowStep.workflowRun.workflow.name,
                workflowStep.beanName,
                workflowStep.id,
                "\$(echo \${${CLUSTER_JOB_ID_VARIABLE}} | cut -d. -f1)",
        ].join(',')

        when:
        String message = service.constructMessage(realm, workflowStep)

        then:
        message == expected
    }

    void "constructLogFileLocation, when cluster id is given, then return message using the id"() {
        given:
        String clusterId = "${nextId}"

        String expected = [
                workflowStep.workflowRun.workflow.name,
                workflowStep.beanName,
                workflowStep.id,
                clusterId,
        ].join(',')

        when:
        String message = service.constructMessage(realm, workflowStep, clusterId)

        then:
        message == expected
    }

    private Path expectedPath(WorkflowStep workflowStep) {
        return expectedPath(workflowStep.dateCreated, JobStatusLoggingFileService.CHECKPOINT_LOG_DIR)
    }

    private String expectedFilePattern(WorkflowStep workflowStep) {
        return "${expectedPath(workflowStep)}/joblog_${workflowStep.id}_\$(echo \${${CLUSTER_JOB_ID_VARIABLE}} | cut -d. -f1)_${realm.id}.log"
    }

    private String expectedFileName(WorkflowStep workflowStep, String clusterId) {
        return "${expectedPath(workflowStep)}/joblog_${workflowStep.id}_${clusterId}_${realm.id}.log"
    }
}
