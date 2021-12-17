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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack

@Rollback
@Integration
class ProcessingStepIntegrationTests {

    @Test
    void testBelongsToMultiJob_WhenJobIsMultiJob_ShouldReturnTrue() {
        // AbstractOtpJob as dummy for Multijob
        Class testMultiJob = AbstractOtpJob
        ProcessingStep p = DomainFactory.createProcessingStep(jobClass: testMultiJob.getName())
        assert p.belongsToMultiJob()
    }

    @Test
    void testFindTopMostProcessingStep_WhenRestartedProcessingStep_ShouldReturnOriginalProcessingStep() {
        ProcessingStep originalStep = DomainFactory.createAndSaveProcessingStep()
        RestartedProcessingStep step1 = DomainFactory.createAndSaveRestartedProcessingStep(originalStep)
        RestartedProcessingStep step2 = DomainFactory.createAndSaveRestartedProcessingStep(step1)

        assert originalStep == ProcessingStep.findTopMostProcessingStep(step2)
    }

    @Test
    void testFindTopMostProcessingStep_WhenProcessingStep_ShouldReturnProcessingStep() {
        ProcessingStep step = DomainFactory.createAndSaveProcessingStep()

        assert step == ProcessingStep.findTopMostProcessingStep(step)
    }

    @Test
    void testClusterJobName() {
        JobExecutionPlan jobExecutionPlan = DomainFactory.createJobExecutionPlan(
                name: "testWorkFlow"
        )
        Process process = DomainFactory.createProcess(
                jobExecutionPlan: jobExecutionPlan
        )
        ProcessingStep step = DomainFactory.createProcessingStep(
                jobClass: "foo",
                process: process,
        )
        String expected = "otp_test_testWorkFlow_${step.id}_foo"

        assert expected == step.getClusterJobName()
    }

    @Test
    void testClusterJobNameWithIndividual() {
        JobExecutionPlan jobExecutionPlan = DomainFactory.createJobExecutionPlan(
                name: "testWorkFlow"
        )
        Process process = DomainFactory.createProcess(
                jobExecutionPlan: jobExecutionPlan
        )
        ProcessingStep step = DomainFactory.createProcessingStep(
                jobClass: "foo",
                process: process,
        )
        SeqTrack seqTrack = DomainFactory.createSeqTrack([
                sample: DomainFactory.createSample([
                        individual: DomainFactory.createIndividual([
                                pid: 'pid',
                        ])
                ]),
        ])

        DomainFactory.createProcessParameter([
                process: process,
                value: seqTrack.id.toString(),
                className: seqTrack.getClass().getName(),
        ])
        String expected = "otp_test_pid_testWorkFlow_${step.id}_foo"

        assert expected == step.getClusterJobName()
    }
}
