/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.utils.TransactionUtils

@Transactional
class WorkflowRunService {

    ConfigFragmentService configFragmentService

    final static List<WorkflowRun.State> STATES_COUNTING_AS_RUNNING = [
            WorkflowRun.State.RUNNING_OTP,
            WorkflowRun.State.RUNNING_WES,
    ].asImmutable()

    private final static String WAITING_WORKFLOW_QUERY = """
        from
            WorkflowRun wr
        where
            wr.state = '${WorkflowRun.State.PENDING}'
            and wr.priority in (
                select
                    pp
                from
                    ProcessingPriority pp
                where
                    pp.allowedParallelWorkflowRuns > :workflowCount
            )
            and not exists (
                from
                    WorkflowRunInputArtefact wia
                where
                    wia.workflowRun = wr
                    and wia.workflowArtefact.state != '${WorkflowArtefact.State.SUCCESS}'
            )
            and wr.workflow.maxParallelWorkflows > (
                select
                    count(id)
                from
                    WorkflowRun wr2
                where
                    wr2.workflow = wr.workflow
                    and wr2.state in ('${STATES_COUNTING_AS_RUNNING.join('\',\'')}')
            )
        order by
            wr.priority.priority desc,
            wr.workflow.priority desc,
            wr.dateCreated
        """

    int countOfRunningWorkflows() {
        return WorkflowRun.countByStateInList(STATES_COUNTING_AS_RUNNING)
    }

    WorkflowRun nextWaitingWorkflow(int workflowCount) {
        return WorkflowRun.find(WAITING_WORKFLOW_QUERY, [
                workflowCount: workflowCount,
        ])
    }

    /**
     * Creates a new unflushed WorkflowRun.
     *
     * The command creates a workflow run with all the given parameter, save it in hibernate but do not flush it the database to improve the performance.
     * Therefore it is necessary to do somewhere later in the transaction a <b> flush </b> to get it in the database.
     *
     * @param workflow The workflow this run should belong to
     * @param priority The priority to use for scheduling the run
     * @param workDirectory The directory for the data of the workflow
     * @param project The project the run should belong to
     * @param displayNameLines A name for the run. It is used in the GUI to show and also for filtering
     * @param shortName A short display name
     * @param configs The sorted configs used for this workflow
     * @return the created, saved but not flushed WorkflowRun
     */
    WorkflowRun buildWorkflowRun(Workflow workflow, ProcessingPriority priority, String workDirectory, Project project, List<String> displayNameLines,
                                 String shortName, List<ExternalWorkflowConfigFragment> configs = []) {
        String combinedConfig = configFragmentService.mergeSortedFragments(configs)
        String displayName = StringUtils.generateMultiLineDisplayName(displayNameLines)

        return new WorkflowRun([
                workDirectory   : workDirectory,
                state           : WorkflowRun.State.PENDING,
                project         : project,
                configs         : configs,
                combinedConfig  : combinedConfig,
                priority        : priority,
                restartedFrom   : null,
                omittedMessage  : null,
                workflowSteps   : [],
                workflow        : workflow,
                displayName     : displayName,
                shortDisplayName: shortName,
        ]).save(flush: false)
    }

    /**
     * Method to change {@link WorkflowRun#jobCanBeRestarted} to true using a separate transaction to ensure that this info doesn't get lost on
     * rollback of the current transaction.
     */
    void markJobAsNotRestartableInSeparateTransaction(WorkflowRun workflowRun) {
        assert workflowRun
        TransactionUtils.withNewTransaction {
            //needs to fetch it new, otherwise a "illegally attempted to associate a proxy with two open Sessions" exception occurred
            WorkflowRun workflowRun2 = WorkflowRun.get(workflowRun.id)
            workflowRun2.jobCanBeRestarted = false
            workflowRun2.save(flush: true)
        }
        workflowRun.refresh()
    }
}
