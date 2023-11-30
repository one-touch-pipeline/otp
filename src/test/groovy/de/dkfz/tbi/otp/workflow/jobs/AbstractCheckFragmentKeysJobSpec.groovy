/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.jobs

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.shared.MissingFragmentKeysException
import de.dkfz.tbi.otp.workflowExecution.*

abstract class AbstractCheckFragmentKeysJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    protected WorkflowRun run

    protected WorkflowStep workflowStep

    protected AbstractCheckFragmentKeysJob job

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
        ]
    }

    abstract protected String workflowName()

    abstract protected AbstractCheckFragmentKeysJob createJob()

    /**
     * Returns the combined config with complete keys which should include at least the list from {@link #getRequiredKeys()}
     * @return correct combined config
     */
    abstract protected String getCombinedConfig()

    /**
     * Returns the combined config with missing keys which are in the list from {@link #getRequiredKeys()}
     * @return combined config with missing keys
     */
    abstract protected String getCombinedConfigMissingKeys()

    /**
     * Required keys are keys to be checked their existence in the job
     * @return key paths
     */
    abstract protected Set<String> getRequiredKeys()

    /**
     * Missing keys are keys to be listed as missing keys during the job execution
     * They are included as text in the thrown exception
     * @return key paths
     */
    abstract protected Set<String> getMissingKeys()

    void setup() {
        run = createWorkflowRun([
                workflow: createWorkflow([
                        name: workflowName(),
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])

        job = createJob()
        job.logService = Mock(LogService)
        job.messageSourceService = Mock(MessageSourceService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
    }

    void "execute(workflowStep) should check the keys in combined config and throw no exception"() {
        given:
        final String messageText = "All required keys are found in combined config fragment."
        run.combinedConfig = combinedConfig

        when:
        job.execute(workflowStep)

        then:
        noExceptionThrown()
        1 * job.messageSourceService.createMessage("workflow.job.checkFragmentKeys.ok") >> {
            return messageText
        }
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        1 * job.logService.addSimpleLogEntry(workflowStep, _) >> { arguments ->
            assert arguments[1].contains(messageText)
        }
    }

    void "execute(workflowStep) should throw exception if some keys are not found"() {
        given:
        final String messageText = "required key(s) not found in the config fragment in ${run}:"
        run.combinedConfig = combinedConfigMissingKeys

        when:
        job.execute(workflowStep)

        then:
        thrown(MissingFragmentKeysException)
        1 * job.messageSourceService.createMessage("workflow.job.checkFragmentKeys.missing", _) >> {
            return messageText + "\n"
        }
        1 * job.logService.addSimpleLogEntry(workflowStep, _) >> { arguments ->
            assert arguments[1].contains(messageText)
            missingKeys.each {
                assert arguments[1].contains(it)
            }
        }
    }
}
