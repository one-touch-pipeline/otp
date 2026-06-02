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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor
import io.swagger.client.wes.model.State
import org.hibernate.*
import org.hibernate.criterion.Order
import org.hibernate.sql.JoinType
import org.hibernate.transform.Transformers
import org.hibernate.type.StandardBasicTypes

import de.dkfz.tbi.otp.SqlUtil
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflowExecution.wes.*

import javax.sql.DataSource
import java.time.ZonedDateTime

@Transactional
class WorkflowRunService {

    DataSource dataSource
    ClusterJobDetailService clusterJobDetailService
    WorkflowLogService workflowLogService
    ClusterJobService clusterJobService
    ConfigFragmentService configFragmentService
    ConfigService configService
    WorkflowStepService workflowStepService
    WesRunService wesRunService

    public static final int PESSIMISTIC_WRITE_TIME_OUT = 10000

    public static final List<WorkflowRun.State> STATES_COUNTING_AS_RUNNING = [
            WorkflowRun.State.RUNNING_OTP,
            WorkflowRun.State.RUNNING_WES,
    ].asImmutable()

    public static final List<WorkflowRun.State> PENDING_STATES = [
            WorkflowRun.State.PENDING,
    ].asImmutable()

    public static final List<WorkflowRun.State> SUCCESS_STATES = [
            WorkflowRun.State.SUCCESS,
    ].asImmutable()

    private static String buildWaitingWorkflowQuery(Map<String, String> placeholders) {
        return """
            WITH running_counts AS (
                SELECT workflow_id, COUNT(*) AS running_count
                FROM workflow_run
                WHERE state IN (${placeholders.runningStates})
                GROUP BY workflow_id
            )
            SELECT wr.id
            FROM workflow_run wr
            JOIN project p ON p.id = wr.project_id
            JOIN workflow w ON w.id = wr.workflow_id
            JOIN processing_priority pp ON pp.id = wr.priority_id
            LEFT JOIN running_counts rc ON rc.workflow_id = wr.workflow_id
            WHERE wr.state IN (${placeholders.pendingStates})
              AND w.enabled = true
              AND p.state NOT IN (${placeholders.excludedStates})
              AND pp.allowed_parallel_workflow_runs > ?
              AND COALESCE(rc.running_count, 0) < w.max_parallel_workflows
              AND NOT EXISTS (
                SELECT 1
                FROM workflow_run_input_artefact wia
                JOIN workflow_artefact wa ON wa.id = wia.workflow_artefact_id
                WHERE wia.workflow_run_id = wr.id
                  AND wa.state NOT IN (${placeholders.successStates})
              )
            ORDER BY pp.priority DESC, w.priority DESC, (wr.restarted_from_id IS NOT NULL) DESC, wr.date_created
            LIMIT 1
        """
    }

    @CompileDynamic
    int countOfRunningWorkflows() {
        return WorkflowRun.countByStateInList(STATES_COUNTING_AS_RUNNING)
    }

    WorkflowRun nextWaitingWorkflow(int allowedRunLimit) {
        Map<String, List<String>> params = [
                runningStates : STATES_COUNTING_AS_RUNNING*.name(),
                pendingStates : PENDING_STATES*.name(),
                excludedStates: [Project.State.ARCHIVED, Project.State.DELETED]*.name(),
                successStates : SUCCESS_STATES*.name(),
        ]

        Map<String, String> placeholders = params.collectEntries { k, v ->
            [k, v.collect { '?' }.join(', ')]
        }

        String sqlQuery = buildWaitingWorkflowQuery(placeholders)

        List<Object> queryParams = []
        queryParams.addAll(params.runningStates)
        queryParams.addAll(params.pendingStates)
        queryParams.addAll(params.excludedStates)
        queryParams.add(allowedRunLimit)
        queryParams.addAll(params.successStates)

        return new Sql(dataSource).withCloseable { Sql sql ->
            GroovyRowResult result = sql.firstRow(sqlQuery, queryParams)
            return result ? getById(result.id as Long) : null
        }
    }

