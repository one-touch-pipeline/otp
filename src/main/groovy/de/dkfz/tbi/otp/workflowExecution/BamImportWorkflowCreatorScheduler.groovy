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
package de.dkfz.tbi.otp.workflowExecution

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.BamImportInstance
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflow.bamImport.BamImportInitializationService
import de.dkfz.tbi.otp.workflow.bamImport.BamImportService
import de.dkfz.tbi.otp.workflow.shared.FailedLoadingDbObjectException
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

import java.time.Instant

@Slf4j
@Component
class BamImportWorkflowCreatorScheduler extends AbstractWorkflowCreatorScheduler {

    @Autowired
    BamImportService bamImportService

    @Autowired
    BamImportInitializationService bamImportInitializationService

    @Override
    Long getNextWaitingImportId() {
        BamImportInstance importInstance = bamImportService.waiting()
        return importInstance ? importInstance.id : 0L
    }

    @Override
    void updateImportState(Long id, WorkflowCreateState state) {
        bamImportService.updateState(id, state)
    }

    @Override
    protected String getImportIdentifier(Long importId) {
        return "BamImport [ID: ${importId}]"
    }

    @Override
    protected DeciderResult createWorkflows(Long importId) {
        BamImportInstance importInstanceDb = getImportInstance(importId)
        int count = importInstanceDb.externallyProcessedBamFiles.size()

        return LogUsedTimeUtils.logUsedTimeStartEnd(log, "create workflows for ${importInstanceDb} " +
                "(bamFiles: ${count}, ${bamImportService.countInstancesInWaitingState()} in queue)") {
            List<WorkflowRun> runs = LogUsedTimeUtils.logUsedTimeStartEnd(log, "  create workflow runs for ${count} bamFiles") {
                bamImportInitializationService.createWorkflowRuns(importInstanceDb)
            }

            Collection<WorkflowArtefact> workflowArtefacts = runs.collectMany { it.outputArtefacts*.value }

            DeciderResult deciderResult = LogUsedTimeUtils.logUsedTimeStartEnd(log, "  decider for ${count} bamfiles") {
                allDecider.decide(workflowArtefacts)
            }

            createSamplePairs(workflowArtefacts, count)

            bamImportService.updateState(importId, WorkflowCreateState.SUCCESS)

            return deciderResult
        }
    }

    @Override
    protected void createSuccessMail(Long importId, Instant instant, String message) {
        notificationCreator.sendBamImportWorkflowCreateSuccessMail(getImportInstance(importId).ticket, importId, instant, message)
    }

    @Override
    protected void createErrorMail(Long importId, Instant instant, Throwable throwable) {
        notificationCreator.sendBamImportWorkflowCreateErrorMail(getImportInstance(importId).ticket, importId, instant, throwable)
    }

    @Override
    protected Instant getExecutionTimestamp(Long importId) {
        BamImportInstance importInstanceDb = BamImportInstance.get(importId)
        return importInstanceDb ? importInstanceDb.dateCreated.toInstant() : null
    }

    private BamImportInstance getImportInstance(Long importId) {
        BamImportInstance importInstanceDb = BamImportInstance.get(importId)
        if (!importInstanceDb) {
            String message = messageSourceService.getMessage("workflow.bamImport.failedLoadingBamImportInstance", [importId].toArray())
            log.error(message)
            throw new FailedLoadingDbObjectException(message)
        }
        return importInstanceDb
    }
}
