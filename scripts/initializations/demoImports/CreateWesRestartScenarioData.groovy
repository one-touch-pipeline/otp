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

/**
 * Creates demo data for OTP-2980: WES report visibility after a step restart.
 *
 * Scenario: a WesFastQcWorkflow run where the WES execution step failed and was restarted.
 * - The old (obsolete) step has a COMPLETE WesRunLog  → hasReports() must return false (step is obsolete)
 * - The restarted step has a COMPLETE WesRunLog       → hasReports() must return true  (step is not obsolete)
 *
 * The restarted step's work folder contains a real report*.html file so the "WESkit Report"
 * link is actually downloadable.
 *
 * Safe to re-run: every create block is guarded by findBy…/atMostOneElement.
 */

import io.swagger.client.wes.model.State

import de.dkfz.tbi.otp.filestore.BaseFolder
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StackTraceUtils
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.time.*

// ── no external services needed — everything goes through GORM directly ───────

// ── constants ─────────────────────────────────────────────────────────────────
// Short display name used as idempotency key — script is safe to re-run
final String SHORT_NAME  = "WES Restart Scenario (OTP-2980)"
// subPath inside the work folder where the report file is placed
final String REPORT_SUBPATH = "report"
// Report file name must match the regex /report.*\.html/ (WesRunService.MATCHER_REPORT_FILE)
final String REPORT_FILENAME = "report-wes-restart-demo.html"

// Fixed UUID so the SSH container init script (30-create-wes-restart-report.sh) can
// place the report file at a known path without needing to query the database.
// Must match the UUID hardcoded in docker/ssh/30-create-wes-restart-report.sh.
final UUID FIXED_WORK_FOLDER_UUID = UUID.fromString("c0ffee00-2980-4000-b000-000000002980")

// ── main transaction ──────────────────────────────────────────────────────────
WorkflowRun workflowRun = null

