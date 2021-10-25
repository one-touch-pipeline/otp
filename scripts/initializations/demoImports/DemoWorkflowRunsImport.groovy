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

/**
 * This script creates demo workflow runs for development purposes.
 */

package initializations.demoImports

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.*

import java.time.ZonedDateTime

Project project1 = Project.findByName('Example project 1')

ProcessingPriority normalPriority = ProcessingPriority.findByName('NORMAL')
ProcessingPriority fasttrackPriority = ProcessingPriority.findByName('FASTTRACK')
ProcessingPriority extremeFasttrackPriority = ProcessingPriority.findByName('EXTREME FASTTRACK')
ProcessingPriority minimalPriority = ProcessingPriority.findByName('MINIMAL')
ProcessingPriority reprocessingPriority = ProcessingPriority.findByName('REPROCESSING')

Workflow fastqWorkflow = Workflow.findByName('FASTQ installation')
Workflow cellRangerWorkflow = Workflow.findByName('Cell Ranger')
Workflow rnaAlignmentWorkflow = Workflow.findByName('RNA alignment')

int workflowNumber = 0

/**
 * Additional FASTQ installation data
 */
Individual individual = Individual.findByPid("example_1")
SeqType seqType = SeqType.findByName("WHOLE_GENOME")
String multiLineDisplayName = '\nproject: ' + project1.displayName + '\nindividual: ' + individual.displayName + '\nsampleType: demo sampleType\nseqType: ' + seqType.displayName + '\nrun: demo run name\nlaneId: demo lane id'

/**
 * Create FASTQ installation workflow runs.
 */
WorkflowRun wr1 = new WorkflowRun(
        state: WorkflowRun.State.RUNNING_OTP,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'FASTQ installation ' + workflowNumber++ + multiLineDisplayName,
        shortDisplayName: 'FASTQ installation ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: normalPriority,
        workflow: fastqWorkflow,
).save(flush: true)

WorkflowArtefact wa1 = new WorkflowArtefact([
        producedBy      : wr1,
        outputRole      : 'FASTQ',
        withdrawnDate   : null,
        withdrawnComment: null,
        state           : WorkflowArtefact.State.PLANNED_OR_RUNNING,
        artefactType    : ArtefactType.FASTQ,
        individual      : individual,
        seqType         : seqType,
        displayName     : 'FASTQ Artefact ' + multiLineDisplayName,
]).save(flush: true)

new WorkflowRunInputArtefact([
        workflowRun: wr1,
        workflowArtefact: wa1,
        role: 'wa2 role',
]).save(flush: true)

new WorkflowRun(
        state: WorkflowRun.State.FAILED,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'FASTQ installation ' + workflowNumber++ + multiLineDisplayName,
        shortDisplayName: 'FASTQ installation ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: normalPriority,
        workflow: fastqWorkflow,
).save(flush: true)

WorkflowRun wr2 = new WorkflowRun(
        state: WorkflowRun.State.RUNNING_OTP,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'FASTQ installation ' + workflowNumber++ + multiLineDisplayName,
        shortDisplayName: 'FASTQ installation ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: fasttrackPriority,
        workflow: fastqWorkflow,
        jobCanBeRestarted: false,
).save(flush: true)

WorkflowArtefact wa2 = new WorkflowArtefact([
        producedBy      : wr2,
        outputRole      : 'FASTQ',
        withdrawnDate   : null,
        withdrawnComment: null,
        state           : WorkflowArtefact.State.SUCCESS,
        artefactType    : ArtefactType.FASTQ,
        individual      : individual,
        seqType         : seqType,
        displayName     : 'FASTQ Artefact ' + multiLineDisplayName,
]).save(flush: true)

new WorkflowRunInputArtefact([
        workflowRun: wr2,
        workflowArtefact: wa2,
        role: 'wa2 role',
]).save(flush: true)

