package de.dkfz.tbi.otp.infrastructure

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.tools.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import groovy.sql.*
import org.joda.time.*

import javax.sql.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static java.util.concurrent.TimeUnit.*

class ClusterJobService {

    DataSource dataSource

    public static final String FORMAT_STRING = "yyyy-MM-dd HH:mm:ss"
    public static final Long HOURS_TO_MILLIS = HOURS.toMillis(1)
    // we assume that jobs with an elapsed walltime under 10ms "obviously failed"
    public static final Duration DURATION_JOB_OBVIOUSLY_FAILED = Duration.millis(9)

    private static final QUERY_BY_TIMESPAN = """
 job.queued >= ?
 AND job.ended < ?
 AND job.ended > job.started
"""

    private static final QUERY_BY_TIMESPAN_NOTFAILED = """
 ${QUERY_BY_TIMESPAN}
 AND job.exit_status != 'FAILED'
"""

    private static final QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE = """
 ${QUERY_BY_TIMESPAN}
 AND job.job_class = ?
 AND job.seq_type_id = ?
"""

    private static final QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED = """
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE}
 AND job.exit_status != 'FAILED'
"""

    /**
     * creates a cluster job object with at this time known attributes
     */
    public ClusterJob createClusterJob(Realm realm, String clusterJobId, String userName,
                                       ProcessingStep processingStep, SeqType seqType = null,
                                       String clusterJobName = processingStep.getClusterJobName(),
                                       String jobClass = processingStep.nonQualifiedJobClass) {
        ClusterJob job = new ClusterJob(
                processingStep: processingStep,
                individual: processingStep.processParameterObject?.individual,
                realm: realm,
                clusterJobId: clusterJobId,
                userName: userName,
                clusterJobName: clusterJobName,
                jobClass: jobClass,
                seqType: seqType,
                queued: new DateTime(),
        ).save(flush: true)
        assert job != null
        return job
    }

    /**
     * Stores values for statistics after the job has been sent
     */
    public void amendClusterJob(ClusterJob job, GenericJobInfo jobInfo) {
        job.requestedCores = jobInfo.askedResources?.cores
        job.requestedWalltime = convertFromJava8DurationToJodaDuration(jobInfo?.askedResources?.walltime)
        job.requestedMemory = jobInfo.askedResources?.mem?.toLong(BufferUnit.k)

        job.jobLog = jobInfo.logFile
        job.accountName = jobInfo.account
        job.dependencies = jobInfo.parentJobIDs ? jobInfo.parentJobIDs.collect { ClusterJob.findByClusterJobId(it) } : []

        assert job.save(flush: true, failOnError: true)
    }

    /**
     * Stores values for statistics after the job has finished
     */
    public void completeClusterJob(
            ClusterJobIdentifier jobIdentifier,
            ClusterJob.Status status,
            GenericJobInfo jobInfo
    ) {
        ClusterJob job
        if (jobIdentifier.realm != null) {
            job = ClusterJob.findByClusterJobIdentifier(jobIdentifier)
        } else {
            job = exactlyOneElement(ClusterJob.findAllByClusterJobId(jobIdentifier.clusterJobId))
        }

        job.exitStatus = status
        job.exitCode = jobInfo.exitCode

        if (job.queued && jobInfo.submitTime) {
            job.queued = convertFromJava8ZonedDateTimeToJodaDateTime(jobInfo.submitTime)
        }
        job.eligible = convertFromJava8ZonedDateTimeToJodaDateTime(jobInfo.eligibleTime)
        job.started = convertFromJava8ZonedDateTimeToJodaDateTime(jobInfo.startTime)
        job.ended = convertFromJava8ZonedDateTimeToJodaDateTime(jobInfo.endTime)
        job.systemSuspendStateDuration = convertFromJava8DurationToJodaDuration(jobInfo.timeSystemSuspState)
        job.userSuspendStateDuration = convertFromJava8DurationToJodaDuration(jobInfo.timeUserSuspState)

        job.cpuTime = convertFromJava8DurationToJodaDuration(jobInfo.cpuTime)
        job.usedCores = jobInfo.usedResources?.cores
        job.usedMemory = jobInfo.usedResources?.mem?.toLong(BufferUnit.k)
        job.usedSwap = jobInfo.usedResources?.swap?.toLong(BufferUnit.k)

        job.node = jobInfo.executionHosts?.unique()?.sort()?.join(",")
        job.startCount = jobInfo.startCount

        job.xten = isXten(job)
        job.nBases = getBasesSum(job)
        job.nReads = getReadsSum(job)
        job.fileSize = getFileSizesSum(job)

        assert job.save(flush: true, failOnError: true)

        handleObviouslyFailedClusterJob(job)
    }

