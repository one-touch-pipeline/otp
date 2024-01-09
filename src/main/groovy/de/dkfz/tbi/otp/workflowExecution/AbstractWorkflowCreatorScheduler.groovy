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
package de.dkfz.tbi.otp.workflowExecution

import grails.async.Promise
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.SystemUserUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

import java.time.Instant

import static grails.async.Promises.task

@Component
@Slf4j
abstract class AbstractWorkflowCreatorScheduler {

    @Autowired
    AllDecider allDecider

    @Autowired
    MessageSourceService messageSourceService

    @Autowired
    NotificationCreator notificationCreator

    @Autowired
    SamplePairDeciderService samplePairDeciderService

    @Autowired
    WorkflowSystemService workflowSystemService

    /**
     * Create workflows in every minute
     * Takes each time one single import in the waiting state
     */
    @Scheduled(fixedDelay = 5000L)
    void scheduleCreateWorkflow() {
        if (!workflowSystemService.enabled) {
            return
        }

        Long importId = nextWaitingImportId
        if (!importId) {
            return
        }

        updateImportState(importId, WorkflowCreateState.PROCESSING)

        createWorkflowsAsync(importId)
    }

    /**
     * only protected for testing, should not used outside this class
     */
    protected Promise<Void> createWorkflowsAsync(long importId) {
        return task {
            createWorkflowsTask(importId)
        }
    }

    /**
     * only protected for testing, should not used outside this class
     */
    @SuppressWarnings("CatchThrowable")
    protected void createWorkflowsTask(long importId) {
        String importIdentifier = getImportIdentifier(importId)
        try {
            DeciderResult deciderResult = createWorkflowsTransactional(importId)
            String message = messageFromDeciderResult(deciderResult)

            sendWorkflowCreateSuccessMail(importId, getExecutionTimestamp(importId), message)
        } catch (Throwable throwable) {
            log.debug("  failed workflow creation for ${importIdentifier}", throwable)
            try {
                sendWorkflowCreateErrorMail(importId, getExecutionTimestamp(importId), throwable)
            } catch (Throwable throwable2) {
                log.debug("  failed error notification for workflow creation of ${importIdentifier}", throwable2)
                throw throwable2
            }
            try {
                updateImportState(importId, WorkflowCreateState.FAILED)
            } catch (Throwable throwable2) {
                log.debug("  failed update status for failed workflow creation of ${importIdentifier}", throwable2)
                throw throwable2
            }
        }
    }

    /**
     * Creates the sample pairs for the alignments.
     * Needed, as long not all analysis workflows are migrated.
     *
     * @deprecated old workflow system
     */
    @Deprecated
    protected void createSamplePairs(Collection<WorkflowArtefact> workflowArtefacts, int count) {
        Long timeSamplePairs = System.currentTimeMillis()
        log.debug("  sample pair creation started")
        List<SeqType> seqTypes = SeqTypeService.allAnalysableSeqTypes
        Collection<AbstractMergingWorkPackage> mergingWorkPackages = workflowArtefacts.findAll {
            it.artefactType == ArtefactType.BAM
        }.collect {
            return (it.artefact.get() as AbstractBamFile).workPackage
        }.findAll {
            it.seqType in seqTypes
        }
        SystemUserUtils.useSystemUser {
            samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)
        }
        log.debug("  sample pair creation finished for ${count} datafiles after: ${System.currentTimeMillis() - timeSamplePairs}ms")
    }

    @Transactional(readOnly = true)
    protected String messageFromDeciderResult(DeciderResult deciderResult) {
        List<String> message = []
        message << "Decider Results"
        if (deciderResult.warnings) {
            message << "Decider created ${deciderResult.warnings.size()} warnings:".toString()
            deciderResult.warnings.each {
                message << "- ${it}".toString()
            }
            message << ""
        }
        if (deciderResult.newArtefacts) {
            message << "Decider created ${deciderResult.newArtefacts.size()} workflow runs / artefact:".toString()
            deciderResult.newArtefacts.each {
                Optional<Artefact> optionalArtefact = it.artefact
                String artefactText = optionalArtefact.present ? optionalArtefact.get().toString() : '-'
                message << "- ${it.producedBy}: ${artefactText}".toString()
            }
        } else {
            message << "No artefacts created"
        }
        message << ""
        message << "Decider log: "
        deciderResult.infos.each {
            message << "- ${it}".toString()
        }
        return message.join('\n')
    }

    /**
     * Get the next available import process id in waiting state
     *
     * @return the id of import process in waiting state
     */
    abstract protected Long getNextWaitingImportId()

    /**
     * Get the import identifier of an import instance
     * Note: no specific location is stored for Bam import
     *
     * @param importId id of FastqImportInstance for Fastq import or BamImportInstance for Bam import
     * @return source file name for fastq or import id for bam import
     */
    abstract protected String getImportIdentifier(Long importId)

    /**
     * Update the current import process state
     *
     * @param importId id of FastqImportInstance for Fastq import or BamImportInstance for Bam import
     * @param state to be set
     */
    abstract protected void updateImportState(Long importId, WorkflowCreateState state)

    /**
     * Create workflows in one single transaction for the given import process
     *
     * @param importId id of FastqImportInstance for Fastq import or BamImportInstance for Bam import
     * @return deciderResult of newly created workflow
     */
    abstract protected DeciderResult createWorkflowsTransactional(Long importId)

    /**
     * Send success notification after workflows are created
     *
     * @param importId id of FastqImportInstance for Fastq import or BamImportInstance for Bam import
     * @param ts timestamp of execution
     * @param message success message
     */
    abstract protected void sendWorkflowCreateSuccessMail(Long importId, Instant instant, String message)

    /**
     * Send error notification after workflows creation failed
     *
     * @param importId id of FastqImportInstance for Fastq import or BamImportInstance for Bam import
     * @param ts timestamp of execution
     * @param throwable which contains the error message and stack trace
     */
    abstract protected void sendWorkflowCreateErrorMail(Long importId, Instant instant, Throwable throwable)

    /**
     * Return the timestamp when the scheduler creates the workflow
     *
     * @param importId id of FastqImportInstance for Fastq import or BamImportInstance for Bam import
     */
    abstract protected Instant getExecutionTimestamp(Long importId)
}