    @CompileDynamic
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
     * @return the created, saved but not flushed WorkflowRun
     */
    @SuppressWarnings('ParameterCount')
    @CompileDynamic
    WorkflowRun buildWorkflowRun(Workflow workflow, ProcessingPriority priority, String workDirectory, Project project, List<String> displayNameLines,
                                 String shortName, WorkflowVersion workflowVersion = null) {
        String displayName = StringUtils.generateMultiLineDisplayName(displayNameLines)

        return new WorkflowRun([
                workDirectory   : workDirectory,
                state           : WorkflowRun.State.PENDING,
                project         : project,
                combinedConfig  : null,
                priority        : priority,
                restartedFrom   : null,
                skipMessage     : null,
                workflowSteps   : [],
                workflow        : workflow,
                workflowVersion : workflowVersion,
                displayName     : displayName,
                shortDisplayName: shortName,
        ]).save(flush: false, deepValidate: false)
    }

    @CompileDynamic
    void saveCombinedConfig(Long id, String combinedConfig) {
        WorkflowRun workflowRun = WorkflowRun.get(id)
        assert workflowRun: "No WorkflowRun with id '${id}' found"
        workflowRun.combinedConfig = combinedConfig
        workflowRun.save(flush: true)
    }

    /**
     * helper do get pessimistic lock for workflowRun and all its steps and wait therefor for 10 second
     */
    @CompileDynamic
    void lockAndRefreshWorkflowRunWithSteps(WorkflowRun run) {
        WorkflowRun.withSession { Session s ->
            s.refresh(run, new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(PESSIMISTIC_WRITE_TIME_OUT))
            run.workflowSteps.each {
                s.refresh(it, new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(PESSIMISTIC_WRITE_TIME_OUT))
            }
        }
    }

    /**
     * Method to change {@link WorkflowRun#jobCanBeRestarted} to false using a separate transaction to ensure that this info doesn't get lost on
     * rollback of the current transaction.
     */
    @CompileDynamic
    void markJobAsNotRestartableInSeparateTransaction(WorkflowRun workflowRun) {
        assert workflowRun
        TransactionUtils.withNewTransaction {
            // needs to fetch it new, otherwise a "illegally attempted to associate a proxy with two open Sessions" exception occurred
            WorkflowRun workflowRun2 = WorkflowRun.get(workflowRun.id)
            workflowRun2.jobCanBeRestarted = false
            workflowRun2.save(flush: true)
        }
        workflowRun.refresh()
    }

    /**
     * Method to change {@link WorkflowRun#jobCanBeRestarted} to true, which should be rolled back in the current transaction if error occurs.
     */
    @CompileDynamic
    void markJobAsRestartable(WorkflowRun workflowRun) {
        assert workflowRun
        workflowRun.jobCanBeRestarted = true
        workflowRun.save(flush: true)
    }

    @CompileDynamic
    private Map<Long, List<WorkflowStepInfo>> fetchWorkflowStepsForRuns(List<Long> runIds) {
        return (WorkflowStep.createCriteria().list {
            createAlias("workflowRun", "workflowRun")
            'in'("workflowRun.id", runIds)
            projections {
                property("id", "id")
                property("obsolete", "obsolete")
                property("beanName", "beanName")
                property("lastUpdated", "lastUpdated")
                property("workflowRun.id", "workflowRunId")
            }
            order("id")
            resultTransformer(Transformers.aliasToBean(WorkflowStepInfo))
        } as List<WorkflowStepInfo>).groupBy { WorkflowStepInfo info -> info.workflowRunId }
    }

    @CompileDynamic
    private List<Long> fetchMatchingRunIds(String stepFilter) {
        return WorkflowStep.executeQuery("""
            SELECT ws.workflowRun.id
            FROM WorkflowStep ws
            WHERE ws.obsolete = false
              AND ws.beanName = :filter
              AND ws.id = (
                  SELECT max(ws2.id) FROM WorkflowStep ws2
                  WHERE ws2.workflowRun = ws.workflowRun AND ws2.obsolete = false
              )
        """, [filter: stepFilter]) as List<Long>
    }

    @CompileDynamic
    private Closure getCriteria(Workflow workflow, List<WorkflowRun.State> states, String name, List<Long> matchingRunIds) {
        return {
            if (name) {
                or {
                    ilike("shortDisplayName", "%${SqlUtil.replaceWildcardCharactersInLikeExpression(name)}%")
                    ilike("displayName", "%${SqlUtil.replaceWildcardCharactersInLikeExpression(name)}%")
                }
            }
            if (states) {
                'in'("state", states)
            }
            if (workflow) {
                eq("workflow", workflow)
            }
            ne("state", WorkflowRun.State.LEGACY)
            if (matchingRunIds != null) {
                // Embed Long IDs as a SQL literal to bypass PostgreSQL's 65,535 JDBC parameter limit.
                sqlRestriction("this_.id IN (${matchingRunIds.join(',')})")
            }
        }
    }

