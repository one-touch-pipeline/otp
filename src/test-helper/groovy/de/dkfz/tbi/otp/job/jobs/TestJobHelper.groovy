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

package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*

import static de.dkfz.tbi.otp.ngsdata.DomainFactory.createAndSaveProcessingStep
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.createProcessParameter
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * Helper class which returns job system related database objects
 */
class TestJobHelper {

    static TestJob createTestJobWithProcessingStep(final ProcessParameterObject processParameterValue = null) {
        ProcessingStep processingStep = createAndSaveProcessingStep()
        if (processParameterValue != null) {
            assert createProcessParameter(processingStep.process, processParameterValue).save(failOnError: true)
        }
        TestJob testJob = new TestJob(processingStep, [])
        return testJob
    }

    static Collection<Process> findProcessesForPlanName(String planName) {
        return Process.findAllByJobExecutionPlan(findJobExecutionPlan(planName))
    }

    static JobExecutionPlan findJobExecutionPlan(String planName) {
        return exactlyOneElement(JobExecutionPlan.findAllByName(planName))
    }
}
