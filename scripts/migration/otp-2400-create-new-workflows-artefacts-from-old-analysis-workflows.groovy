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
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.analysis.indel.IndelWorkflow
import de.dkfz.tbi.otp.workflow.analysis.runyapsa.RunYapsaWorkflow
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.util.concurrent.atomic.AtomicInteger

import static groovyx.gpars.GParsPool.withPool

/**
 * Creates new WorkflowRuns, WorkflowInputArtefacts and WorkflowArtefacts based on the current analysis data
 * It migrates a single analysis workflow each time
 * It can be run multiple times to migrate all analysis workflows
 *
 * Note:
 * Migrate Sophia first before migrating Aceseq
 * Migrate Snv first before migrating RunYapsa
 *
 * @param analysisToMigrate to specify the analysis workflow to migrate
 * @param BATCH_SIZE to process of SNVBamFile in trunks
 * @param DRY_RUN to test the processing without changes in DB
 *
 */

@TupleConstructor
enum AnalysisWorkflow {
    ACESEQ(AceseqWorkflow.WORKFLOW),
    INDEL(IndelWorkflow.WORKFLOW),
    RUNYAPSA(RunYapsaWorkflow.WORKFLOW),
    SNV(SnvWorkflow.WORKFLOW),
    SOPHIA(SophiaWorkflow.WORKFLOW)
    String name
}

//////////////////////////////////////////////////////////////
// User input parameters
/**
 * Specifies what analysis workflow will be migrated. There are 5 options available:
 *
 * AnalysisWorkflow.ACESEQ.name
 * AnalysisWorkflow.INDEL.name
 * AnalysisWorkflow.RUNYAPSA.name
 * AnalysisWorkflow.SNV.name
 * AnalysisWorkflow.SOPHIA.name
 */
@Field final String analysisToMigrate = AnalysisWorkflow.SNV.name

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

final Map<String, String> workflowClassMap = [
        (AceseqWorkflow.WORKFLOW)  : "de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance",
        (SophiaWorkflow.WORKFLOW)  : "de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance",
        (RunYapsaWorkflow.WORKFLOW): "de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance",
        (SnvWorkflow.WORKFLOW)     : "de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance",
        (IndelWorkflow.WORKFLOW)   : "de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance",
].asImmutable()

final class AnalysisQueryResult {
    BamFilePairAnalysis analysis
    AbstractBamFile bamFileDisease
    AbstractBamFile bamFileControl
}

@Field final Map<String, ArtefactType> artefactTypeMap = [
        (AceseqWorkflow.WORKFLOW)  : ArtefactType.ACESEQ,
        (SophiaWorkflow.WORKFLOW)  : ArtefactType.SOPHIA,
        (RunYapsaWorkflow.WORKFLOW): ArtefactType.RUN_YAPSA,
        (SnvWorkflow.WORKFLOW)     : ArtefactType.SNV,
        (IndelWorkflow.WORKFLOW)   : ArtefactType.INDEL,
].asImmutable()

@Field final WorkflowService workflowService = ctx.workflowService

@Field AtomicInteger errorCounter = new AtomicInteger()

/**
 * Queries to fetch the IDs of all migrated workflow instances from the database
 * @param analysisClass to specify the analysis class to migrate
 */
@Field final String queryAnalysesToMigrate = """
SELECT bpa.id 
FROM BamFilePairAnalysis bpa
WHERE bpa.class = :analysisClass 
AND bpa.workflowArtefact IS NUll
AND bpa.withdrawn = false
AND bpa.processingState = 'FINISHED'
ORDER BY bpa.id ASC
"""

/**
 * Queries to fetch the migrated workflow instances for a batch
 * @param analysisIds list of IDs in one batch
 */
@Field final String queryRoddyFilesToMigrate = """
SELECT bpa, bf1, bf2 FROM BamFilePairAnalysis bpa
JOIN bpa.sampleType1BamFile bf1
JOIN FETCH bf1.workflowArtefact
JOIN bpa.sampleType2BamFile bf2
JOIN FETCH bf2.workflowArtefact
WHERE bpa.id in (:analysisIds)
AND bf1.workflowArtefact IS NOT NULL
AND bf2.workflowArtefact IS NOT NULL
"""