    private static DateTime convertFromJava8ZonedDateTimeToJodaDateTime(java.time.ZonedDateTime dateTime) {
        return dateTime ?
                new DateTime(dateTime.year, dateTime.monthValue, dateTime.dayOfMonth, dateTime.hour,
                        dateTime.minute, dateTime.second, DateTimeZone.forID(dateTime.zone.id))
                : null
    }

    private static Duration convertFromJava8DurationToJodaDuration(java.time.Duration duration) {
        return duration ? Duration.millis(duration.toMillis()) : null
    }

    /**
     * returns the specific workflow object to a cluster job, e.g. Run, AlignmentPass
     * @return Object or null
     */
    public static ProcessParameterObject findProcessParameterObjectByClusterJob(ClusterJob job) {
        return job.processingStep.processParameterObject
    }

    /**
     * returns true if a job belongs to data that is sequenced by X-Ten machines
     */
    public static Boolean isXten(ClusterJob job) {
        ProcessParameterObject workflowObject = findProcessParameterObjectByClusterJob(job)
        List<SeqPlatformModelLabel> seqPlatformModelLabels = workflowObject.getContainedSeqTracks().toList()*.seqPlatform.seqPlatformModelLabel
        if (seqPlatformModelLabels*.id.unique().size() == 1 && seqPlatformModelLabels.first() != null) {
            return seqPlatformModelLabels.first().name == "HiSeq X Ten"
        }
        return null
    }

    /**
     * returns the sum of bases of all {@link de.dkfz.tbi.otp.ngsdata.SeqTrack} that belong to this job
     */
    public static Long getBasesSum(ClusterJob job) {
        return normalizePropertyToClusterJobs(job) { ProcessParameterObject workflowObject ->
            workflowObject.getContainedSeqTracks()?.sum { it.nBasePairs ?: 0 }
        }
    }

    /**
     * returns the sum of file sizes of all {@link DataFile} that belong to this job
     */
    public static Long getFileSizesSum(ClusterJob job) {
        return normalizePropertyToClusterJobs(job) { ProcessParameterObject workflowObject ->
            DataFile.findAllBySeqTrackInList(workflowObject.getContainedSeqTracks().asList())?.sum { it.fileSize }
        }
    }

    /**
     * returns the sum of reads of all {@link de.dkfz.tbi.otp.ngsdata.SeqTrack} that belong to this job
     */
    public static Long getReadsSum(ClusterJob job) {
        return normalizePropertyToClusterJobs(job) { ProcessParameterObject workflowObject ->
            List<Long> nReads = workflowObject.getContainedSeqTracks()*.getNReads()
            nReads.contains(null) ? null : nReads.sum()
        }
    }

    /**
     * normalizes given property to the count of {@link ClusterJob} that belong to its OTP-Job
     * jobs work on different object-levels, e.g. FastqcJobs work on DataFiles, whereas CalculateChecksumJobs work on Runs
     * for calculating properties of OTP-Objects per {@link ClusterJob} it is necessary to adjust these properties to
     * the count of Input-Objects used for this {@link ClusterJob}
     */
    private static Long normalizePropertyToClusterJobs(ClusterJob clusterJob, Closure propertyToBeNormalized) {
        List<ClusterJob> clusterJobs = findAllClusterJobsToOtpJob(clusterJob)
        ProcessParameterObject workflowObject = findProcessParameterObjectByClusterJob(clusterJob)
        Long propertyToBeNormalizedResult = propertyToBeNormalized(workflowObject)
        if (propertyToBeNormalizedResult && clusterJobs) {
            return propertyToBeNormalizedResult / clusterJobs.size()
        }
        return null
    }