new WorkflowRun(
        state: WorkflowRun.State.OMITTED_MISSING_PRECONDITION,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'FASTQ installation ' + workflowNumber++ + multiLineDisplayName,
        shortDisplayName: 'FASTQ installation ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: minimalPriority,
        workflow: fastqWorkflow,
).save(flush: true)

/**
 * Create Cell Ranger workflow runs.
 */

new WorkflowRun(
        state: WorkflowRun.State.RUNNING_OTP,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        shortDisplayName: 'Cell Ranger ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: minimalPriority,
        workflow: cellRangerWorkflow,
).save(flush: true)

WorkflowRun wr3 = new WorkflowRun(
        state: WorkflowRun.State.RUNNING_OTP,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        shortDisplayName: 'Cell Ranger ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: extremeFasttrackPriority,
        jobCanBeRestarted: false,
        workflow: cellRangerWorkflow,
).save(flush: true)

new WorkflowRun(
        state: WorkflowRun.State.PENDING,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        shortDisplayName: 'Cell Ranger ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: minimalPriority,
        workflow: cellRangerWorkflow,
).save(flush: true)

/**
 * Create rna alignment workflow runs.
 */

new WorkflowRun(
        state: WorkflowRun.State.RUNNING_OTP,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        shortDisplayName: 'RNA ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: reprocessingPriority,
        workflow: rnaAlignmentWorkflow,
).save(flush: true)

new WorkflowRun(
        state: WorkflowRun.State.PENDING,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        shortDisplayName: 'RNA ' + workflowNumber,
        combinedConfig: '{test: 1}',
        priority: normalPriority,
        workflow: rnaAlignmentWorkflow,
).save(flush: true)


/**
 * Create workflow steps.
 */
List<WorkflowStep> workflowSteps1 = []
List<WorkflowStep> workflowSteps2 = []
List<WorkflowStep> workflowSteps3 = []

workflowSteps1.add(
        new WorkflowStep(
                state: WorkflowStep.State.SUCCESS,
                beanName: 'Test Step 1',
                workflowRun: wr1,
        ).save(flush: true)
)

workflowSteps1.add(
        new WorkflowStep(
                state: WorkflowStep.State.RUNNING,
                beanName: 'Test Step 2',
                workflowRun: wr1,
        ).save(flush: true)
)

workflowSteps1.add(
        new WorkflowStep(
                state: WorkflowStep.State.CREATED,
                beanName: 'Test Step 3',
                workflowRun: wr1,
        ).save(flush: true)
)

workflowSteps2.add(
        new WorkflowStep(
                state: WorkflowStep.State.CREATED,
                beanName: 'Test Step A',
                workflowRun: wr2,
        ).save(flush: true)
)

workflowSteps3.add(
        new WorkflowStep(
                state: WorkflowStep.State.CREATED,
                beanName: 'Test Step XY',
                workflowRun: wr3,
        ).save(flush: true)
)

workflowSteps3.add(
        new WorkflowStep(
                state: WorkflowStep.State.RUNNING,
                beanName: 'Test Step ZZ',
                workflowRun: wr3,
        ).save(flush: true)
)

wr1.workflowSteps = workflowSteps1
wr1.save(flush: true)

wr2.workflowSteps = workflowSteps2
wr2.save(flush: true)

wr3.workflowSteps = workflowSteps3
wr3.save(flush: true)

Realm realm = Realm.findAll().first()

workflowSteps1.each { step ->
        new ClusterJob([
                validated: true,
                realm: realm,
                clusterJobId: "job-" + step.id,
                clusterJobName: "Cluster Job " + step.id,
                jobClass: "test",
                queued: ZonedDateTime.now().minusDays(2),
                started: ZonedDateTime.now().minusDays(1),
                ended: ZonedDateTime.now(),
                userName: "Test User",
                checkStatus: ClusterJob.CheckStatus.CREATED,
                oldSystem: false,
                workflowStep: step,
        ]).save(flush: true)
}
