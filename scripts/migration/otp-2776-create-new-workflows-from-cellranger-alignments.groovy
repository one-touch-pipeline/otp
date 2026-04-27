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
package migration

import groovy.transform.Field
import org.hibernate.SessionFactory

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.cellRanger.CellRangerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

import static groovyx.gpars.GParsPool.withPool

/**
 * Creates new WorkflowRuns and WorkflowArtefacts based on the current SingleCellBamFiles for CellRanger.
 * @param batchSize number of individuals with CellRanger BAM files to be processed together in one batch
 * @param dryRun if true, runs without any database modifications
 */

//////////////////////////////////////////////////////////////
// User input parameters

/**
 * Specifies how many individuals with CellRanger BAM files to be processed together in one batch
 * depending upon the numbers of seqTracks and logical CPU cores available.
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

assert batchSize >= 1

@Field final String WORKFLOW_NAME = CellRangerWorkflow.WORKFLOW

@Field final CellRangerWorkFileService cellRangerWorkFileService = ctx.cellRangerWorkFileService
@Field final WorkflowService workflowService = ctx.workflowService
@Field final SessionFactory sessionFactory = ctx.sessionFactory

FileSystemService fileSystemService = ctx.fileSystemService

@Field AtomicInteger errorCounter = new AtomicInteger()

@Field final String queryIndividualsToMigrate = """
SELECT i.id, count(scb.id)
FROM SingleCellBamFile scb
JOIN scb.workPackage mwp
JOIN mwp.sample.individual i
WHERE scb.workflowArtefact IS NULL
GROUP BY i.id
ORDER BY i.id ASC
"""

@Field final String querySingleCellBamFilesToMigrate = """
SELECT DISTINCT scb FROM SingleCellBamFile scb
JOIN scb.workPackage mwp
JOIN mwp.sample.individual i
JOIN FETCH scb.seqTracks
WHERE scb.workflowArtefact IS NULL
AND i.id in (:individualIds)
ORDER BY scb.id ASC
"""

/*
 * main function to create WF runs and artefacts
 */
void migrateToNewWorkflow(List<Long> individualIds, Workflow workflow, ProcessingPriority priority) {
    List<SingleCellBamFile> singleCellBamFiles = SingleCellBamFile.executeQuery(
            querySingleCellBamFilesToMigrate, [individualIds: individualIds]
    )

    singleCellBamFiles.each { SingleCellBamFile singleCellBamFile ->
        // getting and prepare information
        // get the legacy folder of viewByPid, which are not yet migrated to the UUID filesystem structure
        String directory = cellRangerWorkFileService.getBaseDirectory(singleCellBamFile).resolve(singleCellBamFile.workDirectoryName)
        List<SeqTrack> seqTracks = singleCellBamFile.seqTracks.sort {
            it.id
        }

        // checking, that workflowArtefact is available
        seqTracks.each {
            assert it.workflowArtefact: "input artefact of ${it} can't be null. Was migration script otp-592 called before to create missing input artifacts?"
        }

        // prepare names
        String shortName = [
                WORKFLOW_NAME,
                singleCellBamFile.individual.pid,
                singleCellBamFile.sampleType.displayName,
                singleCellBamFile.seqType.displayNameWithLibraryLayout,
        ].join(' ')

        List<String> runDisplayName = [
                "project: ${singleCellBamFile.project.name}",
                "individual: ${singleCellBamFile.individual.displayName}",
                "sampleType: ${singleCellBamFile.sampleType.displayName}",
                "seqType: ${singleCellBamFile.seqType.displayNameWithLibraryLayout}",
        ]

        List<String> artefactDisplayName = runDisplayName.clone()
        artefactDisplayName.remove(0)

        // create workflow run and input artefacts for SeqTracks
        WorkflowRun workflowRun = new WorkflowRun([
                workDirectory   : directory,
                state           : WorkflowRun.State.LEGACY,
                project         : singleCellBamFile.project,
                combinedConfig  : '{}',
                priority        : priority,
                workflowSteps   : [],
                workflow        : workflow,
                displayName     : runDisplayName.join(', '),
                shortDisplayName: shortName,
        ]).save(flush: false)

        seqTracks.eachWithIndex { SeqTrack seqTrack, int i ->
            new WorkflowRunInputArtefact([
                    workflowRun     : workflowRun,
                    role            : "${AlignmentWorkflow.INPUT_FASTQ}_${i}",
                    workflowArtefact: seqTrack.workflowArtefact,
            ]).save(flush: false)
        }

        WorkflowArtefact workflowArtefact = new WorkflowArtefact([
                producedBy      : workflowRun,
                state           : WorkflowArtefact.State.SUCCESS,
                outputRole      : AlignmentWorkflow.OUTPUT_BAM,
                artefactType    : ArtefactType.BAM,
                displayName     : artefactDisplayName.join(', '),
                // Mark the artefact as withdrawn if the SingleCellBamFile is marked as withdrawn, to avoid that it is used for further processing.
                // The withdrawnDate is set to now, because we don't know when the SingleCellBamFile was marked as withdrawn.
                withdrawnDate   : singleCellBamFile.withdrawn ? LocalDate.now() : null,
                withdrawnComment: singleCellBamFile.withdrawn ? "Migrated from withdrawn SingleCellBamFile (id: ${singleCellBamFile.id})" : null,
        ]).save(flush: false)

        singleCellBamFile.workflowArtefact = workflowArtefact
        singleCellBamFile.save(flush: false)
    }
}
// =================================================

