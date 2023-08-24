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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import grails.util.Environment

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowCommandLog
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog

/**
 * Helper to add new logs to an {@link WorkflowStep} using an separate transaction, so it is added persistently.
 */
@Transactional
class LogService {

    SecurityService securityService

    ProcessingOptionService processingOptionService

    /**
     * Flag containing the information, whether workflow test are currently running.
     *
     * The information is used to log all workflow jobs additional to Slf4j to see the message directly during running the test.
     */
    final boolean workflowTest = (Environment.current.name == "WORKFLOW_TEST")

    /**
     * Add the message as {@link WorkflowMessageLog} to {@link WorkflowStep} in a new transaction, so it is added persistently
     */
    void addSimpleLogEntry(WorkflowStep workflowStep, GString message) {
        addSimpleLogEntry(workflowStep, message.toString())
    }

    /**
     * Add the massage as {@link WorkflowMessageLog} to {@link WorkflowStep} in a new transaction, so it is added persistently
     */
    void addSimpleLogEntry(WorkflowStep workflowStep, String message) {
        SessionUtils.withNewTransaction {
            new WorkflowMessageLog([
                    workflowStep: workflowStep,
                    message     : message,
                    createdBy   : userName(),
            ]).save(flush: true)
        }
        if (workflowTest) {
            log.debug("Message to ${workflowStep.id}: ${message}")
        }
    }

    /**
     * Add the massage including the stacktrace as {@link WorkflowMessageLog} to {@link WorkflowStep} in a new transaction, so it is added persistently
     */
    void addSimpleLogEntryWithException(WorkflowStep workflowStep, String message, Throwable t) {
        String stacktrace = StackTraceUtils.getStackTrace(t)
        SessionUtils.withNewTransaction {
            new WorkflowMessageLog([
                    workflowStep: workflowStep,
                    message     : "${message}\n\n${stacktrace}",
                    createdBy   : userName(),
            ]).save(flush: true)
        }
        if (workflowTest) {
            log.debug("Message to ${workflowStep.id}: ${message}\n${stacktrace}")
        }
    }

    /**
     * Add the message as {@link WorkflowCommandLog} to {@link WorkflowStep} in a new transaction, so it is added persistently
     */
    void addCommandLogEntry(WorkflowStep workflowStepParam, String commandParam, ProcessOutput processOutput) {
        SessionUtils.withNewTransaction {
            //map constructor won't work, if a string is empty, since that is mapped to null and then the validation fail
            WorkflowCommandLog log = new WorkflowCommandLog()
            log.with {
                workflowStep = workflowStepParam
                command = commandParam
                exitCode = processOutput.exitCode
                stdout = processOutput.stdout
                stderr = processOutput.stderr
                createdBy = userName()
                save(flush: true)
            }
        }
        if (workflowTest) {
            log.debug("Command to ${workflowStepParam.id}: ${commandParam}\n${processOutput}")
        }
    }

    private String userName() {
        return securityService.currentUser?.username ?:
                processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_SYSTEM_USER)
    }
}
