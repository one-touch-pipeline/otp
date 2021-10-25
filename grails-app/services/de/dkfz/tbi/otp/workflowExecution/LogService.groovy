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
import grails.plugin.springsecurity.SpringSecurityService

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.utils.StackTraceUtils
import de.dkfz.tbi.otp.utils.TransactionUtils
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowCommandLog
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog

/**
 * Helper to add new logs to an {@link WorkflowStep} using an separate transaction, so it is added persistently.
 */
@Transactional
class LogService {

    SpringSecurityService springSecurityService

    ProcessingOptionService processingOptionService

    /**
     * Add the massage as {@link WorkflowMessageLog} to {@link WorkflowStep} in a new transaction, so it is added persistently
     */
    void addSimpleLogEntry(WorkflowStep workflowStep, String message) {
        TransactionUtils.withNewTransaction {
            new WorkflowMessageLog([
                    workflowStep: workflowStep,
                    message     : message,
                    createdBy   : userName(),
            ]).save(flush: true)
        }
    }

    /**
     * Add the massage including the stacktrace as {@link WorkflowMessageLog} to {@link WorkflowStep} in a new transaction, so it is added persistently
     */
    void addSimpleLogEntryWithException(WorkflowStep workflowStep, String message, Throwable t) {
        String stacktrace = StackTraceUtils.getStackTrace(t)
        TransactionUtils.withNewTransaction {
            new WorkflowMessageLog([
                    workflowStep: workflowStep,
                    message     : "${message}\n\n${stacktrace}",
                    createdBy   : userName(),
            ]).save(flush: true)
        }
    }

    /**
     * Add the message as {@link WorkflowCommandLog} to {@link WorkflowStep} in a new transaction, so it is added persistently
     */
    void addCommandLogEntry(WorkflowStep workflowStepParam, String commandParam, ProcessOutput processOutput) {
        TransactionUtils.withNewTransaction {
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
    }

    private String userName() {
        return springSecurityService.currentUser?.username ?:
                processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_SYSTEM_USER)
    }
}
