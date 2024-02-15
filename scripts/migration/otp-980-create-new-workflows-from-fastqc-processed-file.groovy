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
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import static groovyx.gpars.GParsPool.withPool

/**
 * Creates new WorkflowRuns and WorkflowArtefacts based on the current FastqcProcessedFile data
 * @param BATCH_SIZE to process of fastqcProcessedFiles in trunks
 * @param DRY_RUN to test the processing without changes in DB
 *
 */

//////////////////////////////////////////////////////////////
// User input parameters

/**
 * Specifies how many seqTracks and related fastqcProcessedFiles to be processed together in one batch
 * depending upon the numbers of seqTracks and logical CPU cores available.
 * Since the number of fastqcProcessedFiles per seqTrack varies between 1 and 4, it is recommended
 * to choose a slightly bigger n as in otp-592 migration script.
 *
 * Recommended:  numOfSeqTracks / numLogicalCPUCores / n (some integer)
 */
int batchSize = 500

/**
 * Run this script w/o modification of database if set to true
 */
boolean dryRun = true

/**
 * Process priority in the new workflow
 */
String processPriority = 'NORMAL'

//////////////////////////////////////////////////////////////

assert batchSize > 1

@Field final String OUTPUT_ROLE = 'FastQC'
@Field final String WORKFLOW_NAME = BashFastQcWorkflow.WORKFLOW

@Field final RawSequenceDataViewFileService rawSequenceDataViewFileService = ctx.rawSequenceDataViewFileService

WorkflowService workflowService = ctx.workflowService

/*
 *main function to create WF runs and artefacts
 */

void migrateToNewWorkflow(
        List<SeqTrack> seqTracks,
        Workflow workflow,
        ProcessingPriority priority,
        String outputRole,
        RawSequenceDataViewFileService rawSequenceDataViewFileService
) {
    seqTracks.each { SeqTrack seqTrack ->
        // getting and prepare information
        String directory = rawSequenceDataViewFileService.getDirectoryPath(seqTrack.sequenceFiles.first()).parent
        WorkflowArtefact inputArtefact = seqTrack.workflowArtefact

        assert inputArtefact: "input artefact of ${seqTrack} can't be null. Was migration script otp-592 called before to create missing input artifacts?"

        String shortName = "${BashFastQcWorkflow.WORKFLOW}: ${seqTrack.individual.pid} " +
                "${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"
        List<String> runDisplayName = []
        runDisplayName.with {
            add("project: ${seqTrack.project.name}")
            add("individual: ${seqTrack.individual.displayName}")
            add("sampleType: ${seqTrack.sampleType.displayName}")
            add("seqType: ${seqTrack.seqType.displayNameWithLibraryLayout}")
            add("run: ${seqTrack.run.name}")
            add("lane: ${seqTrack.laneId}")
        }
        List<String> artefactDisplayName = runDisplayName
        artefactDisplayName.remove(0)

        // create workflow run and input artefact for SeqTrack
        WorkflowRun workflowRun = new WorkflowRun([
                workDirectory   : directory,
                state           : WorkflowRun.State.LEGACY,
                project         : seqTrack.project,
                combinedConfig  : '{}',
                priority        : priority,
                workflowSteps   : [],
                workflow        : workflow,
                displayName     : runDisplayName,
                shortDisplayName: shortName,
        ]).save()

        new WorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : BashFastQcWorkflow.INPUT_FASTQ,
                workflowArtefact: inputArtefact,
        ]).save()

        // create workflow artefact for each FastqcProcessedFile of seqTrack
        RawSequenceFile.findAllBySeqTrack(seqTrack).eachWithIndex { RawSequenceFile rawSequenceFile, int i ->
            WorkflowArtefact workflowArtefact = new WorkflowArtefact([
                    producedBy  : workflowRun,
                    state       : WorkflowArtefact.State.SUCCESS,
                    outputRole  : "${outputRole}_${i + 1}",
                    artefactType: ArtefactType.FASTQC,
                    individual  : seqTrack.individual,
                    seqType     : seqTrack.seqType,
                    displayName : artefactDisplayName,
            ])

            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllWhere(sequenceFile: rawSequenceFile))
            fastqcProcessedFile.workflowArtefact = workflowArtefact
            fastqcProcessedFile.save()
        }
    }
}
// =================================================

List<List<Long>> seqTrackIdsWithRawSequenceFileCount = SeqTrack.executeQuery(
        """SELECT s.id, COUNT(f.id) FROM SeqTrack s
                      INNER JOIN RawSequenceFile d ON d.seqTrack.id=s.id
                      INNER JOIN FastqcProcessedFile f ON f.sequenceFile.id=d.id
                      WHERE s.workflowArtefact IS NOT NUll AND f.workflowArtefact IS NULL
                      GROUP BY s.id 
                      ORDER BY s.id ASC""")

List<Long> seqTrackIds = seqTrackIdsWithRawSequenceFileCount.collect { it[0] } as List<Long>
int numFastqcProcessedFiles = seqTrackIds ? seqTrackIdsWithRawSequenceFileCount.collect { it[1] }.sum() as int : 0
int numSeqTracks = seqTrackIds.size()
println "There are ${numFastqcProcessedFiles} Fastqc Processed Files of ${numSeqTracks} Seq. Tracks to be migrated into new workflow system"

if (seqTrackIdsWithRawSequenceFileCount) {

    // process the SeqTracks in chunks
    long numBatches = Math.ceil(numSeqTracks / batchSize) as long
    println "${numBatches} batches will be processed"

    // fetch the FastQC Workflow
    Workflow workflow = workflowService.getExactlyOneWorkflow(WORKFLOW_NAME)
    assert workflow: "configured workflow ${WORKFLOW_NAME} does not exists"
    println "Migrate fastqcProcessedFiles to new workflow systems for Workflow \"${WORKFLOW_NAME}\""

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
                int adjustedSize = Math.min((numSeqTracks - start), batchSize) - 1
                List<SeqTrack> seqTracks = SeqTrack.findAllByIdInList(seqTrackIds[start..start + adjustedSize])
                migrateToNewWorkflow(seqTracks, workflow, priority, OUTPUT_ROLE, rawSequenceDataViewFileService)
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
