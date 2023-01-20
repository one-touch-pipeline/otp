/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.processing

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.*

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Transactional
class JobExecutionPlanService {

    /**
     * Dependency Injection of Grails Application.
     * Needed to resolve job beans for introspection.
     */
    def grailsApplication

    /**
     * Security aware way to access a JobExecutionPlan.
     * @param id The JobExecutionPlan's id
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR')")
    JobExecutionPlan getPlan(long id) {
        return JobExecutionPlan.get(id)
    }

    /**
     * Security aware way to access a JobDefinition.
     * @param id The JobDefinition's id
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR')")
    JobDefinition getJobDefinition(long id) {
        return JobDefinition.get(id)
    }

    /**
     * Enables the given JobExecutionPlan.
     * In case the plan is obsoleted, it cannot be enabled.
     * @param plan The JobExecutionPlan to enable.
     * @return The enabled state after the operation. Should be true on success.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean enablePlan(JobExecutionPlan plan) {
        if (plan.obsoleted) {
            return false
        }
        return changePlanStatus(plan, true)
    }

    /**
     * Disables the given JobExecutionPlan.
     * @param plan The JobExecutionPlan to disable.
     * @return The enabled state after the operation. Should be false(!) on success.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean disablePlan(JobExecutionPlan plan) {
        return changePlanStatus(plan, false)
    }

    private boolean changePlanStatus(JobExecutionPlan plan, boolean enable) {
        boolean before = plan.enabled
        plan.enabled = enable
        plan.save(flush: true)
        if (!plan) {
            log.error("JobExecutionPlan ${plan.id} could not be ${enable ? 'enabled' : 'disabled'}")
            return before
        }
        JobExecutionPlanChangedEvent event = new JobExecutionPlanChangedEvent(this, plan.id)
        grailsApplication.mainContext.publishEvent(event)
        return plan.enabled
    }

    /**
     * Retrieves a list of all JobExecutionPlans the current User has access to.
     * Obsoleted JobExecutionPlans are not considered.
     * @return List of all not obsoleted JobExecutionPlans
     */
    @PostFilter("hasRole('ROLE_OPERATOR')")
    List<JobExecutionPlan> jobExecutionPlans() {
        return JobExecutionPlan.findAllByObsoleted(false, [sort: "name", order: "asc"])
    }

    @PostFilter("hasRole('ROLE_OPERATOR')")
    List<JobExecutionPlan> jobExecutionPlansWithPreviousVersions() {
        return JobExecutionPlan.findAllByNameInList(jobExecutionPlans()*.name)
    }