    /**
     * in case a {@link ProcessingStep} belongs to {@link de.dkfz.tbi.otp.job.processing.RestartedProcessingStep} it
     * returns the top most {@link ProcessingStep} stored as original from ClusterJob
     */
    public static List<ClusterJob> findAllClusterJobsToOtpJob(ClusterJob job) {
        ProcessingStep processingStep = ProcessingStep.findTopMostProcessingStep(job.processingStep)
        return ClusterJob.findAllByProcessingStepAndJobClass(processingStep, job.jobClass)
    }

    /**
     * checks if a {@link ClusterJob} has an elapsed walltime under the duration we assume jobs being failed
     * sometimes the flowControl-API returns jobs with an elapsed walltime in the range zero but an exit Status 'COMPLETED'
     * this can result to misleading statistics
     */
    public void handleObviouslyFailedClusterJob(ClusterJob job) {
        if (job.elapsedWalltime && job.elapsedWalltime <= DURATION_JOB_OBVIOUSLY_FAILED) {
            job.exitStatus = ClusterJob.Status.FAILED
            job.save(flush: true, failOnError: true)
        }
    }

    /**
     * returns a List of Cluster Jobs in a specific time span
     * @return List [clusterJob1, clusterJob2, ...]
     */
    public List findAllClusterJobsByDateBetween(LocalDate sDate, LocalDate eDate, String filter, int offset, int displayedLines, String sortedColumn, String sortOrder) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        return ClusterJob.createCriteria().list {
            order(sortedColumn, sortOrder)
            ge('queued', startDate)
            lt('ended', endDate)
            if (filter) {
                or {
                    ilike('clusterJobId', "%${filter}%")
                    ilike('clusterJobName', "%${filter}%")
                }
            }
            firstResult(offset)
            maxResults(displayedLines)
        }
    }

    /**
     * returns the number of Cluster Jobs in a specific time span
     */
    public int countAllClusterJobsByDateBetween(LocalDate sDate, LocalDate eDate, String filter) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        return ClusterJob.createCriteria().get {
            projections {
                ge('queued', startDate)
                lt('ended', endDate)
                if (filter) {
                    or {
                        ilike('clusterJobId', "%${filter}%")
                        ilike('clusterJobName', "%${filter}%")
                    }
                }
                rowCount()
            }
        }
    }

    /**
     * returns a unique list of job classes in a specific time-span
     * existing in the Cluster Job table
     */
    public List findAllJobClassesByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 DISTINCT job.job_class AS jobClass
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN}
 ORDER BY job.job_class
"""

        List jobClasses = []

        sql.eachRow(query, [startDate.millis, endDate.millis]) {
            jobClasses << it.jobclass
        }

        return jobClasses
    }

    /**
     * returns a List of exit codes and their occurrence in a specific time span
     * @return ArrayList [[exitCode, number of occurrences], ...]
     */
    public List findAllExitCodesByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.exit_code AS exitCode,
 count(job.exit_code) AS exitCodeCount
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN}
 GROUP BY job.exit_code
 ORDER BY job.exit_code
"""

        List exitCodeOccurenceList = []

        sql.eachRow(query, [startDate.millis, endDate.millis]) {
            exitCodeOccurenceList << [it.exitCode, it.exitCodeCount]
        }

        return exitCodeOccurenceList
    }

    /**
     * returns a List of exit statuses and their occurrence in a specific time span
     * @return ArrayList [[exitStatus, number of occurrences], ...]
     */
    public List findAllExitStatusesByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.exit_status AS exitStatus,
 count(job.exit_status) AS exitStatusCount
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN}
 GROUP BY job.exit_status