/**
 * Queries to fetch the Sophia workflow instances which are input for Aceseq workflow
 * @param analysisIds list of IDs in one batch
 */
@Field final String querySophiaForAceseq = """
SELECT bpaAceseq.id, bpaSophia FROM BamFilePairAnalysis bpaSophia
INNER JOIN BamFilePairAnalysis bpaAceseq ON bpaSophia.sampleType1BamFile = bpaAceseq.sampleType1BamFile
                                        AND bpaSophia.sampleType2BamFile = bpaAceseq.sampleType2BamFile 
JOIN FETCH bpaSophia.workflowArtefact
WHERE bpaAceseq.id in (:analysisIds)
ORDER BY bpaAceseq.id, bpaSophia.id
"""

/**
 * Queries to fetch the Snv workflow instances which are input for RunYapsa workflow
 * @param analysisIds list of IDs in one batch
 */
@Field final String querySnvForRunYapsa = """
SELECT bpaRunYapsa.id, bpaSnv FROM BamFilePairAnalysis bpaSnv
INNER JOIN BamFilePairAnalysis bpaRunYapsa ON bpaSnv.sampleType1BamFile = bpaRunYapsa.sampleType1BamFile
                                          AND bpaSnv.sampleType2BamFile = bpaRunYapsa.sampleType2BamFile
JOIN FETCH bpaSnv.workflowArtefact
WHERE bpaRunYapsa.id in (:analysisIds)
ORDER BY bpaRunYapsa.id, bpaSnv.id
"""

/*
 *main function to create WF runs and artefacts
 */
