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
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import static groovyx.gpars.GParsPool.withPool
/**
 * Creates Workflows artefacts for fastqc of index datafile.
 *
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

//////////////////////////////////////////////////////////////

assert batchSize > 1

@Field final String OUTPUT_ROLE = 'FastQC'
@Field final String WORKFLOW_NAME = BashFastQcWorkflow.WORKFLOW

WorkflowService workflowService = ctx.workflowService

/*
 *main function to create WF runs and artefacts
 */

void migrateToNewWorkflow(
        List<SeqTrack> seqTracks,
        String outputRole
) {
    seqTracks.each { SeqTrack seqTrack ->
        // getting and prepare information
        WorkflowArtefact inputArtefact = seqTrack.workflowArtefact

        assert inputArtefact: "input artefact of ${seqTrack} can't be null. Was migration script otp-592 called before to create missing input artifacts?"

        List<DataFile> readDataFiles = DataFile.findAllBySeqTrackAndIndexFile(seqTrack, false)
        if (!readDataFiles) {
            println("No read data files for seqTrack: ${seqTrack}")
            return
        }
        FastqcProcessedFile readFastqcProcessedFile = CollectionUtils.exactlyOneElement(FastqcProcessedFile.findAllByDataFile(readDataFiles.first()))
        WorkflowArtefact readWorkflowArtefact = readFastqcProcessedFile.workflowArtefact

        // create workflow artefact for each indexed FastqcProcessedFile of seqTrack
        DataFile.findAllBySeqTrackAndIndexFile(seqTrack, true).eachWithIndex { DataFile dataFile, int i ->
            WorkflowArtefact workflowArtefact = new WorkflowArtefact([
                    producedBy  : readWorkflowArtefact.producedBy,
                    state       : WorkflowArtefact.State.SUCCESS,
                    outputRole  : "${outputRole}_i${i + 1}",
                    artefactType: ArtefactType.FASTQC,
                    individual  : seqTrack.individual,
                    seqType     : seqTrack.seqType,
                    displayName : readWorkflowArtefact.displayName,
            ])

            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllWhere(dataFile: dataFile))
            fastqcProcessedFile.workflowArtefact = workflowArtefact
            fastqcProcessedFile.save()
        }
    }
}
// =================================================

List<List<Long>> seqTrackIdsWithDataFileCount = SeqTrack.executeQuery(
        """SELECT s.id, COUNT(f.id) FROM SeqTrack s
                      INNER JOIN DataFile d ON d.seqTrack.id=s.id
                      INNER JOIN FastqcProcessedFile f ON f.dataFile.id=d.id
                      WHERE s.workflowArtefact IS NOT NUll AND d.indexFile = true AND f.workflowArtefact IS NULL
                      GROUP BY s.id 
                      ORDER BY s.id ASC""")

List<Long> seqTrackIds = seqTrackIdsWithDataFileCount.collect { it[0] } as List<Long>
int numFastqcProcessedFiles = seqTrackIds ? seqTrackIdsWithDataFileCount.collect { it[1] }.sum() as int : 0
int numSeqTracks = seqTrackIds.size()
println "There are ${numFastqcProcessedFiles} Fastqc Processed Files of indexed fast files of ${numSeqTracks} Seq. Tracks to be migrated into new workflow system"

if (seqTrackIdsWithDataFileCount) {

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
                migrateToNewWorkflow(seqTracks, OUTPUT_ROLE)
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