WorkflowRun.withNewTransaction {
    // Reuse whatever project / seqType already exist in the dump
    SeqType seqType = SeqType.findByName('WHOLE_GENOME') ?: SeqType.findByName('EXAMPLE')
    assert seqType : "No suitable SeqType found. Make sure the dev dump is loaded."

    SeqTrack seqTrack = SeqTrack.findBySeqType(seqType)
    assert seqTrack : "No SeqTrack for seqType ${seqType.name} found."

    Project project = seqTrack.individual.project

    // ── workflow ───────────────────────────────────────────────────────────────
    Workflow workflow = CollectionUtils.atMostOneElement(Workflow.findAllByName(WesFastQcWorkflow.WORKFLOW))
    assert workflow : "WesFastQcWorkflow (${WesFastQcWorkflow.WORKFLOW}) not found. Run CreateWesFastQcWorkflowData first."

    // ── idempotency guard: skip everything if the run already exists ───────────
    workflowRun = WorkflowRun.findByWorkflowAndProjectAndShortDisplayName(workflow, project, SHORT_NAME)
    if (workflowRun) {
        println "WorkflowRun '${SHORT_NAME}' already exists (id=${workflowRun.id}) — nothing to do."
        return
    }

    // ── work folder ───────────────────────────────────────────────────────────
    // Use a fixed UUID so the SSH container init script knows the path without querying the DB.
    // findAnyWritableBaseFolder() would fail because the existing BaseFolder has writable=false,
    // so we look it up directly by path instead.
    BaseFolder baseFolder = BaseFolder.findByPath("/home/otp/filesystem/otp_data")
    assert baseFolder : "BaseFolder not found. Make sure the dev dump is loaded."
    WorkFolder workFolder = new WorkFolder(
            baseFolder: baseFolder,
            uuid: FIXED_WORK_FOLDER_UUID,
    ).save(flush: true)

    // ── workflow run ──────────────────────────────────────────────────────────
    workflowRun = new WorkflowRun(
            workflow        : workflow,
            state           : WorkflowRun.State.FAILED,
            project         : project,
            priority        : project.processingPriority,
            displayName     : "${SHORT_NAME}\nproject: ${project.name}",
            shortDisplayName: SHORT_NAME,
            combinedConfig  : '{}',
            workflowSteps   : [],
            workFolder      : workFolder,
    ).save(flush: true)

    // ── artefacts ─────────────────────────────────────────────────────────────
    // Input artefact: producedBy=null, outputRole=null (it's an input, not produced by this run)
    new WorkflowRunInputArtefact(
            workflowRun     : workflowRun,
            role            : WesFastQcWorkflow.INPUT_FASTQ,
            workflowArtefact: new WorkflowArtefact(
                    state       : WorkflowArtefact.State.FAILED,
                    artefactType: ArtefactType.FASTQ,
                    displayName : "FastQC input artefact for ${SHORT_NAME}",
            ).save(flush: true),
    ).save(flush: true)

    // Output artefact: producedBy and outputRole must both be set
    new WorkflowArtefact(
            producedBy  : workflowRun,
            state       : WorkflowArtefact.State.FAILED,
            outputRole  : WesFastQcWorkflow.OUTPUT_FASTQC,
            artefactType: ArtefactType.FASTQ,
            displayName : "FastQC output artefact for ${SHORT_NAME}",
    ).save(flush: true)

    // ── helper closures ───────────────────────────────────────────────────────
    Closure<WesLog> makeWesLog = { State jobState ->
        new WesLog(
                name    : "main",
                cmd     : "nextflow run nf-core/fastqc",
                startTime: ZonedDateTime.now().minusHours(2),
                endTime  : ZonedDateTime.now().minusMinutes(30),
                stdout  : "stdout output",
                stderr  : jobState == State.COMPLETE ? "" : "ERROR: pipeline failed",
                exitCode: jobState == State.COMPLETE ? 0 : 1,
        ).save(flush: true)
    }

    Closure<WesRunLog> makeWesRunLog = { State jobState ->
        new WesRunLog(
                state     : jobState,
                runLog    : makeWesLog(jobState),
                taskLogs  : [makeWesLog(jobState)],
                runRequest: '{"workflow_url":"nf-core/fastqc"}',
        ).save(flush: true)
    }

    Closure<WorkflowError> makeError = {
        new WorkflowError(
                message   : "WES execution failed - step will be restarted.",
                stacktrace: StackTraceUtils.getStackTrace(new OtpRuntimeException("WES execution failed")),
        )
    }

    Closure addLog = { WorkflowStep step ->
        new WorkflowMessageLog(workflowStep: step, message: "Log for ${step.beanName}", createdBy: "SYSTEM").save(flush: true)
    }

    // ── step 1: preparatory step — succeeded ─────────────────────────────────
    WorkflowStep prepStep = new WorkflowStep(
            workflowRun: workflowRun,
            beanName   : "wesPrepareExecutionJob",
            state      : WorkflowStep.State.SUCCESS,
            clusterJobs: [] as Set,
            wesRuns    : [] as Set,
    ).save(flush: true)
    addLog(prepStep)

    // ── step 2: OLD execution step — failed, now OBSOLETE ─────────────────────
    // This step has a COMPLETE WesRun. Before the fix it showed a report link;
    // after the fix hasReports() returns false because obsolete=true.
    WorkflowStep oldStep = new WorkflowStep(
            workflowRun  : workflowRun,
            beanName     : "wesExecuteJob",
            state        : WorkflowStep.State.FAILED,
            previous     : prepStep,
            workflowError: makeError(),
            obsolete     : true,          // ← marks this as the overwritten attempt
            clusterJobs  : [] as Set,
            wesRuns      : [] as Set,
    ).save(flush: true)
    addLog(oldStep)

    WesRun oldWesRun = new WesRun(
            workflowStep : oldStep,
            wesIdentifier: "wes-restart-old-run",
            subPath      : REPORT_SUBPATH,
            state        : WesRun.MonitorState.FINISHED,
            wesRunLog    : makeWesRunLog(State.COMPLETE),
    ).save(flush: true)
    oldStep.wesRuns.add(oldWesRun)
    oldStep.save(flush: true)

    // ── step 3: RESTARTED execution step — also failed (still open issue) ─────
    // This step has a COMPLETE WesRun with a real report file on disk.
    // hasReports() returns true (obsolete=false) → "WESkit Report" link is shown.
    WorkflowStep newStep = new WorkflowStep(
            workflowRun  : workflowRun,
            beanName     : "wesExecuteJob",
            state        : WorkflowStep.State.FAILED,
            previous     : oldStep,
            restartedFrom: oldStep,
            workflowError: makeError(),
            clusterJobs  : [] as Set,
            wesRuns      : [] as Set,
    ).save(flush: true)
    addLog(newStep)

    WesRun newWesRun = new WesRun(
            workflowStep : newStep,
            wesIdentifier: "wes-restart-new-run",
            subPath      : REPORT_SUBPATH,
            state        : WesRun.MonitorState.FINISHED,
            wesRunLog    : makeWesRunLog(State.COMPLETE),
    ).save(flush: true)
    newStep.wesRuns.add(newWesRun)
    newStep.save(flush: true)

    workflowRun.save(flush: true)

    println "Created WorkflowRun '${SHORT_NAME}' (id=${workflowRun.id})"
    println "  oldStep  id=${oldStep.id}  obsolete=true   wesRun id=${oldWesRun.id}"
    println "  newStep  id=${newStep.id}  obsolete=false  wesRun id=${newWesRun.id}"
    println "  workFolder uuid=${workFolder.uuid}"
    String uuidStr = workFolder.uuid.toString()
    println "  Report must be placed at: <baseFolder>/${uuidStr[0..1]}/${uuidStr[2..3]}/${uuidStr[4..-1]}/${REPORT_SUBPATH}/${REPORT_FILENAME}"
}

println "Done."
println ""
println "IMPORTANT: The report file must be added to the SSH server filesystem tarball."
println "See script comments for instructions."
''
