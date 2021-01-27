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

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

Project project1 = Project.findByName('testProject1')

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
 * Create FASTQ installation workflow runs.
 */
WorkflowRun wr1 = new WorkflowRun(
        state: WorkflowRun.State.RUNNING,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        combinedConfig: '{test: 1}',
        priority: normalPriority,
        workflow: fastqWorkflow,
).save(flush: true)

new WorkflowRun(
        state: WorkflowRun.State.FAILED,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        combinedConfig: '{test: 1}',
        priority: normalPriority,
        workflow: fastqWorkflow,
).save(flush: true)

WorkflowRun wr2 = new WorkflowRun(
        state: WorkflowRun.State.RUNNING,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        combinedConfig: '{test: 1}',
        priority: fasttrackPriority,
        workflow: fastqWorkflow,
        jobCanBeRestarted: false,
).save(flush: true)

new WorkflowRun(
        state: WorkflowRun.State.SKIPPED,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        combinedConfig: '{test: 1}',
        priority: minimalPriority,
        workflow: fastqWorkflow,
).save(flush: true)

/**
 * Create Cell Ranger workflow runs.
 */

new WorkflowRun(
        state: WorkflowRun.State.RUNNING,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        combinedConfig: '{test: 1}',
        priority: minimalPriority,
        workflow: cellRangerWorkflow,
).save(flush: true)

WorkflowRun wr3 = new WorkflowRun(
        state: WorkflowRun.State.RUNNING,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
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
        combinedConfig: '{test: 1}',
        priority: minimalPriority,
        workflow: cellRangerWorkflow,
).save(flush: true)

/**
 * Create rna alignment workflow runs.
 */

new WorkflowRun(
        state: WorkflowRun.State.RUNNING,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
        combinedConfig: '{test: 1}',
        priority: reprocessingPriority,
        workflow: rnaAlignmentWorkflow,
).save(flush: true)

new WorkflowRun(
        state: WorkflowRun.State.PENDING,
        project: project1,
        workDirectory: '/tmp',
        displayName: 'Sample Run ' + workflowNumber++,
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