    @SuppressWarnings('AbcMetric')
    @CompileDynamic
    WorkflowRunSearchResult workflowOverview(WorkflowRunSearchCriteria workflowRunSearchCriteria) {
        String stepFilter = workflowRunSearchCriteria.stepFilter
        List<Long> matchingRunIds = null

        if (stepFilter) {
            matchingRunIds = fetchMatchingRunIds(stepFilter)
            if (matchingRunIds.empty) {
                return new WorkflowRunSearchResult(
                        data: [],
                        workflowsTotal: WorkflowRun.countByStateNotEqual(WorkflowRun.State.LEGACY),
                )
            }
        }

        Closure criteria = getCriteria(workflowRunSearchCriteria.workflow, workflowRunSearchCriteria.states,
                workflowRunSearchCriteria.name, matchingRunIds)
        WorkflowRunSearchResult result = new WorkflowRunSearchResult()

        List<WorkflowRunInfo> workflowRunInfos = WorkflowRun.createCriteria().list {
            criteria.delegate = delegate
            criteria()
            createAlias("workflow", "workflowJoin")
            createAlias("comment", "commentJoin", JoinType.LEFT_OUTER_JOIN)
            projections {
                property("id", "id")
                property("state", "state")
                property("displayName", "displayName")
                property("shortDisplayName", "shortDisplayName")
                property("dateCreated", "dateCreated")
                property("workflowJoin.name", "workflowName")
                property("workflowJoin.deprecatedDate", "workflowDeprecatedDate")
                property("commentJoin.comment", "commentText")
                property("commentJoin.author", "commentAuthor")
                property("commentJoin.modificationDate", "commentModificationDate")
                property("firstJobStarted", "firstJobStarted")
                property("lastJobFinished", "lastJobFinished")
            }
            workflowRunSearchCriteria.orderList.each { DataTablesCommand.Order dtOrder ->
                WorkflowRunListColumn column = WorkflowRunListColumn.fromDataTable(dtOrder.column)
                if (column == WorkflowRunListColumn.COMMENT) {
                    if (dtOrder.direction == DataTablesCommand.Order.Dir.asc) {
                        addOrder(Order.asc("commentJoin.modificationDate").nulls(NullPrecedence.LAST))
                    } else {
                        addOrder(Order.desc("commentJoin.modificationDate").nulls(NullPrecedence.LAST))
                    }
                } else if (column in [WorkflowRunListColumn.FIRST_JOB_STARTED, WorkflowRunListColumn.LAST_JOB_FINISHED]) {
                    if (dtOrder.direction == DataTablesCommand.Order.Dir.asc) {
                        addOrder(Order.asc(column.orderColumn).nulls(NullPrecedence.LAST))
                    } else {
                        addOrder(Order.desc(column.orderColumn).nulls(NullPrecedence.LAST))
                    }
                } else {
                    order(column.orderColumn, dtOrder.direction.name())
                }
            }
            firstResult(workflowRunSearchCriteria.start)
            if (workflowRunSearchCriteria.pagingEnabled) {
                maxResults(workflowRunSearchCriteria.length)
            }
            resultTransformer(Transformers.aliasToBean(WorkflowRunInfo))
        } as List<WorkflowRunInfo>

        Map<Long, List<WorkflowStepInfo>> stepsByRunId = workflowRunInfos ?
                fetchWorkflowStepsForRuns(workflowRunInfos*.id) : [:]

        result.data = workflowRunInfos.collect { WorkflowRunInfo runInfo ->
            List<WorkflowStepInfo> steps = (stepsByRunId[runInfo.id] ?: []).findAll { !it.obsolete }
            WorkflowStepInfo lastStep = steps ? steps.last() : null

            ZonedDateTime durationEnd = runInfo.lastJobFinished ?:
                    (runInfo.state in [WorkflowRun.State.PENDING, WorkflowRun.State.RUNNING_WES, WorkflowRun.State.RUNNING_OTP] ?
                            ZonedDateTime.now() : null)
            String duration = runInfo.firstJobStarted ?
                    (TimeUtils.getFormattedDurationForZonedDateTime(runInfo.firstJobStarted, durationEnd) ?: "-") : "-"

            String comment = runInfo.commentText ?
                    "Author: ${runInfo.commentAuthor}, last modified at: ${TimeFormats.DATE.getFormattedDate(runInfo.commentModificationDate)}," +
                            " comment: ${runInfo.commentText.replaceAll("\n", ", ")}}" : ""

            String workflowDisplayName = "${runInfo.workflowName}${runInfo.workflowDeprecatedDate ? " (deprecated)" : ""}"

            return [
                    state          : runInfo.state,
                    stateDesc      : runInfo.state.description,
                    comment        : comment,
                    workflow       : workflowDisplayName,
                    displayName    : runInfo.displayName,
                    shortName      : runInfo.shortDisplayName,
                    dateCreated    : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(runInfo.dateCreated),
                    lastUpdated    : lastStep?.lastUpdated ? TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(lastStep.lastUpdated) : "",
                    duration       : duration,
                    id             : runInfo.id,
                    step           : lastStep?.beanName,
                    stepId         : lastStep?.id,
                    steps          : (steps - lastStep).reverse()*.beanName,
                    stepIds        : (steps - lastStep).reverse()*.id,
                    firstJobStarted: runInfo.firstJobStarted ? TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedZonedDateTime(runInfo.firstJobStarted) : "",
                    lastJobFinished: runInfo.lastJobFinished ? TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedZonedDateTime(runInfo.lastJobFinished) : "",
            ]
        }

        String runningStates = [WorkflowRun.State.RUNNING_OTP, WorkflowRun.State.RUNNING_WES]
                .collect { "'${it.name()}'" }.join(', ')

        WorkflowRunCounts counts = WorkflowRun.createCriteria().get {
            criteria.delegate = delegate
            criteria()
            projections {
                sqlProjection(
                        "COUNT(*) as workflowsFiltered",
                        "workflowsFiltered",
                        StandardBasicTypes.LONG,
                )
                sqlProjection(
                        "SUM(CASE WHEN state IN (${runningStates}) THEN 1 ELSE 0 END) as running",
                        "running",
                        StandardBasicTypes.LONG,
                )
                sqlProjection(
                        "SUM(CASE WHEN state = '${WorkflowRun.State.FAILED.name()}' THEN 1 ELSE 0 END) as failed",
                        "failed",
                        StandardBasicTypes.LONG,
                )
            }
            resultTransformer(Transformers.aliasToBean(WorkflowRunCounts))
        } as WorkflowRunCounts
        result.workflowsFiltered = counts.workflowsFiltered as int
        result.running = counts.running as int
        result.failed = counts.failed as int
        result.workflowsTotal = WorkflowRun.countByStateNotEqual(WorkflowRun.State.LEGACY)

        return result
    }