void migrateToNewWorkflow(
        List<Long> analysisIds,
        Workflow workflow,
        ProcessingPriority priority
) {
    // fetch the analysis instance with its two bam files for the given analysis IDs
    List<AnalysisQueryResult> analysesInBatch = BamFilePairAnalysis.executeQuery(queryRoddyFilesToMigrate, [
            analysisIds: analysisIds,
    ]).collect {
        new AnalysisQueryResult([
                analysis      : it[0],
                bamFileDisease: it[1],
                bamFileControl: it[2],
        ])
    }

    // construct a Map/Dictionary with Aceseq ID as key and its corresponding Sophia instance as value
    Map<Long, BamFilePairAnalysis> aceseq2SophiaMap = analysisToMigrate == AceseqWorkflow.WORKFLOW ? BamFilePairAnalysis.executeQuery(querySophiaForAceseq, [
            analysisIds: analysisIds,
    ]).collectEntries() {
        [(it[0]): it[1]]
    } : [:]

    // construct a Map/Dictionary with RunYapsa ID as key and its corresponding Snv instance as value
    Map<Long, BamFilePairAnalysis> runyapsa2SnvMap = analysisToMigrate == RunYapsaWorkflow.WORKFLOW ? BamFilePairAnalysis.executeQuery(querySnvForRunYapsa, [
            analysisIds: analysisIds,
    ]).collectEntries() {
        [(it[0]): it[1]]
    } : [:]

    analysesInBatch.each { AnalysisQueryResult analysisQueryResult ->
        BamFilePairAnalysis analysis = analysisQueryResult.analysis
        String shortName = "${analysisToMigrate}: ${analysis.individual.pid} " +
                "${analysis.sampleType1BamFile.sampleType.displayName} ${analysis.seqType.displayNameWithLibraryLayout}"

        AbstractBamFile bamFileDisease = analysisQueryResult.bamFileDisease
        AbstractBamFile bamFileControl = analysisQueryResult.bamFileControl

        assert bamFileDisease: "input artefact of ${bamFileDisease} can't be null."
        assert bamFileControl: "input artefact of ${bamFileControl} can't be null."

        List<String> runDisplayName = []
        runDisplayName.with {
            add("project: ${analysis.project.name}")
            add("individual: ${analysis.individual.displayName}")
            add("sampleType1: ${analysis.sampleType1BamFile.sampleType.displayName}")
            add("sampleType2: ${analysis.sampleType2BamFile.sampleType.displayName}")
            add("seqType: ${analysis.seqType.displayNameWithLibraryLayout}")
            add("referenceGenome: ${analysis.referenceGenome.name}")
        }
        List<String> artefactDisplayName = runDisplayName.clone()
        artefactDisplayName.remove(0)

        // create workflow run and input artefact for current its analysis
        WorkflowRun workflowRun = new WorkflowRun([
                workDirectory   : "",
                state           : WorkflowRun.State.LEGACY,
                project         : analysis.project,
                combinedConfig  : '{}',
                priority        : priority,
                workflowSteps   : [],
                workflow        : workflow,
                displayName     : runDisplayName,
                shortDisplayName: shortName,
        ]).save(flush: false)

        //--- create the input artefacts
        new WorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : AbstractAnalysisWorkflow.INPUT_TUMOR_BAM,
                workflowArtefact: bamFileDisease.workflowArtefact,
        ]).save(flush: false)

        new WorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : AbstractAnalysisWorkflow.INPUT_CONTROL_BAM,
                workflowArtefact: bamFileControl.workflowArtefact,
        ]).save(flush: false)

        // special handling for Aceseq: Sophia as input artefact
        if (analysisToMigrate == AceseqWorkflow.WORKFLOW) {
            BamFilePairAnalysis sophiaAnalysis = aceseq2SophiaMap[analysis.id]

            assert sophiaAnalysis.workflowArtefact: "Migrate Sophia analysis workflows first, then run this script again for Aceseq workflows"

            new WorkflowRunInputArtefact([
                    workflowRun     : workflowRun,
                    role            : SophiaWorkflow.SOPHIA_INPUT,
                    workflowArtefact: sophiaAnalysis.workflowArtefact,
            ]).save(flush: false)
        }

        // special handling for RunYapsa: Snv as input artefact
        if (analysisToMigrate == RunYapsaWorkflow.WORKFLOW) {
            BamFilePairAnalysis snvAnalysis = runyapsa2SnvMap[analysis.id]

            assert snvAnalysis.workflowArtefact: "Migrate Snv analysis workflows first, then run this script again for RunYapsa workflows"

            new WorkflowRunInputArtefact([
                    workflowRun     : workflowRun,
                    role            : SnvWorkflow.SNV_INPUT,
                    workflowArtefact: snvAnalysis.workflowArtefact,
            ]).save(flush: false)
        }

        //--- create the output artefacts
        WorkflowArtefactValues values = new WorkflowArtefactValues(
                workflowRun, AbstractAnalysisWorkflow.ANALYSIS_OUTPUT, artefactTypeMap[analysisToMigrate], artefactDisplayName
        )

        analysis.workflowArtefact = new WorkflowArtefact([
                producedBy      : values.run,
                outputRole      : values.role,
                withdrawnDate   : null,
                withdrawnComment: null,
                state           : WorkflowArtefact.State.SUCCESS,
                artefactType    : values.artefactType,
                displayName     : StringUtils.generateMultiLineDisplayName(values.displayNameLines),
        ]).save(flush: false)

        analysis.save(flush: false)
    }
}

// =================================================
List<Long> analysisIds = BamFilePairAnalysis.executeQuery(queryAnalysesToMigrate, [
        analysisClass: workflowClassMap[analysisToMigrate]
])
List<List<Long>> listOfListOfanalysisIds = analysisIds.collate(batchSize) as List<List<Long>>

int numAnalyses = analysisIds.size()
println "There are ${numAnalyses} analyses to be migrated into new workflow system"

if (analysisIds) {
    // process the data in chunks
    long numBatches = Math.ceil(numAnalyses / batchSize) as long
    println "${numBatches} batches will be processed"

    // fetch the workflow
    Workflow workflow = workflowService.getExactlyOneWorkflow(analysisToMigrate)
    assert workflow: "configured workflow ${analysisToMigrate} does not exists"
    println "Migrate from old to new workflow systems for Workflow \"${analysisToMigrate}\""

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
    try {
        withPool(numCores, {
            // loop through each batch and process it
            listOfListOfanalysisIds.makeConcurrent().each { List<Long> partAnalysisIds ->
                try {
                    TransactionUtils.withNewTransaction { session ->
                        // start the migration
                        migrateToNewWorkflow(partAnalysisIds, workflow, priority)
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
