/*
 * Copyright 2011-2021 The OTP authors
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
import de.dkfz.tbi.otp.utils.TransactionUtils
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths

import static groovyx.gpars.GParsPool.withPool

/**
 * Creates new WorkflowRuns and WorkflowArtefacts based on the current SeqTrack data
 * @param BATCH_SIZE to process of seqTracks in trunks
 * @param DRY_RUN to test the processing without changes in DB
 *
 */

//////////////////////////////////////////////////////////////
//User input parameters

/**
 * Specifies how many seqTracks to be processed together in one batch
 * depending upon the numbers of seqTracks and CPU cores available
 * Recommended:  numOfSeqTracks / numCPUCores / n (some integer)
 */
int batchSize  = 500

/**
 * Run this script w/o modification of database if set to true
 */
boolean dryRun = true

/**
 * Process priority in the new workflow
 */
String  processPriority  = 'NORMAL'

//////////////////////////////////////////////////////////////

assert batchSize > 1

@Field final String OUTPUT_ROLE   = 'FASTQ'
@Field final String WORKFLOW_NAME = 'FASTQ installation'

@Field final LsdfFilesService lsdfFilesService = ctx.lsdfFilesService

/*
 *main function to create WF runs and artefacts
 */
void migrateToNewWorkflow(List<SeqTrack> seqTracks, Workflow workflow, ProcessingPriority priority, String outputRole, LsdfFilesService lsdfFilesService) {

    seqTracks.each { SeqTrack seqTrack ->
        if (!seqTrack.workflowArtefact) { // only process if the workflowArtefact is null
            //prepare attributes needed for the WF runs
            String directory = Paths.get(lsdfFilesService.getFileViewByPidPath(seqTrack.dataFiles.first())).parent
            String name = "${seqTrack.project.name} ${seqTrack.individual.displayName} ${seqTrack.sampleType.displayName} " +
                    "${seqTrack.seqType.displayNameWithLibraryLayout} lane ${seqTrack.laneId} run ${seqTrack.run.name}"

            //create the WF run
            Map runParam = [
                    workDirectory : directory,
                    state         : WorkflowRun.State.LEGACY,
                    project       : seqTrack.project,
                    combinedConfig: '{}',
                    priority      : priority,
                    workflowSteps : [],
                    workflow      : workflow,
                    displayName   : name,
            ]
            WorkflowRun run = new WorkflowRun(runParam).save()

            //create the WF artefact
            Map artefactParam = [
                    producedBy    : run,
                    state         : WorkflowArtefact.State.LEGACY,
                    outputRole    : outputRole,
                    artefactType  : ArtefactType.FASTQC,
                    individual    : seqTrack.individual,
                    seqType       : seqTrack.seqType,
                    displayName   : name,
            ]
            WorkflowArtefact artefact = new WorkflowArtefact(artefactParam).save()

            //assign the foreign key to artefact
            seqTrack.workflowArtefact = artefact
            seqTrack.save()
        }
    }
}

//=================================================

int numSeqTracks = SeqTrack.count
println "There are ${numSeqTracks} Seq Tracks to be migrated into New workflow"

//process the SeqTracks in chunks
long numBatches = Math.ceil(numSeqTracks / batchSize)
println "${numBatches} batches will be processed"

//fetch the FastQ Installation Workflow
Workflow workflow = Workflow.getExactlyOneWorkflow(WORKFLOW_NAME)
assert workflow
println "Migrate seqTracks to new workflow systems for Workflow \"${WORKFLOW_NAME}\""

//prepare batch for GPars pool
List<Integer> loop = []
0.upto(numBatches - 1) {
    loop += it
}

int numCores = Runtime.getRuntime().availableProcessors()
println "${numCores} CPU core(s) are available"

//fetch the priority from database
ProcessingPriority priority = ProcessingPriority.findByName(processPriority)

print "Processing: "
withPool(numCores, {
    //loop thru each batch and process it
    loop.makeConcurrent().each {
        TransactionUtils.withNewTransaction { session ->
            //start the migration
            List<SeqTrack> seqTracks = SeqTrack.listOrderById(max: batchSize, offset: batchSize * it)
            migrateToNewWorkflow(seqTracks, workflow, priority, OUTPUT_ROLE, lsdfFilesService)

            //flush changes to the database
            if(!dryRun) {
                session.flush()
            }

            print('.')
        }
    }
})
println " finished"
