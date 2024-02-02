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
package de.dkfz.tbi.otp.job.restarting

import grails.testing.gorm.DataTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.HelperUtils

class RestartCheckerServiceSpec extends Specification implements DataTest {

    void "canJobBeRestarted, when job is of type AutoRestartable, returns true"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()

        expect:
        restartCheckerService.canJobBeRestarted(Mock(AutoRestartableJob))
    }

    void "canJobBeRestarted, when job is not of type AutoRestartable, returns false"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()

        expect:
        !restartCheckerService.canJobBeRestarted(Mock(Job))
    }

    void "canWorkflowBeRestarted, when start job bean is of type RestartableStartJob, returns true"() {
        given:
        String planName = HelperUtils.uniqueString
        RestartCheckerService restartCheckerService = new RestartCheckerService(
                context: Mock(ApplicationContext) {
                    getBean(_) >> Mock(RestartableStartJob)
                }
        )
        ProcessingStep step = new ProcessingStep(
                process: new Process(
                        jobExecutionPlan: new JobExecutionPlan(
                                name: planName,
                                startJob: new StartJobDefinition(
                                        bean: HelperUtils.uniqueString
                                )
                        )
                )
        )

        expect:
        restartCheckerService.canWorkflowBeRestarted(step)
    }

    void "canWorkflowBeRestarted, when start job bean is not of type RestartableStartJob, returns false"() {
        given:
        String planName = HelperUtils.uniqueString
        RestartCheckerService restartCheckerService = new RestartCheckerService(
                context: Mock(ApplicationContext) {
                    getBean(_) >> Mock(StartJob)
                }
        )
        ProcessingStep step = new ProcessingStep(
                process: new Process(
                        jobExecutionPlan: new JobExecutionPlan(
                                name: planName,
                                startJob: new StartJobDefinition(
                                        bean: HelperUtils.uniqueString
                                )
                        )
                )
        )

        expect:
        !restartCheckerService.canWorkflowBeRestarted(step)
    }

    void "isJobAlreadyRestarted, when job has already been restarted, returns true"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()
        ProcessingStep step = new RestartedProcessingStep()

        expect:
        restartCheckerService.isJobAlreadyRestarted(step)
    }

    void "isJobAlreadyRestarted, when job has not been restarted, returns false"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()
        ProcessingStep step = new ProcessingStep()

        expect:
        !restartCheckerService.isJobAlreadyRestarted(step)
    }

    void "isWorkflowAlreadyRestarted, when workflow has already been restarted, returns true"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()
        ProcessingStep step = new ProcessingStep(
                process: new Process(
                    restarted: new Process()
                )
        )

        expect:
        restartCheckerService.isWorkflowAlreadyRestarted(step)
    }

    void "isWorkflowAlreadyRestarted, when workflow has not been restarted, returns false"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()
        ProcessingStep step = new ProcessingStep(
                process: new Process()
        )

        expect:
        !restartCheckerService.isWorkflowAlreadyRestarted(step)
    }
}