    /**
     * Retrieves all Processes for the given JobExecutionPlan.
     * @param plan The Plan whose Processes should be retrieved
     * @param max The number of elements to retrieve, default 10
     * @param offset The offset in the list, default -
     * @param column The column to search for, default "id"
     * @param order {@code true} for ascending ordering, {@code false} for descending, default {@code false}
     * @return List of all Processes run for the JobExecutionPlan filtered as requested
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Process> getAllProcesses(JobExecutionPlan plan, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        final List<JobExecutionPlan> plans = withParents(plan)
        return plans ? Process.findAllByJobExecutionPlanInList(plans, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"]) : []
    }

    /**
     * Retrieves all Processes for the given JobExecutionPlan together with their latests Update.
     * This method returns a map of all Processes for the given JobExecutionPlan as the key values
     * and the latest ProcessingStepUpdate for the Processes as values.
     * It is possible to restrict the returned Processes. The most important one is probably the
     * restriction on execution state. Using this one will filter out all Processes whose latest
     * update is not in the requested state.
     * @param plan The JobExecutionPlan for which the data should be retrieved
     * @param max The maximum number of Processes to retrieve
     * @param offset The offset in the list of Processes to retrieve
     * @param column The column for searching
     * @param order The sort order, true for ascending, false for descending
     * @param state The execution state for restricting the result
     * @return Map of Processes with latest ProcessingStepUpdate
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<Process, ProcessingStepUpdate> getLatestUpdatesForPlan(
            JobExecutionPlan plan, int max = 10, int offset = 0, String column = "id", boolean order = false, List<ExecutionState> states = []) {
        final List<Long> plans = withParents(plan)*.id
        String query = """
        SELECT
            p, max(u.id)
        FROM
            ProcessingStepUpdate as u
            INNER JOIN u.processingStep as step
            INNER JOIN step.process as p
            INNER JOIN p.jobExecutionPlan as plan
        WHERE
            plan.id in (:planIds)
            AND step.next IS NULL
            AND NOT EXISTS (
                SELECT
                    u2.id
                FROM
                    ProcessingStepUpdate AS u2
                WHERE
                    u2.previous = u
            )
        """
        if (states) {
            query = query + "AND u.state IN (:states)\n"
        }
        query = query + "GROUP BY p.id\n"
        query = query + "ORDER BY p.${column} ${order ? 'asc' : 'desc'}"

        Map<Process, ProcessingStepUpdate> results = [:]
        Map params = [planIds: plans]
        if (states) {
            params.put("states", states)
        }
        def processes = ProcessingStepUpdate.executeQuery(query.toString(), params, [max: max, offset: offset])
        List<Long> ids = processes.collect { it[1] }
        List<ProcessingStepUpdate> updates = ids ? ProcessingStepUpdate.findAllByIdInList(ids) : []
        processes.each {
            results.put(it[0] as Process, updates.find { update -> update.id == it[1] })
        }

        return results
    }

    /**
     * Returns the number of Processes run for the given JobExecutionPlan.
     * @param plan The Plan for which the number of run Processes should be returned
     * @return The number of Processes run for this plan
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    int getProcessCount(JobExecutionPlan plan) {
        final List<JobExecutionPlan> plans = withParents(plan)
        return plans ? Process.countByJobExecutionPlanInList(plans) : 0
    }

    /**
     * Returns whether at least one Process is currently running for the given JobExecutionPlan.
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which it should be checked whether a Process is running
     * @return {@code true} in case there is a Process running for plan, {@code false} otherwise
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean isProcessRunning(JobExecutionPlan plan) {
        final List<JobExecutionPlan> plans = withParents(plan)
        final Process process = plans ? CollectionUtils.atMostOneElement(Process.findAllByFinishedAndJobExecutionPlanInList(false, plans)) : null
        return (process != null)
    }

    /**
     * Returns the last Process which has been created for the given JobExecutionPlan.
     * If no Process exists for the given JobExecutionPlan {@code null} is returned. This is either
     * a Process which is running or a Process which has been finished (either successfully or failed).
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which the last created Process should be returned
     * @return The last created Process, or {@code null} if none is available
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Process getLastExecutedProcess(JobExecutionPlan plan) {
        final List<Long> plans = withParents(plan)*.id
        String query = """
        SELECT
            p
        FROM
            ProcessingStepUpdate AS u
            INNER JOIN u.processingStep AS step
            INNER JOIN step.process AS p
            INNER JOIN p.jobExecutionPlan AS plan
        WHERE
            plan.id IN (:planIds)
            AND u.state = 'CREATED'
            AND step.previous IS NULL
        ORDER BY
            p.id DESC
        """

        List result = Process.executeQuery(query.toString(), [planIds: plans], [max: 1])
        if (result.isEmpty()) {
            return null
        }
        return result[0]
    }

    /**
     * Returns the number of Processes which have been started for the given JobExecutionPlan.
     * Both finished and running Processes are considered. The method also considers the previous,
     * but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which the number of started processes should be retrieved.
     * @param state Optional ExecutionState to restrict the number of Processes returned
     * @return The number of Processes which have been started for plan
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    int getNumberOfProcesses(JobExecutionPlan plan, List<ExecutionState> states = null) {
        final List<JobExecutionPlan> plans = withParents(plan)
        if (!states) {
            return plans ? Process.countByJobExecutionPlanInList(plans) : 0
        }
        String query = """
        SELECT
            COUNT(DISTINCT p.id)
        FROM
            ProcessingStepUpdate AS u
            INNER JOIN u.processingStep as step
            INNER JOIN step.process as p
            INNER JOIN p.jobExecutionPlan as plan
        WHERE
            plan.id in (:planIds)
            AND step.next IS NULL
            AND u.state in (:states)
            AND NOT EXISTS (
                SELECT
                    u2.id
                FROM
                    ProcessingStepUpdate AS u2
                WHERE
                    u2.previous = u
            )
        """
        return Process.executeQuery(query.toString(), [planIds: plans*.id, states: states])[0] as int
    }

    /**
     * Generates some information about the given Plan.
     * @param plan The JobExecutionPlan for which the information should be extracted.
     * @return Plan Information in a JSON ready format
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    PlanInformation planInformation(JobExecutionPlan plan) {
        return PlanInformation.fromPlan(plan)
    }

    /**
     * Retrieves all JobDefinitions for the given JobExecutionPlan
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<JobDefinition> jobDefinitions(JobExecutionPlan plan) {
        List<JobDefinition> jobs = []
        JobDefinition job = plan.firstJob
        while (job) {
            jobs << job
            job = job.next
        }
        return jobs
    }

    /**
     * Returns a list of JobExecutionPlans containing the passed in plan and all it's obsoleted
     * previous plans.
     *
     * Method is protected to be tested in Unit tests.
     *
     * Warning: This method is recursive
     * @param plan The plan for which all parents should be retrieved
     */
    protected List<JobExecutionPlan> withParents(JobExecutionPlan plan) {
        if (plan.previousPlan) {
            return withParents(plan.previousPlan) << plan
        }
        return [plan]
    }

