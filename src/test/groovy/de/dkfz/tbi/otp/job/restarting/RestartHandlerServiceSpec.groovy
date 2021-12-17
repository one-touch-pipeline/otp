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
package de.dkfz.tbi.otp.job.restarting

import grails.testing.gorm.DataTest
import org.slf4j.Logger
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@SuppressWarnings("UnnecessaryGetter")
class RestartHandlerServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                JobErrorDefinition,
                JobDefinition,
                JobExecutionPlan,
                Process,
                ProcessingError,
                ProcessingStep,
                ProcessingStepUpdate,
        ]
    }

    @Unroll
    void "test handleRestart"() {
        given:
        int countCall = isHandleTypeMessageAndCalledHandleActionCalled ? 1 : 0
        RestartHandlerService service = new RestartHandlerService(
                restartCheckerService: Mock(RestartCheckerService) {
                    isJobAlreadyRestarted(_) >> jobRestarted
                    isWorkflowAlreadyRestarted(_) >> workflowRestarted
                    canJobBeRestarted(_) >> canJobRestart
                    canWorkflowBeRestarted(_) >> canWorkflowRestart
                },
                restartParseService: Mock(RestartParseService) {
                    countCall * handleTypeMessage(_, _) >> JobErrorDefinition.Action.STOP
                    0 * _
                },
                restartActionService: Mock(RestartActionService) {
                    countCall * handleAction(_, _)
                    0 * _
                }
        )
        ProcessingStep processingStep = DomainFactory.createProcessingStep()
        Job job = GroovyMock(Job) {
            _ * getLog() >> Mock(Logger) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> processingStep
        }
        if (hasJobErrorDefinitions) {
            DomainFactory.createJobErrorDefinition([jobDefinitions: [processingStep.jobDefinition]])
        }

        when:
        service.handleRestart(job)

        then:
        noExceptionThrown()

        where:
        jobRestarted | workflowRestarted | canJobRestart | canWorkflowRestart | hasJobErrorDefinitions || isHandleTypeMessageAndCalledHandleActionCalled
        false        | false             | true          | true               | true                   || true
        true         | false             | true          | true               | true                   || false
        false        | true              | true          | true               | true                   || false
        true         | true              | true          | true               | true                   || false
        false        | false             | false         | true               | true                   || true
        false        | false             | true          | false              | true                   || true
        false        | false             | false         | false              | true                   || false
        false        | false             | true          | true               | false                  || false
    }
}