"""

        List exitStatusOccurenceList = []

        sql.eachRow(query, [startDate.millis, endDate.millis]) {
            exitStatusOccurenceList << [it.exitStatus as ClusterJob.Status, it.exitStatusCount]
        }

        return exitStatusOccurenceList
    }

    /**
     * returns all failed jobs and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 4 jobs failed processing
     */
    public Map findAllFailedByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.ended / $HOURS_TO_MILLIS AS hour,
 count(job.id) AS count
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN}
 AND job.exit_status = 'FAILED'
 GROUP BY job.ended / $HOURS_TO_MILLIS
"""

        Map hours = [:]

        sql.eachRow(query, [startDate.millis, endDate.millis]) {
            hours[new DateTime(it.hour * HOURS_TO_MILLIS)] = it.count
        }

        List data = hourBuckets.collect {
            it in hours ? hours[it] : 0
        }

        return [days: hourBuckets*.toString(FORMAT_STRING), data: data]
    }

    /**
     * returns all states and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: ['queued': [0, ...], 'started': [1, ...], 'ended': [0, ...]]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 0 jobs have been queued, 1 job has been started and 0 jobs have been ended
     */
    public Map findAllStatesByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        Map data = ["queued": [], "started": [], "ended": []]

        data.keySet().each {

            String query = """
SELECT
 job.${it} / $HOURS_TO_MILLIS AS hour,
 count(job.id) AS count
 FROM cluster_job AS job
 WHERE
 job.${it} >= ?
 AND job.${it} < ?
 GROUP BY job.${it} / $HOURS_TO_MILLIS
"""

            Map hours = [:]

            sql.eachRow(query, [startDate.millis, endDate.millis]) {
                hours[new DateTime(it.hour * HOURS_TO_MILLIS)] = it.count
            }

            data."${it}" = hourBuckets.collect {
                it in hours ? hours[it] : 0
            }
        }

        return ["days": hourBuckets*.toString(FORMAT_STRING), "data": data]
    }

    /**
     * returns the average core usages per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 4 cores were used to process the jobs
     */
    public Map findAllAvgCoreUsageByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.started / $HOURS_TO_MILLIS AS hourStarted,
 job.ended / $HOURS_TO_MILLIS AS hourEnded,
 SUM(job.cpu_time / (job.ended - job.started)) AS sumAvgCpuTime
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_NOTFAILED}
 GROUP BY job.started / $HOURS_TO_MILLIS, job.ended / $HOURS_TO_MILLIS
"""

        List results = []

        sql.eachRow(query, [startDate.millis, endDate.millis]) {
            results << [startDate: new DateTime(it.hourStarted * HOURS_TO_MILLIS), endDate: new DateTime(it.hourEnded * HOURS_TO_MILLIS), cpuAvgUsed: it.sumAvgCpuTime]
        }

        def data = hourBuckets.collect { currentHour ->
            def nextHour = currentHour.plusHours(1)

            def jobsThisHour = results.findAll {
                it.startDate < nextHour && currentHour <= it.endDate
            }

            Double cpuAvgUsedSum = jobsThisHour*.cpuAvgUsed.sum()

            return cpuAvgUsedSum ? cpuAvgUsedSum as Integer : 0
        }

        return ["days": hourBuckets*.toString(FORMAT_STRING), "data": data]
    }

    /**
     * returns the memory usages and its number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [2048, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 2048 mb memory was used to process the jobs
     */
    public Map findAllMemoryUsageByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.started / $HOURS_TO_MILLIS AS hourStarted,
 job.ended / $HOURS_TO_MILLIS AS hourEnded,
 SUM(job.used_memory) AS sumAvgMemoryUsed
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_NOTFAILED}
 GROUP BY job.started / $HOURS_TO_MILLIS, job.ended / $HOURS_TO_MILLIS
"""

        List results = []

        sql.eachRow(query, [startDate.millis, endDate.millis]) {
            results << [startDate: new DateTime(it.hourStarted * HOURS_TO_MILLIS), endDate: new DateTime(it.hourEnded * HOURS_TO_MILLIS), memoryAvgUsed: it.sumAvgMemoryUsed / (1024 ** 2)]
        }

        def data = hourBuckets.collect { currentHour ->
            def nextHour = currentHour.plusHours(1)

            def jobsThisHour = results.findAll {
                currentHour <= it.endDate && it.endDate < nextHour || it.startDate < nextHour && nextHour < it.endDate
            }

            Double memoryAvgUsedSum = jobsThisHour*.memoryAvgUsed.sum()

            return memoryAvgUsedSum ? memoryAvgUsedSum as Integer : 0
        }

        return ["days": hourBuckets*.toString(FORMAT_STRING), "data": data]
    }

    /**
     * returns the average time in queue and average processing time, both as absolut and percentage values, for all jobs
     * @return map [queue: [percentageQueue, queuePeriod], process: [percentageProcess, processPeriod]]
     */
    public Map findAllStatesTimeDistributionByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 SUM (job.started - job.queued) AS queueTime,
 SUM (job.ended - job.started) AS processingTime
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_NOTFAILED}
"""

        Long queue, process, pQueue, pProcess

        sql.query(query, [startDate.millis, endDate.millis], {
            it.next()
            queue = it.getLong('queueTime') ?: 0
            process = it.getLong('processingTime') ?: 0
        })


        pQueue = queue ? ((100 / (queue + process) * queue) as double).round() : 0
        pProcess = queue ? 100 - pQueue : 0

        return [queue: [pQueue, new Period(queue).getHours().toString()], process: [pProcess, new Period(process).getHours().toString()]]
    }

    /**
     * returns a unique list of sequencing types
     * existing in the Cluster Job table
     */
    public List findJobClassSpecificSeqTypesByDateBetween(String jobClass, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 DISTINCT job.seq_type_id AS seqType
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN}
 AND job.job_class = ?
 AND job.seq_type_id IS NOT NULL
 ORDER BY job.seq_type_id
"""

        List seqTypes = []

        sql.eachRow(query, [startDate.millis, endDate.millis, jobClass]) {
            seqTypes << SeqType.get(it.seqType)
        }

        return seqTypes
    }

    /**
     * returns a list of exit codes and their count of
     * a specific job class and sequencing type, in a specific time span
     * @return list of arrays containing the exitCode and its count
     * [0] = exitCode
     * [1] = count of exitCode
     */
    public List findJobClassAndSeqTypeSpecificExitCodesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.exit_code AS exitCode,
 count(job.exit_code) AS exitCodeCount
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE}
 GROUP BY job.exit_code
 ORDER BY job.exit_code
