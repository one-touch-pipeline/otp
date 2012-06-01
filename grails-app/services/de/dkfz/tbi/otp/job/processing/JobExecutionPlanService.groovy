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
     * Dependency Injection of Grails Application.
     * Needed to resolve job beans for introspection.
     **/
    def grailsApplication

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
     * Enables the given JobExecutionPlan.
     * In case the plan is obsoleted, it cannot be enabled.
     * @param plan The JobExecutionPlan to enable.
     * @return The enabled state after the operation. Should be true on success.
     **/
    @PreAuthorize("hasPermission(#plan, write) or hasRole('ROLE_ADMIN')")
    public boolean enablePlan(JobExecutionPlan plan) {
        if (plan.obsoleted) {
            return false
        }
        boolean before = plan.enabled
        plan.enabled = true
        plan = plan.save(flush: true)
        if (!plan) {
            log.error("JobExecutionPlan ${plan.id} could not be enabled")
            return before
        }
        return plan.enabled
    }

    /**
     * Disables the given JobExecutionPlan.
     * @param plan The JobExecutionPlan to disable.
     * @return The enabled state after the operation. Should be false(!) on success.
     **/
    @PreAuthorize("hasPermission(#plan, write) or hasRole('ROLE_ADMIN')")
    public boolean disablePlan(JobExecutionPlan plan) {
        boolean before = plan.enabled
        plan.enabled = false
        plan = plan.save(flush: true)
        if (!plan) {
            log.error("JobExecutionPlan ${plan.id} could not be disabled")
            return before
        }
        return plan.enabled
    }

    /**
     * Retrieves a list of all JobExecutionPlans the current User has access to.
     * Obsoleted JobExecutionPlans are not considered.
     * @return List of all not obsoleted JobExecutionPlans
    **/
    @PostFilter("hasPermission(filterObject, read) or hasRole('ROLE_ADMIN')")
    public List<JobExecutionPlan> getAllJobExecutionPlans() {
        return JobExecutionPlan.findAllByObsoleted(false, [sort: "id", order: "desc"])
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
    public Map<Process, ProcessingStepUpdate> getLatestUpdatesForPlan(JobExecutionPlan plan, int max = 10, int offset = 0, String column = "id", boolean order = false, ExecutionState state = null) {
        final List<Long> plans = withParents(plan).collect { it.id }
        String query = '''
SELECT p, max(u.id)
FROM ProcessingStepUpdate as u
INNER JOIN u.processingStep as step
INNER JOIN step.process as p
INNER JOIN p.jobExecutionPlan as plan
WHERE plan.id in (:planIds)
'''
        if (state) {
            query = query + "AND u.state = :state\n"
        }
        query = query + "GROUP BY p.id\n"
        query = query + "ORDER BY p.${column} ${order ? 'asc' : 'desc'}"

        LinkedHashMap<Process, ProcessingStepUpdate> results = new LinkedHashMap<Process, ProcessingStepUpdate>()
        Map params = [planIds: plans]
        if (state) {
            params.put("state", state)
        }
        def processes = ProcessingStepUpdate.executeQuery(query, params, [max: max, offset: offset])
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
        final List<Long> plans = withParents(plan).collect { it.id }
        String query = '''
SELECT p
FROM ProcessingStepUpdate AS u
INNER JOIN u.processingStep AS step
INNER JOIN step.process AS p
INNER JOIN p.jobExecutionPlan AS plan
WHERE
plan.id IN (:planIds)
AND u.state = 'CREATED'
AND step.previous IS NULL
ORDER BY p.id DESC
'''

        List result = Process.executeQuery(query, [planIds: plans], [max: 1])
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
    **/
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public int getNumberOfProcesses(JobExecutionPlan plan, ExecutionState state = null) {
        final List<JobExecutionPlan> plans = withParents(plan)
        if (!state) {
            return Process.countByJobExecutionPlanInList(plans)
        }
        String query = '''
SELECT COUNT(DISTINCT p.id)
FROM ProcessingStepUpdate AS u
INNER JOIN u.processingStep as step
INNER JOIN step.process as p
INNER JOIN p.jobExecutionPlan as plan
WHERE plan.id in (:planIds)
AND step.next IS NULL
AND u.state = :state
AND u.id IN (
    SELECT MAX(u2.id)
    FROM ProcessingStepUpdate AS u2
    INNER JOIN u2.processingStep as step2
    INNER JOIN step2.process as p2
    INNER JOIN p2.jobExecutionPlan as plan2
    WHERE plan2.id in (:planIds)
    AND step2.next IS NULL
    GROUP BY p2.id
)
'''
        return Process.executeQuery(query, [planIds: plans.collect { it.id }, state: state])[0]
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
     * Generates some information about the given Plan.
     * Returns a Map with three elements "jobs", "connections" and "name".
     * Jobs is a List containing the information about each Job in this plan as a map with the following fields:
     * <ul>
     * <li>id (long)</li>
     * <li>name (string)</li>
     * <li>startJob (boolean)</li>
     * <li>endStateAware (boolean)</li>
     * <li>pbsJob (boolean)</li>
     * <li>constantParameters (list)</li>
     * <li>inputParameters (list)</li>
     * <li>outputParameters (list)</li>
     * <li>passthroughParameters (list)</li>
     * </ul>
     *
     * The lists of parameters contain objects of the following structure:
     * <ul>
     * <li>id (long) (only provided for constant parameter)</li>
     * <li>value (string) (only provided for constant parameter)</li>
     * <li>type (map)</li>
     * <li>mapping (long) (only provided for input and passthrough parameter)</li>
     * </ul>
     *
     * The type is a complex structure consiting of the following fields:
     * <ul>
     * <li>id (long) (referrenced by mapping)</li>
     * <li>name (string)</li>
     * <li>description (string)</li>
     * <li>className (string)</li>
     * </ul>
     *
     * Connections is a List containing the connections between two jobs as a map with fields "from" and "to"
     * taking just the id of the Job in the "jobs" field.
     * @param plan The JobExecutionPlan for which the information should be extracted.
     * @return Plan Information in a JSON ready format
     */
    @PreAuthorize("hasPermission(#plan, read) or hasRole('ROLE_ADMIN')")
    public Map planInformation(JobExecutionPlan plan) {
        List jobs = []
        List connections = []
        def addToJobs = { job ->
            def bean = grailsApplication.mainContext.getBean(job.bean)
            boolean startJob = (bean instanceof StartJob)
            boolean endStateAware = (bean instanceof EndStateAwareJob)
            boolean pbsJob = (bean instanceof PbsJob)
            List constantParameters = []
            List outputParameters = []
            List inputParameters = []
            List passthroughParameters = []
            List parameterMappings = []
            def typeToMap = {
                [
                    id: it.id,
                    name: it.name,
                    description: it.description,
                    className: it.className
                ]
            }
            job.constantParameters.each { param ->
                constantParameters << [
                    id: param.id,
                    value: param.value,
                    type: typeToMap(param.type)
                ]
            }
            ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.INPUT).each {
                inputParameters << [type: typeToMap(it), mapping: ParameterMapping.findByJobAndTo(job, it)?.from?.id]
            }
            ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.OUTPUT).each {
                outputParameters << [type: typeToMap(it)]
            }
            ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.PASSTHROUGH).each {
                passthroughParameters << [type: typeToMap(it), mapping: ParameterMapping.findByJobAndTo(job, it)?.from?.id]
            }
            jobs << [
                id: job.id,
                name: job.name,
                startJob: startJob,
                endStateAware: endStateAware,
                pbsJob: pbsJob,
                bean: job.bean,
                constantParameters: constantParameters,
                inputParameters: inputParameters,
                outputParameters: outputParameters,
                passthroughParameters: passthroughParameters
            ]
        }
        def addConnection = { from, to ->
            connections << [from: from.id, to: to.id]
        }
        addToJobs(plan.startJob)
        addConnection(plan.startJob, plan.firstJob)
        JobDefinition job = plan.firstJob
        while (job) {
            addToJobs(job)
            if (job.next) {
                addConnection(job, job.next)
            }
            job = job.next
        }
        return [jobs: jobs, connections: connections, name: plan.name]
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
AND p.finished = true
GROUP BY p.id, u.id, u.date
HAVING u.date = MAX(u.date)
ORDER BY MAX(u.date) desc
'''
        List results = Process.executeQuery(query, [state: state, planIds: planIds], [max: 1])
        if (results.isEmpty()) {
            return null
        }
        return results[0]
    }
}
