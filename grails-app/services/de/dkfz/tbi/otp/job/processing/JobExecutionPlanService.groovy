package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PostFilter

/**
 * Service providing methods to access information about JobExecutionPlans.
 *
 */
class JobExecutionPlanService {
    static transactional = true
    static final profiled = true

    /**
     * Security aware way to access a JobExecutionPlan.
     * @param id The JobExecutionPlan's id
     * @return
     */
    @PostAuthorize("hasPermission(returnObject, read) or hasRole('ROLE_ADMIN')")
    public JobExecutionPlan getPlan(long id) {
        return JobExecutionPlan.get(id)
    }

    /**
     * Retrieves a list of all JobExecutionPlans the current User has access to.
     * Obsoleted JobExecutionPlans are not considered.
     * @return List of all not obsoleted JobExecutionPlans
    **/
    @PostFilter("hasPermission(filterObject, read) or hasRole('ROLE_ADMIN')")
    public List<JobExecutionPlan> getAllJobExecutionPlans() {
        return JobExecutionPlan.findAllByObsoleted(false)
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
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public List<Process> getAllProcesses(JobExecutionPlan plan, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        final List<JobExecutionPlan> plans = withParents(plan)
        return Process.findAllByJobExecutionPlanInList(plans, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public Map<Process, ProcessingStepUpdate> getLatestUpdatesForPlan(JobExecutionPlan plan, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        final List<Long> plans = withParents(plan).collect { it.id }
        String query = '''
SELECT p, max(u.id)
FROM ProcessingStepUpdate as u
INNER JOIN u.processingStep as step
INNER JOIN step.process as p
INNER JOIN p.jobExecutionPlan as plan
WHERE plan.id in (:planIds)
GROUP BY p.id
'''
        query = query + "ORDER BY p.${column} ${order ? 'asc' : 'desc'}"

        LinkedHashMap<Process, ProcessingStepUpdate> results = new LinkedHashMap<Process, ProcessingStepUpdate>()
        def processes = ProcessingStepUpdate.executeQuery(query, [planIds: plans], [max: max, offset: offset])
        List<Long> ids = processes.collect { it[1] }
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByIdInList(ids)
        processes.each {
            results.put(it[0] as Process, updates.find { update -> update.id == it[1] } )
        }

        return results
    }

    /**
     * Returns the number of Processes run for the given JobExecutionPlan.
     * @param plan The Plan for which the number of run Processes should be returned
     * @return The number of Processes run for this plan
     */
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public int getProcessCount(JobExecutionPlan plan) {
        final List<JobExecutionPlan> plans = withParents(plan)
        return Process.countByJobExecutionPlanInList(plans)
    }

    /**
     * Returns whether at least one Process is currently running for the given JobExecutionPlan.
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which it should be checked whether a Process is running
     * @return {@code true} in case there is a Process running for plan, {@code false} otherwise
    **/
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public boolean isProcessRunning(JobExecutionPlan plan) {
        final List<JobExecutionPlan> plans = withParents(plan)
        final Process process = Process.findByFinishedAndJobExecutionPlanInList(false, plans)
        return (process != null)
    }

    /**
     * Returns the last Process which executed successfully for the given JobExecutionPlan.
     * If there is no successful executed Process for the given JobExecutionPlan {@code null} is returned.
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which the last successful Process should be returned
     * @return The last successful executed Process, or {@code null} if none is available
     **/
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public Process getLastSucceededProcess(JobExecutionPlan plan) {
        return lastProcessWithState(plan, ExecutionState.SUCCESS)
    }

    /**
     * Returns the last Process whose execution failed for the given JobExecutionPlan.
     * If there is no failed Process for the given JobExecutionPlan {@code null} is returned.
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which the last failed Process should be returned
     * @return The last failed Process, or {@code null} if none is available
     **/
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public Process getLastFailedProcess(JobExecutionPlan plan) {
        return lastProcessWithState(plan, ExecutionState.FAILURE)
    }

    /**
     * Returns the last Process whose execution finished for the given JobExecutionPlan.
     * If there is no finished Process (either success or failure) for the given JobExecutionPlan
     * {@code null} is returned.
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which the last finished Process should be returned
     * @return The last finished Process, or {@code null} if none is available
     **/
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public Process getLastFinishedProcess(JobExecutionPlan plan) {
        final Process success = getLastSucceededProcess(plan)
        final Process failure = getLastFailedProcess(plan)
        // neither failed nor success - return null
        if (!success && !failure) {
            return null
        }
        // no success, but a failure, so failure is last finished
        if (!success) {
            return failure
        }
        // no failure, but a success, so success is last finished
        if (!failure) {
            return success
        }
        // both are present, so take most recent one, if both have same end time use success
        final List<ProcessingStep> allSteps = ProcessingStep.findAllByProcessInList([success, failure])
        List<ProcessingStep> lastSteps = []
        allSteps.each {
            if (!it.next) {
                lastSteps << it
            }
        }
        return ProcessingStepUpdate.findAllByProcessingStepInList(lastSteps).sort { it.date }.last().processingStep.process
    }

    /**
     * Returns the last Process which has been created for the given JobExecutionPlan.
     * If no Process exists for the given JobExecutionPlan {@code null} is returned. This is either
     * a Process which is running or a Process which has been finished (either successfully or failed).
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which the last created Process should be returned
     * @return The last created Process, or {@code null} if none is available
    **/
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public Process getLastExecutedProcess(JobExecutionPlan plan) {
        final List<JobExecutionPlan> plans = withParents(plan)
        List<JobDefinition> firstJobs = []
        plans.each { JobExecutionPlan p ->
            firstJobs << p.firstJob
        }
        final List<ProcessingStep> steps = ProcessingStep.findAllByJobDefinitionInList(firstJobs)
        final List<ProcessingStepUpdate> created = ProcessingStepUpdate.findAllByStateAndProcessingStepInList(ExecutionState.CREATED, steps)
        if (created.isEmpty()) {
            return null
        }
        return created.sort { it.date }.last().processingStep.process
    }

    /**
     * Returns the number of Processes which have been started for the given JobExecutionPlan.
     * Both finished and running Processes are considered. The method also considers the previous,
     * but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which the number of started processes should be retrieved.
     * @return The number of Processes which have been started for plan
    **/
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public int getNumberOfProcesses(JobExecutionPlan plan) {
        final List<JobExecutionPlan> plans = withParents(plan)
        return Process.countByJobExecutionPlanInList(plans)
    }

    /**
     * Returns the number of successful finished Processes for the given JobExecutionPlan.
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * @param plan The JobExecutionPlan for which the number of successful finished Processes should be retrieved.
     * @return The number of successful finished Processes
     */
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public int getNumberOfSuccessfulFinishedProcesses(JobExecutionPlan plan) {
        final List<JobExecutionPlan> plans = withParents(plan)
        int count = 0
        plans.each {
            if (it.finishedSuccessful) {
                count += it.finishedSuccessful
            }
        }
        return count
    }

    /**
     * Returns the number of finished Processes (either successful or failed) for the given JobExecutionPlan.
     * The method also considers the previous, but obsoleted JobExecutionPlans for the given plan.
     * The method differs to getNumberOfProcesses by not considering running Processes
     * @param plan The JobExecutionPlan for which the number of finished Processes should be retrieved.
     * @return The number of finished Processes
     * @see getNumberOfProcesses
     */
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public int getNumberOfFinishedProcesses(JobExecutionPlan plan) {
        final List<JobExecutionPlan> plans = withParents(plan)
        return Process.countByFinishedAndJobExecutionPlanInList(true, plans)
    }

    /**
     * Returns a list of JobExecutionPlans containing the passed in plan and all it's obsoleted
     * previous plans.
     *
     * Method is protected to be tested in Unit tests.
     *
     * Warning: This method is recursive
     * @param plan The plan for which all parents should be retrieved
     * @return
     */
    protected List<JobExecutionPlan> withParents(JobExecutionPlan plan) {
        if (plan.previousPlan) {
            return withParents(plan.previousPlan) << plan
        } else {
            return [plan]
        }
    }

    private Process lastProcessWithState(JobExecutionPlan plan, ExecutionState state) {
        final List<Long> planIds = withParents(plan).collect { it.id }
        // Selects for the given JobExecutionPlans all processes
        // Restricts them on the last run ProcessingStep
        // selects the ProcessingStepUpdate for these steps with the maximum id
        // and restricts on the requested execution state
        // orders by the update id in descending way and takes the first list element
        // and that's the Process we are looking for.
        String query =
'''
SELECT p
FROM ProcessingStepUpdate AS u
INNER JOIN u.processingStep AS step
INNER JOIN step.process AS p
INNER JOIN p.jobExecutionPlan AS plan
WHERE
step.next IS NULL
AND u.state = :state
AND plan.id in (:planIds)
GROUP BY p.id, u.id
HAVING u.id = MAX(u.id)
ORDER BY MAX(u.id) desc
'''
        List results = Process.executeQuery(query, [state: state, planIds: planIds], [max: 1])
        if (results.isEmpty()) {
            return null
        }
        return results[0]
    }
}
