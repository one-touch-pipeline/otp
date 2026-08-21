/*
 * Copyright 2011-2026 The OTP authors
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
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.otp.workflow.shared.FailedLoadingDbObjectException
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

import java.time.Instant

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.BLACKLIST_IMPORT_SOURCE_NOTIFICATION

@Slf4j
@Component
class FastqImportWorkflowCreatorScheduler extends AbstractWorkflowCreatorScheduler {

    @Autowired
    DataInstallationInitializationService dataInstallationInitializationService

    @Autowired
    FastqImportInstanceService fastqImportInstanceService

    @Autowired
    MetaDataFileService metaDataFileService

    @Autowired
    TicketService ticketService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    Long getNextWaitingImportId() {
        FastqImportInstance fastqImportInstance =  fastqImportInstanceService.waiting()
        return fastqImportInstance ? fastqImportInstance.id : 0L
    }

    @Override
    void updateImportState(Long id, WorkflowCreateState state) {
        fastqImportInstanceService.updateState(id, state)
    }

    @Override
    @Transactional(readOnly = true)
    protected String getImportIdentifier(Long importId) {
        FastqImportInstance fastqImportInstance = getFastqImportInstance(importId)
        MetaDataFile metaDataFile = metaDataFileService.findByFastqImportInstance(fastqImportInstance)
        return metaDataFile.fileNameSource
    }

    @Override
    protected DeciderResult createWorkflows(Long importId) {
        FastqImportInstance fastqImportInstanceDb = getFastqImportInstance(importId)
        MetaDataFile metaDataFile = metaDataFileService.findByFastqImportInstance(fastqImportInstanceDb)
        int count = fastqImportInstanceDb.sequenceFiles.size()

        return LogUsedTimeUtils.logUsedTimeStartEnd(log, "create workflows for ${metaDataFile.fileNameSource} " +
                "(dataFiles: ${count}, ${fastqImportInstanceService.countInstancesInWaitingState()} in queue)") {
            List<WorkflowRun> runs = LogUsedTimeUtils.logUsedTimeStartEnd(log, "  create workflow runs for ${count} datafiles") {
                dataInstallationInitializationService.createWorkflowRuns(fastqImportInstanceDb)
            }

            Collection<WorkflowArtefact> workflowArtefacts = runs.collectMany { it.outputArtefacts*.value }
            DeciderResult deciderResult = LogUsedTimeUtils.logUsedTimeStartEnd(log, "  decider for ${count} datafiles") {
                allDecider.decide(workflowArtefacts)
            }

            createSamplePairs(deciderResult.newArtefacts, count)

            fastqImportInstanceService.updateState(fastqImportInstanceDb, WorkflowCreateState.SUCCESS)

            if (fastqImportInstanceDb.ticket) {
                sendImportSourceOperatorNotification(fastqImportInstanceDb)
            }

            return deciderResult
        }
    }

    void sendImportSourceOperatorNotification(FastqImportInstance fastqImportInstance) {
        Ticket ticket = fastqImportInstance.ticket
        String prefixedTicketNumber = ticketService.getPrefixedTicketNumber(ticket)
        String subject = "Import source ready for deletion [${prefixedTicketNumber}]"
        String ticketUrl = ticketService.buildTicketDirectLink(ticket)

        String content = """\
                |Related Ticket: ${prefixedTicketNumber}
                |${ticketUrl}
                |
                |Deletion Script:
                |
                |#!/bin/bash
                |
                |set -e
                |""".stripMargin()

        List<String> pathsToDelete = getPathsToDelete(fastqImportInstance)
        content += pathsToDelete.collect { "rm -f ${it}" }.join("\n")

        if (pathsToDelete) {
            mailHelperService.saveMail(subject, content)
        }
    }

    private List<String> getPathsToDelete(FastqImportInstance fastqImportInstance) {
        return getPrefixBlacklistFilteredStrings(fastqImportInstance.sequenceFiles*.fullInitialPath)
    }

    private List<String> getPrefixBlacklistFilteredStrings(List<String> strings) {
        List<String> blacklist = processingOptionService.findOptionAsList(BLACKLIST_IMPORT_SOURCE_NOTIFICATION)
        return strings.findAll { String path ->
            blacklist.every { it == "" || !path.startsWith(it) }
        }
    }

    @Override
    protected void createSuccessMail(Long importId, Instant instant, String message, Map<String, String> attachments) {
        FastqImportInstance fastqImportInstance = FastqImportInstance.get(importId)
        MetaDataFile metaDataFile = metaDataFileService.findByFastqImportInstance(fastqImportInstance)
        notificationCreator.sendWorkflowCreateSuccessMail(metaDataFile, message, attachments)
    }

    @Override
    protected void createErrorMail(Long importId, Instant instant, Throwable throwable) {
        FastqImportInstance fastqImportInstance = FastqImportInstance.get(importId)
        MetaDataFile metaDataFile = metaDataFileService.findByFastqImportInstance(fastqImportInstance)
        notificationCreator.sendWorkflowCreateErrorMail(metaDataFile, throwable)
    }

    @Override
    protected Instant getExecutionTimestamp(Long importId) {
        FastqImportInstance fastqImportInstanceDb = FastqImportInstance.get(importId)
        return fastqImportInstanceDb ? fastqImportInstanceDb.dateCreated.toInstant() : null
    }

    private FastqImportInstance getFastqImportInstance(Long importId) {
        FastqImportInstance fastqImportInstanceDb = FastqImportInstance.get(importId)
        if (!fastqImportInstanceDb) {
            String message = messageSourceService.getMessage("workflow.bamImport.failedLoadingFastqImportInstance", [importId].toArray())
            log.error(message)
            throw new FailedLoadingDbObjectException(message)
        }
        return fastqImportInstanceDb
    }
}
