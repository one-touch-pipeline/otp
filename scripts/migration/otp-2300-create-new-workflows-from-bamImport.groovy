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
package migration

import groovy.transform.Field

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.bamImport.BamImportWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import static groovyx.gpars.GParsPool.withPool

/**
 * Creates new WorkflowRuns and WorkflowArtefacts based on the current ExternallyProcessedBamFile data
 * @param BATCH_SIZE to process of ExternallyProcessedBamFile in trunks
 * @param DRY_RUN to test the processing without changes in DB
 *
 */

//////////////////////////////////////////////////////////////
// User input parameters

/**
 * Specifies how many bam files to be processed together in one batch
 * depending upon the numbers of bam files and logical CPU cores available.
 *
 */
int batchSize = 100

/**
 * Run this script w/o modification of database if set to true
 */
boolean dryRun = true

/**
 * Process priority in the new workflow
 */
String processPriority = 'prod-prio3'

//////////////////////////////////////////////////////////////

assert batchSize > 1

@Field final String WORKFLOW_NAME = BamImportWorkflow.WORKFLOW

@Field final WorkflowService workflowService = ctx.workflowService

/*
 *main function to create WF runs and artefacts
 */

void migrateToNewWorkflow(
        List<ExternallyProcessedBamFile> bamFiles,
        Workflow workflow,
        ProcessingPriority priority
) {
    bamFiles.each { ExternallyProcessedBamFile bamFile ->
        String shortName = "${BamImportWorkflow.WORKFLOW}: ${bamFile.mergingWorkPackage.individual.pid} " +
                "${bamFile.mergingWorkPackage.sampleType.displayName} ${bamFile.mergingWorkPackage.seqType.displayNameWithLibraryLayout}"
        List<String> runDisplayName = []
        runDisplayName.with {
            add("project: ${bamFile.mergingWorkPackage.project.name}")
            add("individual: ${bamFile.mergingWorkPackage.individual.displayName}")
            add("sampleType: ${bamFile.mergingWorkPackage.sampleType.displayName}")
            add("seqType: ${bamFile.mergingWorkPackage.seqType.displayNameWithLibraryLayout}")
            add("referenceGenome: ${bamFile.mergingWorkPackage.referenceGenome.name}")
            add("libraryPreparationKit: ${bamFile.mergingWorkPackage.libraryPreparationKit}")
        }
        List<String> artefactDisplayName = runDisplayName
        artefactDisplayName.remove(0)

        // create workflow run and input artefact for current bam file
        WorkflowRun workflowRun = new WorkflowRun([
                workDirectory   : "",
                state           : WorkflowRun.State.LEGACY,
                project         : bamFile.mergingWorkPackage.project,
                combinedConfig  : '{}',
                priority        : priority,
                workflowSteps   : [],
                workflow        : workflow,
                displayName     : runDisplayName,
                shortDisplayName: shortName,
        ]).save(flush: false)

        WorkflowArtefactValues values = new WorkflowArtefactValues(
                workflowRun, BamImportWorkflow.OUTPUT_BAM, ArtefactType.BAM, artefactDisplayName
        )

        bamFile.workflowArtefact = new WorkflowArtefact([
                producedBy      : values.run,
                outputRole      : values.role,
                withdrawnDate   : null,
                withdrawnComment: null,
                state           : WorkflowArtefact.State.SUCCESS,
                artefactType    : values.artefactType,
                displayName     : StringUtils.generateMultiLineDisplayName(values.displayNameLines),
        ]).save(flush: false)

        bamFile.save(flush: false)
    }
}
// =================================================

List<Long> bamFileIds = ExternallyProcessedBamFile.executeQuery(
        """SELECT s.id FROM ExternallyProcessedBamFile s
                      WHERE s.workflowArtefact IS NULL
                      ORDER BY s.id ASC""")

int numBamFiles = bamFileIds.size()
println "There are ${numBamFiles} bam files to be migrated into new workflow system"

if (bamFileIds) {

    // process the bam files in chunks
    long numBatches = Math.ceil(numBamFiles / batchSize) as long
    println "${numBatches} batches will be processed"

    // fetch the BamImport Workflow
    Workflow workflow = workflowService.getExactlyOneWorkflow(WORKFLOW_NAME)
    assert workflow: "configured workflow ${WORKFLOW_NAME} does not exists"
    println "Migrate bam files to new workflow systems for Workflow \"${WORKFLOW_NAME}\""

    // prepare batch for GPars pool
    List<Integer> loop = []
    0.upto(numBatches - 1) {
        loop += it
    }

    int numCores = Runtime.runtime.availableProcessors()
    println "${numCores} logical CPU core(s) are available"

    // fetch the priority from database
    ProcessingPriority priority = CollectionUtils.exactlyOneElement(ProcessingPriority.findAllByName(processPriority),
            "Processing priority ${processPriority} doesnt exist.")

    dryRun && println("dry run, nothing is saved")
    print "Processing: "
    withPool(numCores, {
        // loop through each batch and process it
        loop.makeConcurrent().each {
            SessionUtils.withNewTransaction { session ->
                // start the migration
                int start = batchSize * it
                int adjustedSize = Math.min((numBamFiles - start), batchSize) - 1
                List<ExternallyProcessedBamFile> bamFiles = ExternallyProcessedBamFile.findAllByIdInList(bamFileIds[start..start + adjustedSize])
                migrateToNewWorkflow(bamFiles, workflow, priority)
                // flush changes to the database
                if (!dryRun) {
                    session.flush()
                }
                print('.')
            }
        }
    })
    println " finished"
} else {
    println "nothing to do!"
}
