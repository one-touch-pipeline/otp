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

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.util.concurrent.atomic.AtomicInteger

import static groovyx.gpars.GParsPool.withPool

/**
 * Creates new WorkflowRuns and WorkflowArtefacts based on the current RoddyBamFiles for WES, WGS and ChipSeq
 * @param BATCH_SIZE to process of fastqcProcessedFiles in trunks
 * @param DRY_RUN to test the processing without changes in DB
 *
 */

//////////////////////////////////////////////////////////////
// User input parameters

/**
 * Specifies how many individuals with wes/wgs/chipseq bam files to be processed together in one batch
 * depending upon the numbers of seqTracks and logical CPU cores available.
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

@Field final String WORKFLOW_NAME = PanCancerWorkflow.WORKFLOW

@Field final RoddyBamFileService roddyBamFileService = ctx.roddyBamFileService
@Field final WorkflowService workflowService = ctx.workflowService

FileSystemService fileSystemService = ctx.fileSystemService

@Field String seqTypeIdsQueryString = [
        SeqTypeService.wholeGenomePairedSeqType,
        SeqTypeService.exomePairedSeqType,
        SeqTypeService.chipSeqPairedSeqType,
]*.id.join(', ')

@Field AtomicInteger errorCounter = new AtomicInteger()

@Field final String queryIndividualsToMigrate = """
SELECT i.id, count(r.id) 
FROM RoddyBamFile r
JOIN r.workPackage mwp
JOIN mwp.sample.individual i
WHERE r.workflowArtefact IS NUll
AND mwp.seqType.id in (${seqTypeIdsQueryString})
GROUP BY i.id
ORDER BY i.id ASC
"""

@Field final String queryRoddyFilesToMigrate = """
SELECT r FROM RoddyBamFile r
JOIN r.workPackage mwp
JOIN mwp.sample.individual i
JOIN FETCH r.seqTracks
WHERE r.workflowArtefact IS NUll
AND mwp.seqType.id in (${seqTypeIdsQueryString})
AND i.id in (:individualIds)
ORDER BY r.id ASC
"""

@Field final String querySeqTrackAndFastqc = """
SELECT s, f FROM FastqcProcessedFile f
JOIN f.sequenceFile d
JOIN d.seqTrack s
JOIN s.sample.individual i
WHERE d.indexFile = false
AND s.seqType.id in (${seqTypeIdsQueryString})
AND i.id in (:individualIds)
"""

/*
 * main function to create WF runs and artefacts
 */

