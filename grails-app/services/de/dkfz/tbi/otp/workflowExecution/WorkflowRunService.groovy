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
import org.hibernate.NullPrecedence
import org.hibernate.criterion.Order
import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.SqlUtil
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils

import java.time.LocalDateTime

import static de.dkfz.tbi.otp.infrastructure.ClusterJob.CheckStatus.FINISHED

@Transactional
class WorkflowRunService {

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

    ClusterJobService clusterJobService

    ConfigFragmentService configFragmentService

    ConfigService configService

    WorkflowStepService workflowStepService

    int countOfRunningWorkflows() {
        return WorkflowRun.countByStateInList(STATES_COUNTING_AS_RUNNING)
    }

    WorkflowRun nextWaitingWorkflow(int workflowCount) {
        return WorkflowRun.find(WAITING_WORKFLOW_QUERY, [
                workflowCount: workflowCount,
        ])
    }

    WorkflowRun getById(long id) {
        return WorkflowRun.get(id)
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

    private Closure getCriteria(Workflow workflow, List<WorkflowRun.State> states, String name) {
        return {
            if (name) {
                ilike("displayName", "%${SqlUtil.replaceWildcardCharactersInLikeExpression(name)}%")
            }
            if (states) {
                'in'("state", states)
            }
            if (workflow) {
                eq("workflow", workflow)
            }
            ne("state", WorkflowRun.State.LEGACY)
        }
    }

    @SuppressWarnings('AbcMetric')
    WorkflowRunSearchResult workflowOverview(WorkflowRunSearchCriteria workflowRunSearchCriteria) {
        Closure criteria = getCriteria(workflowRunSearchCriteria.workflow, workflowRunSearchCriteria.states, workflowRunSearchCriteria.name)
        WorkflowRunSearchResult result = new WorkflowRunSearchResult()

        result.data = WorkflowRun.createCriteria().list {
            criteria.delegate = delegate
            criteria()
            workflowRunSearchCriteria.orderList.each { DataTablesCommand.Order dtOrder ->
                WorkflowRunListColumn column = WorkflowRunListColumn.fromDataTable(dtOrder.column)
                if (column == WorkflowRunListColumn.COMMENT) {
                    createAlias("comment", "comment", JoinType.LEFT_OUTER_JOIN)
                    if (dtOrder.direction == DataTablesCommand.Order.Dir.asc) {
                        addOrder(Order.asc("comment.modificationDate").nulls(NullPrecedence.LAST))
                    } else {
                        addOrder(Order.desc("comment.modificationDate").nulls(NullPrecedence.LAST))
                    }
                } else {
                    order(column.orderColumn, dtOrder.direction.name())
                }
            }
            firstResult(workflowRunSearchCriteria.start)
            if (workflowRunSearchCriteria.pagingEnabled) {
                maxResults(workflowRunSearchCriteria.length)
            }
        }.collect { WorkflowRun r ->
            String duration = r.workflowSteps.empty ? "-" :
                    r.state in [WorkflowRun.State.PENDING,
                                WorkflowRun.State.RUNNING_WES,
                                WorkflowRun.State.RUNNING_OTP,] ?
                            TimeUtils.getFormattedDuration(convertDateToLocalDateTime(r.workflowSteps.first().dateCreated),
                                    convertDateToLocalDateTime(new Date())) :
                            TimeUtils.getFormattedDuration(convertDateToLocalDateTime(r.workflowSteps.first().dateCreated),
                                    convertDateToLocalDateTime(r.workflowSteps.last().lastUpdated))

            List<WorkflowStep> steps = r.workflowSteps.findAll { !it.obsolete }
            WorkflowStep lastStep = steps ? steps.last() : null
            return [
                    state      : r.state,
                    stateDesc  : r.state.description,
                    comment    : r.comment?.displayString()?.replaceAll("\n", ", ") ?: "",
                    workflow   : r.workflow.toString(),
                    displayName: r.displayName,
                    shortName  : r.shortDisplayName,
                    dateCreated: TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(r.dateCreated),
                    lastUpdated: lastStep?.lastUpdated ? TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(lastStep.lastUpdated) : "",
                    duration   : duration,
                    id         : r.id,
                    step       : lastStep?.beanName,
                    stepId     : lastStep?.id,
                    steps      : (steps - lastStep).reverse()*.beanName,
                    stepIds    : (steps - lastStep).reverse()*.id,
            ]
        }
        result.workflowsFiltered = WorkflowRun.createCriteria().count {
            criteria.delegate = delegate
            criteria()
        }
        result.running = WorkflowRun.createCriteria().count {
            criteria.delegate = delegate
            criteria()
            "in"("state", [WorkflowRun.State.RUNNING_OTP, WorkflowRun.State.RUNNING_WES])
        }
        result.failed = WorkflowRun.createCriteria().count {
            criteria.delegate = delegate
            criteria()
            eq("state", WorkflowRun.State.FAILED)
        }
        result.workflowsTotal = WorkflowRun.countByStateNotEqual(WorkflowRun.State.LEGACY)

        return result
    }

    private LocalDateTime convertDateToLocalDateTime(Date date) {
        return date.toInstant().atZone(configService.timeZoneId).toLocalDateTime()
    }

    WorkflowRun findAllByRestartedFrom(WorkflowRun workflowRun) {
        return CollectionUtils.atMostOneElement(WorkflowRun.findAllByRestartedFrom(workflowRun))
    }

    List<WorkflowRun> workflowRunList(Workflow workflow, List<WorkflowRun.State> states, String name) {
        Closure criteria = getCriteria(workflow, states, name,)
        List<WorkflowRun> data = WorkflowRun.createCriteria().list {
            criteria.delegate = delegate
            criteria()
        } as List<WorkflowRun>
        data.sort { -it.id }
        return data
    }

    List<Map<String, Object>> workflowRunDetails(WorkflowRun workflowRun) {
        List<WorkflowStep> workflowSteps = workflowRun.workflowSteps.reverse()

        return workflowSteps.collect { step ->
            boolean isPreviousOfFailedStep = !workflowSteps.findAll {
                workflowStepService.getPreviousRunningWorkflowStep(it)?.id == step.id && it.state == WorkflowStep.State.FAILED
            }.empty

            return [
                    state                 : step.state,
                    id                    : step.id,
                    name                  : step.beanName,
                    dateCreated           : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(step.dateCreated),
                    lastUpdated           : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(step.lastUpdated),
                    duration              : TimeUtils.getFormattedDuration(convertDateToLocalDateTime(step.dateCreated),
                            convertDateToLocalDateTime(step.lastUpdated)),

                    error                 : step.workflowError,
                    clusterJobs           : (step.clusterJobs as List<ClusterJob>).sort { it.dateCreated }.collect { ClusterJob clusterJob ->
                        [
                                state   : "${clusterJob.checkStatus}${clusterJob.checkStatus == FINISHED ? "/${clusterJob.exitStatus}" : ""}",
                                id      : clusterJob.id,
                                name    : clusterJob.clusterJobName,
                                jobId   : clusterJob.clusterJobId,
                                hasLog  : clusterJobService.doesClusterJobLogExist(clusterJob),
                                node    : clusterJob.node ?: "-",
                                wallTime: clusterJob.elapsedWalltimeAsHhMmSs,
                                exitCode: clusterJob.exitCode ?: "-",
                        ]
                    },
                    wes                   : step.wesIdentifier,
                    hasLogs               : !step.logs.empty,
                    obsolete              : step.obsolete,
                    previousStepId        : workflowStepService.getPreviousRunningWorkflowStep(step)?.id,
                    isPreviousOfFailedStep: isPreviousOfFailedStep,
            ]
        }
    }
}