    @CompileDynamic
    protected String calculateDuration(WorkflowRun run) {
        if (!run.firstJobStarted) {
            return "-"
        }
        ZonedDateTime end = run.lastJobFinished ?:
                (run.state in [WorkflowRun.State.PENDING, WorkflowRun.State.RUNNING_WES, WorkflowRun.State.RUNNING_OTP] ?
                        ZonedDateTime.now() : null)
        return TimeUtils.getFormattedDurationForZonedDateTime(run.firstJobStarted, end) ?: "-"
    }

    @CompileDynamic
    protected String calculateStepDuration(WorkflowStep step) {
        if (!step.jobStarted) {
            return "-"
        }
        ZonedDateTime end = step.jobFinished ?: ZonedDateTime.now()
        return TimeUtils.getFormattedDurationForZonedDateTime(step.jobStarted, end) ?: "-"
    }

    @CompileDynamic
    WorkflowRun findAllByRestartedFrom(WorkflowRun workflowRun) {
        return CollectionUtils.atMostOneElement(WorkflowRun.findAllByRestartedFrom(workflowRun))
    }

    String getCumulatedClusterJobsStatus(List<ClusterJobStateDto> clusterJobStates) {
        if (!clusterJobStates || clusterJobStates.size() == 0) {
            return ''
        }

        Map<ClusterJob.CheckStatus, Integer> mappingOfCheckStates = [
                (ClusterJob.CheckStatus.CHECKING): 5,
                (ClusterJob.CheckStatus.CREATED) : 3,
                (ClusterJob.CheckStatus.FINISHED): 1,
        ]

        Map<ClusterJob.Status, Integer> mappingOfExitStates = [
                (null as ClusterJob.Status)  : 0,
                (ClusterJob.Status.COMPLETED): 0,
                (ClusterJob.Status.FAILED)   : 1,
        ]

        ClusterJobStateDto highestPriorityClusterJobState = clusterJobStates.max { clusterJobState ->
            mappingOfCheckStates[clusterJobState.checkStatus] + mappingOfExitStates[clusterJobState.exitStatus]
        }

        return mapCheckStatusAndExitStatusToState(highestPriorityClusterJobState.checkStatus, highestPriorityClusterJobState.exitStatus)
    }