fileSystemService.getRemoteFileSystem()

List<List<Long>> individualsIdsWithSingleCellBamFileCount = SingleCellBamFile.executeQuery(
        queryIndividualsToMigrate)

List<Long> individualIds = individualsIdsWithSingleCellBamFileCount.collect { it[0] } as List<Long>
List<List<Long>> listOfListOfIndividuals = individualIds.collate(batchSize)
int numBamFiles = individualIds ? individualsIdsWithSingleCellBamFileCount.collect { it[1] }.sum() as int : 0
int numIndividuals = individualIds.size()
println "There are ${numBamFiles} CellRanger BAM Files of ${numIndividuals} Individuals to be migrated into new workflow system"

if (individualsIdsWithSingleCellBamFileCount) {
    // process in chunks
    long numBatches = listOfListOfIndividuals.size()
    println "${numBatches} batches will be processed"

    // fetch the CellRanger Workflow
    Workflow workflow = workflowService.getExactlyOneWorkflow(WORKFLOW_NAME)
    assert workflow: "configured workflow ${WORKFLOW_NAME} does not exist"
    println "Migrate CellRanger BAM files to new workflow system for Workflow \"${WORKFLOW_NAME}\""

    int numCores = Runtime.runtime.availableProcessors()
    println "${numCores} logical CPU core(s) are available"

    // fetch the priority from database
    ProcessingPriority priority = CollectionUtils.exactlyOneElement(ProcessingPriority.findAllByName(processPriority),
            "Processing priority ${processPriority} doesnt exist.")

    dryRun && println("dry run, nothing is saved")
    print "Processing: "

    try {
        withPool(numCores, {
            // loop through each batch and process it
            listOfListOfIndividuals.makeConcurrent().each { List<Long> partIndividualIds ->
                try {
                    SessionUtils.withNewTransaction {
                        // start the migration
                        migrateToNewWorkflow(partIndividualIds, Workflow.get(workflow.id), ProcessingPriority.get(priority.id))
                        if (dryRun) {
                            sessionFactory.currentSession.clear() // clear the session to avoid any accidental flush of changes to the database
                        } else {
                            sessionFactory.currentSession.flush() // flush changes to the database
                        }
                        print('.')
                    }
                } catch (Throwable t) {
                    println StackTraceUtils.getStackTrace(t)
                    errorCounter.incrementAndGet()
                }
            }
        })
    } finally {
        if (errorCounter.get()) {
            println "\nThere occurred: ${errorCounter.get()} errors"
        }
    }
    println " finished"
} else {
    println "nothing to do!"
}