"""

        List exitCodeOccurenceList = []

        sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType?.id]) {
            exitCodeOccurenceList << [it.exitCode, it.exitCodeCount]
        }

        return exitCodeOccurenceList
    }

    /**
     * returns a list of exit statuses and their count of
     * a specific job class and sequencing type, in a specific time span
     * @return list of arrays containing the exitStatuses and its count
     * [0] = exitStatus
     * [1] = count of exitCode
     */
    public List findJobClassAndSeqTypeSpecificExitStatusesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.exit_status AS exitStatus,
 count(job.exit_status) AS exitStatusCount
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE}
 GROUP BY job.exit_status
 ORDER BY job.exit_status
"""

        List exitStatusOccurenceList = []

        sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType?.id]) {
            exitStatusOccurenceList << [it.exitStatus as ClusterJob.Status, it.exitStatusCount]
        }

        return exitStatusOccurenceList
    }

    /**
     * returns all states per hour at a given time span of a specific job class and sequencing type and
     * and array of all dates (per hour) existing in the given time span
     * @return ["days": [day1Hour0, day2Hour1, ...], "data": ["queued": [...], "started": [...], "ended":[...]]]
     */
    public Map findJobClassAndSeqTypeSpecificStatesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        Map data = ["queued": [], "started": [], "ended": []]

        data.keySet().each {

            String query = """
SELECT
 job.${it} / $HOURS_TO_MILLIS AS hour,
 count(job.id) AS count
 FROM cluster_job AS job
 WHERE
 job.${it} >= ?
 AND job.${it} < ?
 AND job.job_class = ?
 AND job.seq_type_id = ?
 GROUP BY job.${it} / $HOURS_TO_MILLIS
"""

            Map hours = [:]

            sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType?.id]) {
                hours[new DateTime(it.hour * HOURS_TO_MILLIS)] = it.count
            }

            data."${it}" = hourBuckets.collect {
                it in hours ? hours[it] : 0
            }
        }

        return ["days": hourBuckets*.toString(FORMAT_STRING), "data": data]
    }

    /**
     * returns requested and elapsed walltimes of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    public Map findJobClassAndSeqTypeSpecificWalltimesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 (job.ended - job.started) /1000 /60 AS elapsedWalltime,
 job.n_reads /1000 /1000 AS reads,
 job.id AS id,
 job.xten AS xten
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
"""

        def walltimeData = []

        sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType?.id]) {
            walltimeData << [it.reads, it.elapsedWalltime, it.xten ? 'blue' : 'black', it.id]
        }

        def xAxisMax = walltimeData ? walltimeData*.get(0).max() : 0
        def labels = xAxisMax != 0 ? getLabels(xAxisMax, 10) : []

        return ["data": walltimeData, "labels": labels, "xMax": xAxisMax]
    }

    /**
     * returns the average core usage of a specific jobclass and seq type
     * @return double
     */
    public double findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 AVG(job.cpu_time) as avgCpuTime,
 AVG(job.ended - job.started) as avgWalltime
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
"""

        Double avgCpu

        sql.query(query, [startDate.millis, endDate.millis, jobClass, seqType?.id], {
            it.next()
            Long avgCpuTime = it.getLong('avgCpuTime')
            Long avgWalltime = it.getLong('avgWalltime')
            avgCpu = avgCpuTime && avgWalltime ? (avgCpuTime / avgWalltime) as Double : 0
        })


        return avgCpu.round(2)
    }

    /**
     * return the average memory usage of a specific jobclass and seqtype
     * @return int
     */
    public int findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 AVG(job.used_memory) AS avgMemory
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
"""

        Integer avgMemory

        sql.query(query, [startDate.millis, endDate.millis, jobClass, seqType?.id], {
            it.next()
            avgMemory = it.getLong('avgMemory')
        })

        return avgMemory ?: 0
    }

    /**
     * returns the average time in queue and average processing time for a specific jobclass and seqtype
     * @return map ["avgQueue": avgQueue, "avgProcess": avgProcess]
     */
    public Map findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate, Long basesToBeNormalized = null) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 AVG(n_bases) AS basesCount,
 CAST (AVG (job.started - job.queued) AS int) AS avgQueue,
 CAST (AVG (job.ended - job.started) AS int) AS avgProcess
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
 AND n_bases IS NOT NULL
