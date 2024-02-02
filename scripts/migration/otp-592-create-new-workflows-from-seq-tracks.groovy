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

import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.TransactionUtils
import de.dkfz.tbi.otp.workflowExecution.*

import static groovyx.gpars.GParsPool.withPool

/**
 * Creates new WorkflowRuns and WorkflowArtefacts based on the current SeqTrack data
 * @param BATCH_SIZE to process of seqTracks in trunks
 * @param DRY_RUN to test the processing without changes in DB
 *
 */

//////////////////////////////////////////////////////////////
// User input parameters

/**
 * Specifies how many seqTracks to be processed together in one batch
 * depending upon the numbers of seqTracks and CPU cores available
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

@Field final String OUTPUT_ROLE = 'FASTQ'
@Field final String WORKFLOW_NAME = 'FASTQ installation'

@Field final LsdfFilesService lsdfFilesService = ctx.lsdfFilesService

@Field final List errorlist = [].asSynchronized()

WorkflowService workflowService = ctx.workflowService

/*
 *main function to create WF runs and artefacts
 */

void migrateToNewWorkflow(List<SeqTrack> seqTracks, Workflow workflow, ProcessingPriority priority, String outputRole, LsdfFilesService lsdfFilesService) {
    seqTracks.each { SeqTrack seqTrack ->
        if (!seqTrack.workflowArtefact) { // only process if the workflowArtefact is null
            if (!seqTrack.validate()) {
                errorlist << seqTrack.id
                return
            }

            // prepare attributes needed for the WF runs
            String directory = lsdfFilesService.getFileViewByPidPathAsPath(seqTrack.sequenceFiles.first()).parent
            String shortName = "DI: ${seqTrack.individual.pid} ${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"
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

            // create the WF run
            Map runParam = [
                    workDirectory   : directory,
                    state           : WorkflowRun.State.LEGACY,
                    project         : seqTrack.project,
                    combinedConfig  : '{}',
                    priority        : priority,
                    workflowSteps   : [],
                    workflow        : workflow,
                    displayName     : runDisplayName,
                    shortDisplayName: shortName,
            ]
            WorkflowRun run = new WorkflowRun(runParam).save()

            // create the WF artefact
            Map artefactParam = [
                    producedBy  : run,
                    state       : WorkflowArtefact.State.SUCCESS,
                    outputRole  : outputRole,
                    artefactType: ArtefactType.FASTQ,
                    individual  : seqTrack.individual,
                    seqType     : seqTrack.seqType,
                    displayName : artefactDisplayName,
            ]
            WorkflowArtefact artefact = new WorkflowArtefact(artefactParam).save()

            // assign the foreign key to artefact
            seqTrack.workflowArtefact = artefact
            seqTrack.save()
        }
    }
}

// =================================================

List<Long> seqTrackIds = SeqTrack.withCriteria {
    isNull("workflowArtefact")
    order("id", "asc")
    projections {
        property("id")
    }
} as List<Long>

int numSeqTracks = seqTrackIds.size()

println "There are ${numSeqTracks} Seq. Tracks to be migrated into new workflow system"

if (numSeqTracks != 0) {

    // process the SeqTracks in chunks
    long numBatches = Math.ceil(numSeqTracks / batchSize) as long
    println "${numBatches} batches will be processed"

    // fetch the FastQ Installation Workflow
    Workflow workflow = workflowService.getExactlyOneWorkflow(WORKFLOW_NAME)
    assert workflow
    println "Migrate seqTracks to new workflow systems for Workflow \"${WORKFLOW_NAME}\""

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
        // loop thru each batch and process it
        loop.makeConcurrent().each {
            TransactionUtils.withNewTransaction { session ->
                int start = batchSize * it
                int adjustedSize = Math.min((numSeqTracks - start), batchSize) - 1
                List<SeqTrack> seqTracks = SeqTrack.findAllByIdInList(seqTrackIds[start..start + adjustedSize])
                migrateToNewWorkflow(seqTracks, workflow, priority, OUTPUT_ROLE, lsdfFilesService)

                // flush changes to the database
                if (!dryRun) {
                    session.flush()
                }
                print('.')
            }
        }
    })
    println " finished"

    if (errorlist) {
        println "\n\ninvalid seqtracks ids (${errorlist.size()} of ${numSeqTracks}):\n${errorlist}"
    }
} else {
    println "nothing to do!"
}