    List<Map<String, Object>> workflowRunDetails(WorkflowRun workflowRun) {
        List<WorkflowStep> workflowSteps = workflowRun.workflowSteps.reverse()

        return workflowSteps.collect { WorkflowStep step ->
            List<ClusterJob> clusterJobs = (step.clusterJobs as List<ClusterJob>).sort { it.dateCreated }
            List<WesRun> wesRuns = (step.wesRuns as List<WesRun>).sort { it.dateCreated }

            return [
                    state                    : step.state,
                    id                       : step.id,
                    name                     : step.beanName,
                    dateCreated              : TimeFormats.DATE_TIME.getFormattedDate(step.dateCreated),
                    lastUpdated              : TimeFormats.DATE_TIME.getFormattedDate(step.lastUpdated),
                    jobStarted               : step.jobStarted ? TimeFormats.DATE_TIME.getFormattedZonedDateTime(step.jobStarted) : "",
                    jobFinished              : step.jobFinished ? TimeFormats.DATE_TIME.getFormattedZonedDateTime(step.jobFinished) : "",
                    duration                 : calculateStepDuration(step),
                    error                    : step.workflowError,
                    clusterJobs              : collectClusterJobDetails(clusterJobs),
                    cumulatedClusterJobsState: getCumulatedClusterJobsStatus(clusterJobs.collect { new ClusterJobStateDto(it.checkStatus, it.exitStatus) }),
                    wesRuns                  : collectWesRunDetails(wesRuns),
                    cumulatedWesRunsState    : wesRunService.getCumulatedWesRunsStatus(wesRuns.collect { new WesRunStateDto(it.state, it.wesRunLog?.state) }),
                    hasLogs                  : !workflowLogService.findAllByWorkflowStepInCorrectOrder(step).empty,
                    obsolete                 : step.obsolete,
                    previousStepId           : workflowStepService.getPreviousRunningWorkflowStep(step)?.id,
            ]
        }
    }

    private List<Map<String, Object>> collectClusterJobDetails(List<ClusterJob> clusterJobs) {
        return clusterJobs.collect { ClusterJob clusterJob ->
            [
                    state   : mapCheckStatusAndExitStatusToState(clusterJob.checkStatus, clusterJob.exitStatus),
                    id      : clusterJob.id,
                    name    : clusterJob.clusterJobName,
                    jobId   : clusterJob.clusterJobId,
                    hasLog  : clusterJobService.isClusterJobLogPathSet(clusterJob),
                    node    : clusterJob.node ?: "-",
                    wallTime: clusterJobDetailService.getElapsedWalltimeAsHhMmSs(clusterJob),
                    exitCode: clusterJob.exitCode ?: "-",
            ]
        } as List<Map<String, Object>>
    }

    private List<Map<String, Object>> collectWesRunDetails(List<WesRun> wesRuns) {
        return wesRuns.collect { WesRun wesRun ->
            [
                    id           : wesRun.id,
                    wesIdentifier: wesRun.wesIdentifier,
                    state        : wesRun.wesRunLog?.state ?: State.UNKNOWN,
                    hasReport    : wesRunService.hasReports(wesRun),
                    exitCode     : wesRun.wesRunLog?.runLog?.exitCode ?: "-",
            ]
        } as List<Map<String, Object>>
    }

    private static String mapCheckStatusAndExitStatusToState(ClusterJob.CheckStatus checkStatus, ClusterJob.Status exitStatus) {
        return "${checkStatus}${checkStatus == ClusterJob.CheckStatus.FINISHED ? "/${exitStatus}" : ""}"
    }
}

@TupleConstructor
class ClusterJobStateDto {
    ClusterJob.CheckStatus checkStatus
    ClusterJob.Status exitStatus
}