    /**
     * Number of processes of each workflow
     * return Map of job execution plan names -> all processes count
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<String, Long> processCount(List<JobExecutionPlan> plans = []) {
        Process.createCriteria().list {
            createAlias("jobExecutionPlan", "jep")
            if (plans) {
                'in'("jobExecutionPlan", plans)
            }
            projections {
                groupProperty("jep.name")
                count("id")
            }
            order("jep.name", "asc")
        }.collectEntries { e ->
            [e[0], e[1]]
        }
    }

    /**
     * Number of finished processes of each workflow
     * @return Map of job execution plan names -> finished processes
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<String, Long> finishedProcessCount(List<JobExecutionPlan> plans = []) {
        Process.createCriteria().list {
            createAlias("jobExecutionPlan", "jep")
            if (plans) {
                'in'("jobExecutionPlan", plans)
            }
            eq("finished", true)
            projections {
                groupProperty("jep.name")
                count("id")
            }
            order("jep.name", "asc")
        }.collectEntries { e ->
            [e[0], e[1]]
        }
    }

    /**
     * Number of failed processes of each workflow
     * @return Map of job execution plan names -> failed processes
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<String, Long> failedProcessCount(List<JobExecutionPlan> plans) {
        if (!plans) {
            return [:]
        }

        String query = """
        SELECT
            plan.name,
            COUNT(DISTINCT p.id)
        FROM
            ProcessingStepUpdate as u
            INNER JOIN u.processingStep as step
            INNER JOIN step.process as p
            INNER JOIN p.jobExecutionPlan as plan
        WHERE
            plan.id IN (${plans*.id.join(", ")})
            AND step.next IS NULL
            AND u.state = '${ExecutionState.FAILURE}'
            AND NOT EXISTS (
                SELECT
                    u2.id
                FROM
                    ProcessingStepUpdate AS u2
                WHERE
                    u2.previous = u
            )
        GROUP BY
            plan.name
        """
        return Process.executeQuery(query.toString(), []).collectEntries { e ->
            [e[0], e[1]]
        }
    }

    /**
     * Last process with given state of each workflow
     * @return Map of job execution plan names -> date of last process with given state
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<String, Date> lastProcessDate(List<JobExecutionPlan> plans, ExecutionState state) {
        ProcessingStepUpdate.createCriteria().list {
            createAlias("processingStep.process.jobExecutionPlan", "jep")
            processingStep {
                isNull("next")
                process {
                    'in'("jobExecutionPlan", plans)
                    eq("finished", true)
                }
            }
            eq("state", state)
            projections {
                groupProperty("jep.name")
                max("date")
            }
            order("jep.name", "asc")
        }.collectEntries { e ->
            [e[0], e[1]]
        }
    }
}
