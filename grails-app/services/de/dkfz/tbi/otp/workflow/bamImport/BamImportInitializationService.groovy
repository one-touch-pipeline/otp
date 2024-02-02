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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.transaction.TransactionStatus

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.BamImportInstance
import de.dkfz.tbi.otp.workflowExecution.*

@Slf4j
@Transactional
class BamImportInitializationService {

    WorkflowArtefactService workflowArtefactService
    WorkflowRunService workflowRunService
    WorkflowService workflowService

    List<WorkflowRun> createWorkflowRuns(BamImportInstance instance, ProcessingPriority priority = null) {
        Workflow workflow = workflowService.getExactlyOneWorkflow(BamImportWorkflow.WORKFLOW)
        List<WorkflowRun> workflowRuns = instance.externallyProcessedBamFiles.collect { ExternallyProcessedBamFile bamFile ->
            createRunForBamFile(workflow, bamFile, priority)
        }

        WorkflowRun.withTransaction { TransactionStatus status ->
            status.flush()
        }

        return workflowRuns
    }

    private WorkflowRun createRunForBamFile(Workflow workflow, ExternallyProcessedBamFile bamFile, ProcessingPriority priority) {
        List<String> runDisplayName = [
                "project: ${bamFile.mergingWorkPackage.project.name}",
                "individual: ${bamFile.mergingWorkPackage.individual.displayName}",
                "sampleType: ${bamFile.mergingWorkPackage.sampleType.displayName}",
                "seqType: ${bamFile.mergingWorkPackage.seqType.displayNameWithLibraryLayout}",
                "referenceGenome: ${bamFile.mergingWorkPackage.referenceGenome.name}",
                "libraryPreparationKit: ${bamFile.mergingWorkPackage.libraryPreparationKit}",
        ]*.toString()

        List<String> artefactDisplayName = new ArrayList<>(runDisplayName)
        artefactDisplayName.remove(0)

        String shortName = "BamImport: ${bamFile.mergingWorkPackage.individual.pid} ${bamFile.mergingWorkPackage.sampleType.displayName}" +
                "${bamFile.mergingWorkPackage.seqType.displayNameWithLibraryLayout}"

        ProcessingPriority pp = priority ?: bamFile.mergingWorkPackage.project.processingPriority
        WorkflowRun run = workflowRunService.buildWorkflowRun(workflow, pp, "", bamFile.mergingWorkPackage.project, runDisplayName, shortName)
        WorkflowArtefact artefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                run, BamImportWorkflow.OUTPUT_BAM, ArtefactType.BAM, artefactDisplayName
        ))

        bamFile.workflowArtefact = artefact
        bamFile.save(flush: false)

        return run
    }
}