"""

        Long avgBases, avgQueue, avgProcess

        // prevents negative values that could appear cause of rounding errors with milliseconds values
        // OTP-1304, queued gets set through OTP, started & ended gets set by the cluster
        sql.query(query, [startDate.millis, endDate.millis, jobClass, seqType?.id], {
            it.next()
            avgBases = it.getLong('basesCount')
            avgQueue = Math.max(0, it.getLong('avgQueue') ?: 0)
            avgProcess = Math.max(0, it.getLong('avgProcess') ?: 0)
        })

        // normalize result to custom number of bases
        if (basesToBeNormalized) {
            avgProcess = avgProcess / (avgBases == 0L ? 1L : avgBases) * basesToBeNormalized
        }

        return ["avgQueue": avgQueue, "avgProcess": avgProcess]
    }

    /**
     * returns statistics about the input-coverage of the files used as input of {@link ClusterJob} for a specific jobClass and seqType
     * @param referenceGenomeSizeInBases used to recalculate the coverage from the selected amount of bases (inputcoverage = inputbases / referencegenomesize)
     * @return map ["minCov": minimumCoverage, "maxCov": maximumCoverage, "avgCov": averageCoverage, "medianCov": medianCoverage]
     */
    public Map findJobClassAndSeqTypeSpecificCoverages(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate, Double referenceGenomeSizeInBases) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        Map coverageStatistic = [avgCov: null, minCov: null, maxCov: null, medianCov: null]
        Long jobCount

        def query = """
SELECT
count(*) as jobCount,
avg(n_bases) as avgBases,
max(n_bases) as maxBases,
min(n_bases) as minBases
FROM cluster_job AS job
WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
 AND n_bases IS NOT NULL
"""

        sql.query(query, [startDate.millis, endDate.millis, jobClass, seqType?.id]) {
            it.next()
            jobCount = it.getLong('jobCount')
            if (jobCount) {
                coverageStatistic.put('avgCov', it.getLong('avgBases') / referenceGenomeSizeInBases)
                coverageStatistic.put('minCov', it.getLong('minBases') / referenceGenomeSizeInBases)
                coverageStatistic.put('maxCov', it.getLong('maxBases') / referenceGenomeSizeInBases)
            }
        }

        if (!jobCount) {
            return coverageStatistic
        }

        // if even number take the average of the two middle numbers
        if (jobCount % 2 == 0L) {
            query = """
