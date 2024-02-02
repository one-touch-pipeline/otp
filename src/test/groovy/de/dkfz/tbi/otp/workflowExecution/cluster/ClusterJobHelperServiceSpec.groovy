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
package de.dkfz.tbi.otp.workflowExecution.cluster

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.util.Environment
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.roddy.config.ResourceSet
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class ClusterJobHelperServiceSpec extends Specification implements ServiceUnitTest<ClusterJobHelperService>, DataTest, WorkflowSystemDomainFactory {

    private static final String QUEUE_NAME = 'queue'

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    @Unroll
    void "mergeResources, when jobSubmissionOptions is #jobSubmissionOptions and queue is queue, then result is #result"() {
        given:
        ProcessingPriority processingPriority = createProcessingPriority([
                queue: QUEUE_NAME,
        ])

        when:
        Map<JobSubmissionOption, String> mergedMap = service.mergeResources(processingPriority, jobSubmissionOptions)

        then:
        TestCase.assertContainSame(mergedMap, result)

        where:
        jobSubmissionOptions                  || result
        [:]                                   || [(JobSubmissionOption.QUEUE): QUEUE_NAME]
        [(JobSubmissionOption.WALLTIME): "7"] || [(JobSubmissionOption.QUEUE): QUEUE_NAME, (JobSubmissionOption.WALLTIME): "7"]
        [(JobSubmissionOption.MEMORY): "3"]   || [(JobSubmissionOption.QUEUE): QUEUE_NAME, (JobSubmissionOption.MEMORY): "3"]
    }

    void "createResourceSet, when jobSubmissionOptions given, then create the correct ResourceSet"() {
        given:
        Map<JobSubmissionOption, String> jobSubmissionOptions = [
                (JobSubmissionOption.QUEUE)       : QUEUE_NAME,
                (JobSubmissionOption.WALLTIME)    : "PT5H",
                (JobSubmissionOption.MEMORY)      : "123M",
                (JobSubmissionOption.CORES)       : "7",
                (JobSubmissionOption.NODES)       : "3",
                (JobSubmissionOption.STORAGE)     : "234M",
                (JobSubmissionOption.NODE_FEATURE): "something",
        ]

        when:
        ResourceSet resourceSet = service.createResourceSet(jobSubmissionOptions)

        then:
        resourceSet.walltime.toHours() == 5
        resourceSet.mem.toString() == "123M"
        resourceSet.queue == QUEUE_NAME
        resourceSet.cores == 7
        resourceSet.nodes == 3
        resourceSet.storage.toString() == "234M"
        resourceSet.additionalNodeFlag == "something"
    }

    void "createResourceSet, when empty jobSubmissionOptions given, then create empty ResourceSet"() {
        given:
        Map<JobSubmissionOption, String> jobSubmissionOptions = [:]

        when:
        ResourceSet resourceSet = service.createResourceSet(jobSubmissionOptions)

        then:
        resourceSet.walltime == null
        resourceSet.mem == null
        resourceSet.queue == null
        resourceSet.cores == null
        resourceSet.nodes == null
        resourceSet.storage == null
        resourceSet.additionalNodeFlag == null
    }

    void "wrapScript, if script given, it is wrapped inside some pre and post work"() {
        given:
        String script = "Some ${nextId}\nscript ${nextId}"
        String logFile = "SomeFile${nextId}"
        String logMessage = "SomeMessage${nextId}"

        List<String> expectedPattern = [
                'set -e\n',
                'umask 0027\n',
                script,
                "touch \"${logFile}\"\n",
                "chmod 0640 \"${logFile}\"\n",
                "echo \"${logMessage}\" >> \"${logFile}\"\n",
        ]
        int remaining = 0

        when:
        String wrapScript = service.wrapScript(script, logFile, logMessage)

        then:
        expectedPattern.each {
            int match = wrapScript.indexOf(it, remaining)
            assert match != -1: "'${it}' was not found in reminding script:\n${wrapScript.substring(remaining)}"
            remaining = match + it.length()
        }
    }

    void "constructJobName, if WorkflowStep given, return the job name"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String expectedName = [
                'otp',
                Environment.current.name.toLowerCase(),
                workflowStep.workflowRun.workflow.name,
                workflowStep.beanName,
                workflowStep.workflowRun.shortDisplayName,
                workflowStep.id,
        ].join('_')

        when:
        String jobName = service.constructJobName(workflowStep)

        then:
        jobName == expectedName
    }
}