void migrateToNewWorkflow(
        List<Long> individualIds,
        Workflow workflow,
        ProcessingPriority priority
) {

    List<RoddyBamFile> roddyBamFiles = RoddyBamFile.executeQuery(
            queryRoddyFilesToMigrate, [individualIds: individualIds]
    )

    Map<SeqTrack, List<FastqcProcessedFile>> fastqcPerSeqTrack = FastqcProcessedFile.executeQuery(
            querySeqTrackAndFastqc, [individualIds: individualIds]
    ).groupBy {
        it[0]
    }.collectEntries {
        [(it.key): it.value.collect { it[1] }]
    }

    Map<Long, WorkflowArtefact> workflowArtefactMap = [:]

    roddyBamFiles.each { RoddyBamFile roddyBamFile ->
        // getting and prepare information
        String directory = roddyBamFile.oldStructureUsed ? roddyBamFileService.getBaseDirectory(roddyBamFile) : roddyBamFileService.getWorkDirectory(roddyBamFile)
        List<SeqTrack> seqTracks = roddyBamFile.seqTracks.sort {
            it.id
        }

        List<FastqcProcessedFile> fastqcProcessedFiles = seqTracks.collectMany {
            List<FastqcProcessedFile> fastqcProcessedFiles = fastqcPerSeqTrack[it]
            assert fastqcProcessedFiles && fastqcProcessedFiles.size() == 2
            fastqcProcessedFiles
        }.sort {
            it.id
        }

        // checking, that workflowArtefact is available
        seqTracks.every {
            assert it.workflowArtefact: "input artefact of ${it} can't be null. Was migration script otp-592 called before to create missing input artifacts?"
        }

        fastqcProcessedFiles.every {
            assert it.workflowArtefact: "input artefact of ${it} can't be null. Was migration script otp-980 called before to create missing input artifacts?"
        }
        assert fastqcProcessedFiles.size() == seqTracks.size() * 2

        // prepare names
        String shortName = [
                PanCancerWorkflow.WORKFLOW,
                roddyBamFile.individual.pid,
                roddyBamFile.sampleType.displayName,
                roddyBamFile.seqType.displayNameWithLibraryLayout,
        ].join(' ')

        List<String> runDisplayName = [
                "project: ${roddyBamFile.project.name}",
                "individual: ${roddyBamFile.individual.displayName}",
                "sampleType: ${roddyBamFile.sampleType.displayName}",
                "seqType: ${roddyBamFile.seqType.displayNameWithLibraryLayout}",
        ]

        List<String> artefactDisplayName = runDisplayName.clone()
        artefactDisplayName.remove(0)

        // create workflow run and input artefact for SeqTrack
        WorkflowRun workflowRun = new WorkflowRun([
                workDirectory   : directory,
                state           : WorkflowRun.State.LEGACY,
                project         : roddyBamFile.project,
                combinedConfig  : '{}',
                priority        : priority,
                workflowSteps   : [],
                workflow        : workflow,
                displayName     : runDisplayName,
                shortDisplayName: shortName,
        ]).save(flush: false)

        seqTracks.eachWithIndex { SeqTrack seqTrack, int i ->
            new WorkflowRunInputArtefact([
                    workflowRun     : workflowRun,
                    role            : "${PanCancerWorkflow.INPUT_FASTQ}_${i}",
                    workflowArtefact: seqTrack.workflowArtefact,
            ]).save(flush: false)
        }

        fastqcProcessedFiles.eachWithIndex { FastqcProcessedFile fastqcProcessedFile, int i ->
            new WorkflowRunInputArtefact([
                    workflowRun     : workflowRun,
                    role            : "${PanCancerWorkflow.INPUT_FASTQC}_${i}",
                    workflowArtefact: fastqcProcessedFile.workflowArtefact,
            ]).save(flush: false)
        }

        WorkflowArtefact workflowArtefact = new WorkflowArtefact([
                producedBy  : workflowRun,
                state       : WorkflowArtefact.State.SUCCESS,
                outputRole  : PanCancerWorkflow.OUTPUT_BAM,
                artefactType: ArtefactType.BAM,
                displayName : artefactDisplayName,
        ])

        roddyBamFile.workflowArtefact = workflowArtefact
        roddyBamFile.save(flush: false)

        workflowArtefactMap[roddyBamFile] = workflowArtefact
    }
}
// =================================================

fileSystemService.getRemoteFileSystem()

List<List<Long>> individualsIdsWithRoddyBamFileCount = SeqTrack.executeQuery(
        queryIndividualsToMigrate)

List<Long> individualIds = individualsIdsWithRoddyBamFileCount.collect { it[0] } as List<Long>
List<List<Long>> listOfListOfIndividuals = individualIds.collate(batchSize)
int numRoddyFiles = individualIds ? individualsIdsWithRoddyBamFileCount.collect { it[1] }.sum() as int : 0
int numIndividuals = individualIds.size()
println "There are ${numRoddyFiles} PanCancer Roddy Files of ${numIndividuals} Individuals to be migrated into new workflow system"

if (individualsIdsWithRoddyBamFileCount) {

    // process the SeqTracks in chunks
    long numBatches = listOfListOfIndividuals.size()
    println "${numBatches} batches will be processed"

    // fetch the FastQC Workflow
    Workflow workflow = workflowService.getExactlyOneWorkflow(WORKFLOW_NAME)
    assert workflow: "configured workflow ${WORKFLOW_NAME} does not exists"
    println "Migrate PanCancer bam files to new workflow systems for Workflow \"${WORKFLOW_NAME}\""

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
                    TransactionUtils.withNewTransaction { session ->
                        // start the migration
                        migrateToNewWorkflow(partIndividualIds, workflow, priority)
                        // flush changes to the database
                        if (!dryRun) {
                            session.flush()
                        }
                        print('.')
                    }
                } catch (Throwable t) {
                    println StackTraceUtils.getStackTrace(t)
                    errorCounter.incrementAndGet()
                    throw t
                }
            }
        })
    } finally {
        if (errorCounter.get()) {
            println "\nThere occured: ${errorCounter.get()} errors"
        }
    }
    println " finished"
} else {
    println "nothing to do!"
}