SELECT
avg(n_bases_from_offset) as medianBases
FROM
 (SELECT n_bases as n_bases_from_offset
 FROM cluster_job as job
 WHERE
  ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
  AND n_bases IS NOT NULL
  ORDER BY n_bases
  LIMIT 2 OFFSET ?)
job
"""
        } else {
            query = """
SELECT
n_bases as medianBases
FROM cluster_job AS job
WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
 AND n_bases IS NOT NULL
 ORDER BY n_bases
 LIMIT 1 OFFSET ?
"""
        }


        int medianOffset = Math.round(jobCount / 2) - 1

        sql.query(query, [startDate.millis, endDate.millis, jobClass, seqType.id, medianOffset]) {
            it.next()
            coverageStatistic.put('medianCov', it.getLong('medianBases') / referenceGenomeSizeInBases)
        }

        return coverageStatistic
    }

    /**
     * returns the time in queue and the processing time of a specific job
     * @return map ["queue": [percentQueue, queueInMillis], "process": [percentProcess, processInMillis]]
     */
    public Map findJobSpecificStatesTimeDistributionByJobId(Long id) {
        def sql = new Sql(dataSource)

        ClusterJob job = ClusterJob.get(id)
        if (!job) {
            return null
        }

        Duration queue = new Duration(job.queued, job.started)
        Duration process = new Duration(job.started, job.ended)

        def queueMillis = Math.max(0, queue.getMillis())
        def processMillis = Math.max(0, process.getMillis())

        long total = queueMillis + processMillis

        int pProcess = Math.round(100 / total * processMillis)
        int pQueue = 100 - pProcess

        return ["queue": [pQueue, queue.millis], "process": [pProcess, process.millis]]
    }

    /**
     * returns all dates and hours between the two given dates as DateTime
     * e.g startDate = 2000-01-01, endDate = 2000-01-02
     * result = [2000-01-01 00:00:00, 2000-01-01 01:00:00, 2000-01-01 02:00:00, ... , 2000-03-01 00:00:00]
     */
    public List getDaysAndHoursBetween(LocalDate sDate, LocalDate eDate) {
        def daysArr = []

        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        while (startDate < endDate.plusHours(1)) {
            daysArr << startDate
            startDate = startDate.plusHours(1)
        }

        return daysArr
    }

    /**
     * returns the latest Job Date
     * @return latest Job Date (queued)
     */
    public LocalDate getLatestJobDate() {
        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.queued AS latestQueued
 FROM cluster_job AS job
 ORDER BY job.queued DESC
 LIMIT 1
"""

        Long latestJobDateAsLong

        sql.eachRow(query) {
            latestJobDateAsLong = it.latestQueued
        }

        return latestJobDateAsLong ? new LocalDate(latestJobDateAsLong) : null
    }

    /**
     * returns an amount of labels aligned for scatter RGraphs in dependence to a given maximum value
     * @return List of dates as Strings and Integer-values
     * e.g. max = 8000 and quot = 10, return = [["800", "1600", "2400", ... , "8000"], [800, 1600, 2400, ... , 8000] ]
     */
    public List getLabels(Long max, int quot) {
        def labels = (1..quot).collect {
            (max / quot * it).toDouble().round(2)
        }
        def labelsAsString = labels*.toString()

        return [labelsAsString, labels]
    }

    /**
     * parses arguments for methods with dates and the use of the getDaysAndHoursBetween-method
     * @return list containing the Datetime-objects for startDate and endDate and
     * the list returned by the getDaysAndHoursBetween-method
     */
    private List parseArgs(LocalDate sDate, LocalDate eDate) {
        ArrayList<String> hourBuckets = getDaysAndHoursBetween(sDate, eDate)
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        return [startDate, endDate, hourBuckets]
    }

    /**
     * parses arguments for methods with dates
     * @return list containing the Datetime-objects for startDate and endDate
     */

    private List parseDateArgs(LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()

        return [startDate, endDate]
    }
}
