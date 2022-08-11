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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

class AbstractJobImplSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ClusterJob,
                JobDefinition,
                JobExecutionPlan,
                Process,
                ProcessingStep,
                ProcessingStepUpdate,
                Realm,
        ]
    }

    AbstractJobImpl abstractJobImpl
    TestConfigService configService

    Map<String, ProcessingStep> processingStepHierarchy

    void setup() {
        processingStepHierarchy = createProcessingStepWithHierarchy()
    }

    void "failedOrNotFinishedClusterJobs, with no send step, should throw RuntimeException"() {
        given:
        abstractJobImpl = Spy(AbstractJobImpl) {
            2 * processingStep >> DomainFactory.createProcessingStep()
        }

        when:
        abstractJobImpl.failedOrNotFinishedClusterJobs()

        then:
        RuntimeException e = thrown()
        e.message.contains("No sending processing step found for")
    }

    void "test failedOrNotFinishedClusterJobs, no ClusterJob for send step, throws RunTimeException"() {
        given:
        abstractJobImpl = Spy(AbstractJobImpl) {
            1 * processingStep >> processingStepHierarchy.validate
        }
        abstractJobImpl.jobStatusLoggingService = new JobStatusLoggingService()

        DomainFactory.createClusterJob(processingStep: processingStepHierarchy.validate)

        when:
        abstractJobImpl.failedOrNotFinishedClusterJobs()

        then:
        RuntimeException e = thrown()
        e.message.contains("No ClusterJobs found for")
    }

    void "test failedOrNotFinishedClusterJobs return list of failed or not finished jobs"() {
        given:
        configService = new TestConfigService()
        abstractJobImpl = Spy(AbstractJobImpl) {
            1 * processingStep >> processingStepHierarchy.validate
        }
        abstractJobImpl.jobStatusLoggingService = new JobStatusLoggingService()
        abstractJobImpl.jobStatusLoggingService.configService = configService

        ClusterJob clusterJobSend = DomainFactory.createClusterJob(processingStep: processingStepHierarchy.send)

        expect:
        [clusterJobSend] == abstractJobImpl.failedOrNotFinishedClusterJobs()
    }

    private Map<String, ProcessingStep> createProcessingStepWithHierarchy() {
        ProcessingStep send = DomainFactory.createProcessingStepWithUpdates()

        ProcessingStep wait = DomainFactory.createProcessingStep(process: send.process, previous: send)
        DomainFactory.createProcessingStepWithUpdates(wait)

        ProcessingStep validate = DomainFactory.createProcessingStep(process: wait.process, previous: wait)
        DomainFactory.createProcessingStepWithUpdates(validate)

        return [
                send: send,
                wait: wait,
                validate: validate,
        ]
    }
}
