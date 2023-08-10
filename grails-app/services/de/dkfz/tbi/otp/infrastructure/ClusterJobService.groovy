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
package de.dkfz.tbi.otp.infrastructure

import grails.gorm.transactions.Transactional
import groovy.sql.Sql
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.roddy.execution.jobs.GenericJobInfo
import de.dkfz.roddy.tools.BufferUnit
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils

import javax.sql.DataSource
import java.nio.file.FileSystem
import java.time.*

import static java.util.concurrent.TimeUnit.HOURS

@CompileDynamic
@Transactional
class ClusterJobService {

    FileSystemService fileSystemService
    FileService fileService
    DataSource dataSource

    static final Long HOURS_TO_MILLIS = HOURS.toMillis(1)
    static final Duration DURATION_JOB_OBVIOUSLY_FAILED = Duration.ofMillis(9)
    // we assume that jobs with an elapsed walltime under 10ms "obviously failed"

    static private final int FACTOR_2 = 2

    static private final long FACTOR_100 = 100

    static private final long FACTOR_1024 = 1024

    static private final String QUERY_BY_TIMESPAN = """
 job.queued >= ?
 AND job.ended < ?
 AND job.ended > job.started
"""

    static private final String QUERY_BY_TIMESPAN_NOTFAILED = """
 ${QUERY_BY_TIMESPAN}
 AND job.exit_status != 'FAILED'
"""

    static private final String QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE = """
 ${QUERY_BY_TIMESPAN}
 AND job.job_class = ?
 AND job.seq_type_id = ?
"""

    static private final String QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED = """
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE}
 AND job.exit_status != 'FAILED'
"""

    List<ClusterJob> findAllByProcessingStep(ProcessingStep processingStep) {
        return ClusterJob.findAllByProcessingStep(processingStep)
    }

    ClusterJob findById(long id) {
        return ClusterJob.get(id)
    }

    /**
     * The clusterJobId is not unique. Therefore the newest clusterJobId is fetched.
     */
    ClusterJob findNewestClusterJobByClusterJobId(String id) {
        return CollectionUtils.atMostOneElement(ClusterJob.findAllByClusterJobId(id, [max: 1, sort: "dateCreated", order: "desc"]))
    }

    /**
     * creates a cluster job object with at this time known attributes
     */
    @SuppressWarnings("ParameterCount")
    @Deprecated
    ClusterJob createClusterJob(Realm realm, String clusterJobId, String userName,
                                ProcessingStep processingStep, SeqType seqType = null,
                                String clusterJobName = processingStep.clusterJobName,
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
                queued: ZonedDateTime.now(),
        ).save(flush: true)
        assert job != null
        return job
    }

    /**
     * creates a cluster job object with at this time known attributes
     */
    @SuppressWarnings("ParameterCount")
    ClusterJob createClusterJob(Realm realm, String clusterJobId, String userName,
                                WorkflowStep workflowStep, String clusterJobName,
                                String jobClass = workflowStep.class.simpleName) {
        ClusterJob job = new ClusterJob([
                workflowStep  : workflowStep,
                oldSystem     : false,
                realm         : realm,
                clusterJobId  : clusterJobId,
                userName      : userName,
                clusterJobName: clusterJobName,
                jobClass      : jobClass,
                queued        : ZonedDateTime.now(),
        ]).save(flush: true)
        return job
    }

    /**
     * Stores values for statistics after the job has been sent
     */
    void amendClusterJob(ClusterJob job, GenericJobInfo jobInfo) {
        job.with {
            requestedCores = jobInfo.askedResources?.cores
            requestedWalltime = jobInfo?.askedResources?.walltime
            requestedMemory = jobInfo.askedResources?.mem?.toLong(BufferUnit.k)

            jobLog = jobInfo.logFile
            node = jobInfo.executionHosts?.unique()?.sort()?.join(",")
            accountName = jobInfo.account
            dependencies = jobInfo.parentJobIDs ? jobInfo.parentJobIDs.collect {
                CollectionUtils.exactlyOneElement(
                        job.oldSystem ? ClusterJob.findAllByClusterJobIdAndProcessingStep(it, job.processingStep) :
                                ClusterJob.findAllByClusterJobIdAndWorkflowStep(it, job.workflowStep)
                )
            } : []
        }
    }

    /**
     * Stores values for statistics after the job has finished
     */
    void completeClusterJob(ClusterJob job, ClusterJob.Status status, GenericJobInfo jobInfo) {
        job.with {
            exitStatus = status
            exitCode = jobInfo.exitCode
            queued = jobInfo.submitTime ?: job.queued
            eligible = jobInfo.eligibleTime
            started = jobInfo.startTime
            ended = jobInfo.endTime

            cpuTime = jobInfo.cpuTime
            usedCores = jobInfo.usedResources?.cores
            usedMemory = jobInfo.usedResources?.mem?.toLong(BufferUnit.k)
            usedSwap = jobInfo.usedResources?.swap?.toLong(BufferUnit.k) as Integer

            node = jobInfo.executionHosts?.unique()?.sort()?.join(",")

            xten = isXten(job)
            nBases = getBasesSum(job)
            nReads = getReadsSum(job)
            fileSize = getFileSizesSum(job)

            save(flush: true)
        }

        handleObviouslyFailedClusterJob(job)
    }

    /**
     * Does log exist and can be read.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean doesClusterJobLogExist(ClusterJob clusterJob) {
        if (!clusterJob.jobLog) {
            return false
        }
        FileSystem fs = fileSystemService.getRemoteFileSystem(clusterJob.realm)
        return fileService.fileIsReadable(fs.getPath(clusterJob.jobLog), clusterJob.realm)
    }

    /**
     * Retrieves the cluster job log file for the given ClusterJob and returns the file content.
     * @param clusterJob for which the log file should be retrieved
     * @return Content of log file
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    String getClusterJobLog(ClusterJob clusterJob) {
        if (!clusterJob.jobLog) {
            return "Path to job log not set."
        }
        try {
            FileSystem fs = fileSystemService.getRemoteFileSystem(clusterJob.realm)
            return fs.getPath(clusterJob.jobLog).text
        } catch (IOException e) {
            return e.message
        }
    }

    /**
     * returns the specific workflow object to a cluster job, e.g. Run
     * @return Object or null
     */
    static ProcessParameterObject findProcessParameterObjectByClusterJob(ClusterJob job) {
        return job.processingStep?.processParameterObject
    }

    /**
     * returns true if a job belongs to data that is sequenced by X-Ten machines
     */
    //There are cases that xten and non xten are together, so the null as third value is needed
    @SuppressWarnings('BooleanMethodReturnsNull')
    static Boolean isXten(ClusterJob job) {
        if (!job.oldSystem) {
            //new workflow system do not support access to seqtracks, which are needed to get that information
            return null
        }
        ProcessParameterObject workflowObject = findProcessParameterObjectByClusterJob(job)
        List<SeqPlatformModelLabel> seqPlatformModelLabels = workflowObject.containedSeqTracks.toList()*.seqPlatform*.seqPlatformModelLabel
        if (seqPlatformModelLabels*.id.unique().size() == 1 && seqPlatformModelLabels.first() != null) {
            return seqPlatformModelLabels.first().name == "HiSeq X Ten"
        }
        return null
    }

    /**
     * returns the sum of bases of all {@link de.dkfz.tbi.otp.ngsdata.SeqTrack} that belong to this job
     */
    static Long getBasesSum(ClusterJob job) {
        if (!job.oldSystem) {
            //new workflow system do not support access to seqtracks, which are needed to get that information
            return null
        }
        return normalizePropertyToClusterJobs(job) { ProcessParameterObject workflowObject ->
            workflowObject.containedSeqTracks?.sum { it.nBasePairs ?: 0 }
        }
    }

    /**
     * returns the sum of file sizes of all {@link RawSequenceFile} that belong to this job
     */
    static Long getFileSizesSum(ClusterJob job) {
        if (!job.oldSystem) {
            //new workflow system do not support access to seqtracks, which are needed to get that information
            return null
        }
        return normalizePropertyToClusterJobs(job) { ProcessParameterObject workflowObject ->
            List<SeqTrack> seqTracks = workflowObject.containedSeqTracks.asList()
            (seqTracks ? RawSequenceFile.findAllBySeqTrackInListAndIndexFile(seqTracks, false) : [])?.sum { it.fileSize }
        }
    }

    /**
     * returns the sum of reads of all {@link de.dkfz.tbi.otp.ngsdata.SeqTrack} that belong to this job
     */
    static Long getReadsSum(ClusterJob job) {
        if (!job.oldSystem) {
            //new workflow system do not support access to seqtracks, which are needed to get that information
            return null
        }
        return normalizePropertyToClusterJobs(job) { ProcessParameterObject workflowObject ->
            List<Long> nReads = workflowObject.containedSeqTracks*.NReads
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
    static List<ClusterJob> findAllClusterJobsToOtpJob(ClusterJob job) {
        ProcessingStep processingStep = ProcessingStep.findTopMostProcessingStep(job.processingStep)
        return ClusterJob.findAllByProcessingStepAndJobClass(processingStep, job.jobClass)
    }

    /**
     * checks if a {@link ClusterJob} has an elapsed walltime under the duration we assume jobs being failed
     * sometimes the flowControl-API returns jobs with an elapsed walltime in the range zero but an exit Status 'COMPLETED'
     * this can result to misleading statistics
     */
    void handleObviouslyFailedClusterJob(ClusterJob job) {
        if (job.elapsedWalltime && job.elapsedWalltime <= DURATION_JOB_OBVIOUSLY_FAILED) {
            job.exitStatus = ClusterJob.Status.FAILED
            job.save(flush: true)
        }
    }

    /**
     * returns a List of Cluster Jobs in a specific time span
     * @return List [clusterJob1, clusterJob2, ...]
     */
    List findAllClusterJobsByDateBetween(
            LocalDate startDate, LocalDate endDate, String filter, int offset, int displayedLines, String sortedColumn, String sortOrder) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        return ClusterJob.createCriteria().list {
            order(sortedColumn, sortOrder)
            ge('queued', startEndDateTime.startDate)
            lt('ended', startEndDateTime.endDate)
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
    int countAllClusterJobsByDateBetween(LocalDate startDate, LocalDate endDate, String filter) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        return ClusterJob.createCriteria().get {
            projections {
                ge('queued', startEndDateTime.startDate)
                lt('ended', startEndDateTime.endDate)
                if (filter) {
                    or {
                        ilike('clusterJobId', "%${filter}%")
                        ilike('clusterJobName', "%${filter}%")
                    }
                }
                rowCount()
            }
        } as int
    }

    /**
     * returns the number of cluster jobs for specified period and projects
     */
    int getNumberOfClusterJobsForSpecifiedPeriodAndProjects(Date startDate = null, Date endDate = null, List<Project> projects) {
        return ClusterJob.createCriteria().count {
            individual {
                'in'('project', projects)
            }
            if (startDate && endDate) {
                between('dateCreated', startDate, endDate)
            }
        }
    }

    /**
     * returns a unique list of job classes in a specific time-span
     * existing in the Cluster Job table
     */
    List findAllJobClassesByDateBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

        String query = """
SELECT
 DISTINCT job.job_class AS jobClass
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN}
 ORDER BY job.job_class
"""

        List jobClasses = []

        sql.eachRow(query, startEndDateTime.asMillis()) {
            jobClasses << it.jobclass
        }

        return jobClasses
    }

    /**
     * returns a List of exit codes and their occurrence in a specific time span
     * @return List [[exitCode, number of occurrences], ...]
     */
    List findAllExitCodesByDateBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        sql.eachRow(query, startEndDateTime.asMillis()) {
            exitCodeOccurenceList << [it.exitCode, it.exitCodeCount]
        }

        return exitCodeOccurenceList
    }

    /**
     * returns a List of exit statuses and their occurrence in a specific time span
     * @return List [[exitStatus, number of occurrences], ...]
     */
    List findAllExitStatusesByDateBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        sql.eachRow(query, startEndDateTime.asMillis()) {
            exitStatusOccurenceList << [it.exitStatus as ClusterJob.Status, it.exitStatusCount]
        }

        return exitStatusOccurenceList
    }

    /**
     * returns all failed jobs and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 4 jobs failed processing
     */
    Map findAllFailedByDateBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeIntervalWithHourBuckets startEndDateTimeWithHourBuckets = new DateTimeIntervalWithHourBuckets(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        sql.eachRow(query, startEndDateTimeWithHourBuckets.asMillis()) {
            hours[TimeUtils.fromMillis(it.hour * HOURS_TO_MILLIS)] = it.count
        }

        List data = startEndDateTimeWithHourBuckets.hourBuckets.collect {
            it in hours ? hours[it] : 0
        }

        return [
                days: startEndDateTimeWithHourBuckets.formattedHourBuckets(),
                data: data,
        ]
    }

    /**
     * returns all states and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: ['queued': [0, ...], 'started': [1, ...], 'ended': [0, ...]]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 0 jobs have been queued, 1 job has been started and 0 jobs have been ended
     */
    Map findAllStatesByDateBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeIntervalWithHourBuckets startEndDateTimeWithHourBuckets = new DateTimeIntervalWithHourBuckets(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

            sql.eachRow(query, startEndDateTimeWithHourBuckets.asMillis()) {
                hours[TimeUtils.fromMillis(it.hour * HOURS_TO_MILLIS)] = it.count
            }

            data."${it}" = startEndDateTimeWithHourBuckets.hourBuckets.collect {
                it in hours ? hours[it] : 0
            }
        }

        return ["days": startEndDateTimeWithHourBuckets.formattedHourBuckets(), "data": data]
    }

    /**
     * returns the average core usages per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 4 cores were used to process the jobs
     */
    Map findAllAvgCoreUsageByDateBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeIntervalWithHourBuckets startEndDateTimeWithHourBuckets = new DateTimeIntervalWithHourBuckets(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        List<Map<String, ?>> results = []

        sql.eachRow(query, startEndDateTimeWithHourBuckets.asMillis()) {
            results << [
                    startDate : TimeUtils.fromMillis(it.hourStarted * HOURS_TO_MILLIS),
                    endDate   : TimeUtils.fromMillis(it.hourEnded * HOURS_TO_MILLIS),
                    cpuAvgUsed: it.sumAvgCpuTime,
            ]
        }

        List<Integer> data = startEndDateTimeWithHourBuckets.hourBuckets.collect { currentHour ->
            ZonedDateTime nextHour = currentHour.plusHours(1)

            List jobsThisHour = results.findAll {
                it.startDate < nextHour && currentHour <= it.endDate
            }

            Double cpuAvgUsedSum = jobsThisHour*.cpuAvgUsed.sum()

            return cpuAvgUsedSum ? cpuAvgUsedSum as Integer : 0
        }

        return ["days": startEndDateTimeWithHourBuckets.formattedHourBuckets(), "data": data]
    }

    /**
     * returns the memory usages and its number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [2048, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 2048 mb memory was used to process the jobs
     */
    Map findAllMemoryUsageByDateBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeIntervalWithHourBuckets startEndDateTimeWithHourBuckets = new DateTimeIntervalWithHourBuckets(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        List<Map<String, ?>> results = []

        sql.eachRow(query, startEndDateTimeWithHourBuckets.asMillis()) {
            results << [
                    startDate    : TimeUtils.fromMillis(it.hourStarted * HOURS_TO_MILLIS),
                    endDate      : TimeUtils.fromMillis(it.hourEnded * HOURS_TO_MILLIS),
                    memoryAvgUsed: it.sumAvgMemoryUsed / (FACTOR_1024 * FACTOR_1024),
            ]
        }

        List<Integer> data = startEndDateTimeWithHourBuckets.hourBuckets.collect { currentHour ->
            ZonedDateTime nextHour = currentHour.plusHours(1)

            List jobsThisHour = results.findAll {
                currentHour <= it.endDate && it.endDate < nextHour || it.startDate < nextHour && nextHour < it.endDate
            }

            Double memoryAvgUsedSum = jobsThisHour*.memoryAvgUsed.sum()

            return memoryAvgUsedSum ? memoryAvgUsedSum as Integer : 0
        }

        return ["days": startEndDateTimeWithHourBuckets.formattedHourBuckets(), "data": data]
    }

    /**
     * returns the average time in queue and average processing time, both as absolut and percentage values, for all jobs
     * @return map [queue: [percentageQueue, queuePeriod], process: [percentageProcess, processPeriod]]
     */
    List findAllStatesTimeDistributionByDateBetween(LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

        String query = """
SELECT
 SUM (job.started - job.queued) AS queueTime,
 SUM (job.ended - job.started) AS processingTime
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_NOTFAILED}
"""

        Long queue, process, percentageQueue, percentageProcess

        sql.query(query, startEndDateTime.asMillis()) {
            it.next()
            queue = it.getLong('queueTime') ?: 0
            process = it.getLong('processingTime') ?: 0
        }

        percentageQueue = queue ? ((FACTOR_100 / (queue + process) * queue) as double).round() : 0
        percentageProcess = queue ? FACTOR_100 - percentageQueue : 0

        return queue ? [
                ["Queue", percentageQueue, "${percentageQueue}% (${Duration.ofMillis(queue).toHours()} hours)"],
                ["Process", percentageProcess, "${percentageProcess}% (${Duration.ofMillis(process).toHours()} hours)"],
        ] : []
    }

    /**
     * returns a unique list of sequencing types
     * existing in the Cluster Job table
     */
    List findJobClassSpecificSeqTypesByDateBetween(String jobClass, LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        sql.eachRow(query, startEndDateTime.asMillis() + [jobClass]) {
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
    List findJobClassAndSeqTypeSpecificExitCodesByDateBetween(String jobClass, SeqType seqType, LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        sql.eachRow(query, startEndDateTime.asMillis() + [jobClass, seqType?.id]) {
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
    List findJobClassAndSeqTypeSpecificExitStatusesByDateBetween(String jobClass, SeqType seqType, LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        sql.eachRow(query, startEndDateTime.asMillis() + [jobClass, seqType?.id]) {
            exitStatusOccurenceList << [it.exitStatus as ClusterJob.Status, it.exitStatusCount]
        }

        return exitStatusOccurenceList
    }

    /**
     * returns all states per hour at a given time span of a specific job class and sequencing type and
     * and array of all dates (per hour) existing in the given time span
     * @return ["days": [day1Hour0, day2Hour1, ...], "data": ["queued": [...], "started": [...], "ended":[...]]]
     */
    Map findJobClassAndSeqTypeSpecificStatesByDateBetween(String jobClass, SeqType seqType, LocalDate startDate, LocalDate endDate) {
        DateTimeIntervalWithHourBuckets startEndDateTimeWithHourBuckets = new DateTimeIntervalWithHourBuckets(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

            sql.eachRow(query, startEndDateTimeWithHourBuckets.asMillis() + [jobClass, seqType?.id]) {
                hours[TimeUtils.fromMillis(it.hour * HOURS_TO_MILLIS)] = it.count
            }

            data."${it}" = startEndDateTimeWithHourBuckets.hourBuckets.collect {
                it in hours ? hours[it] : 0
            }
        }

        return ["days": startEndDateTimeWithHourBuckets.formattedHourBuckets(), "data": data]
    }

    /**
     * returns requested and elapsed walltimes of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    Map findJobClassAndSeqTypeSpecificWalltimesByDateBetween(String jobClass, SeqType seqType, LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

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

        List<List> walltimeData = []

        sql.eachRow(query, startEndDateTime.asMillis() + [jobClass, seqType?.id]) {
            walltimeData << [it.reads, it.elapsedWalltime, it.xten ? 'blue' : 'black', it.id]
        }

        Long xAxisMax = walltimeData ? walltimeData*.get(0).max() ?: 0 : 0
        List labels = xAxisMax == 0 ? [] : getLabels(xAxisMax, 10)

        return ["data": walltimeData, "labels": labels, "xMax": xAxisMax]
    }

    /**
     * returns the average core usage of a specific jobclass and seq type
     * @return double
     */
    double findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween(String jobClass, SeqType seqType, LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

        String query = """
SELECT
 AVG(job.cpu_time) as avgCpuTime,
 AVG(job.ended - job.started) as avgWalltime
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
"""

        Double avgCpu

        sql.query(query, startEndDateTime.asMillis() + [jobClass, seqType?.id]) {
            it.next()
            Long avgCpuTime = it.getLong('avgCpuTime')
            Long avgWalltime = it.getLong('avgWalltime')
            avgCpu = avgCpuTime && avgWalltime ? (avgCpuTime / avgWalltime) as Double : 0
        }

        return avgCpu.round(FACTOR_2)
    }

    /**
     * return the average memory usage of a specific jobclass and seqtype
     * @return int
     */
    int findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween(String jobClass, SeqType seqType, LocalDate startDate, LocalDate endDate) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

        String query = """
SELECT
 AVG(job.used_memory) AS avgMemory
 FROM cluster_job AS job
 WHERE
 ${QUERY_BY_TIMESPAN_JOBCLASS_SEQTYPE_NOTFAILED}
"""

        Integer avgMemory

        sql.query(query, startEndDateTime.asMillis() + [jobClass, seqType?.id]) {
            it.next()
            avgMemory = it.getLong('avgMemory')
        }

        return avgMemory ?: 0
    }

    /**
     * returns the average time in queue and average processing time for a specific jobclass and seqtype
     * @return map ["avgQueue": avgQueue, "avgProcess": avgProcess]
     */
    Map findSpecificAvgStatesTimeDistribution(String jobClass, SeqType seqType, LocalDate startDate, LocalDate endDate, Long basesToBeNormalized = null) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

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
        sql.query(query, startEndDateTime.asMillis() + [jobClass, seqType?.id,]) {
            it.next()
            avgBases = it.getLong('basesCount')
            avgQueue = Math.max(0, it.getLong('avgQueue') ?: 0)
            avgProcess = Math.max(0, it.getLong('avgProcess') ?: 0)
        }

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
    Map findJobClassAndSeqTypeSpecificCoverages(String jobClass, SeqType seqType, LocalDate startDate, LocalDate endDate, Double referenceGenomeSizeInBases) {
        DateTimeInterval startEndDateTime = new DateTimeInterval(startDate, endDate)

        Sql sql = new Sql(dataSource)

        Map coverageStatistic = [avgCov: null, minCov: null, maxCov: null, medianCov: null]
        Long jobCount

        String query = """
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

        sql.query(query, startEndDateTime.asMillis() + [jobClass, seqType?.id]) {
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
        if (jobCount % FACTOR_2 == 0L) {
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

        int medianOffset = Math.round(jobCount / FACTOR_2) - 1

        sql.query(query, startEndDateTime.asMillis() + [jobClass, seqType.id, medianOffset]) {
            it.next()
            coverageStatistic.put('medianCov', it.getLong('medianBases') / referenceGenomeSizeInBases)
        }

        return coverageStatistic
    }

    /**
     * returns the time a job has spent queueing and processing in milliseconds and as a percentage of the total time as
     * a map
     * @return map ["queue": [percentage: q_p, ms: q_ms], "process": [percentage: p_p, ms: p_ms]]
     */
    Map<String, Map<String, Long>> findJobSpecificStatesTimeDistributionByJobId(Long id) {
        ClusterJob job = ClusterJob.get(id)
        if (!job) {
            return [:]
        }

        Duration queue = Duration.between(job.queued, job.started)
        Duration process = Duration.between(job.started, job.ended)

        Long processMillis = Math.max(0, process.toMillis())
        Long queueMillis = Math.max(0, queue.toMillis())

        Long total = queueMillis + processMillis
        Long percentageProcess = Math.round(FACTOR_100 / total * processMillis)
        Long percentageQueue = FACTOR_100 - percentageProcess

        return [
                "process": [
                        percentage: percentageProcess,
                        ms        : process.toMillis(),
                ],
                "queue"  : [
                        percentage: percentageQueue,
                        ms        : queue.toMillis(),
                ],
        ]
    }

    /**
     * returns the latest Job Date
     * @return latest Job Date (queued)
     */
    ZonedDateTime getLatestJobDate() {
        Sql sql = new Sql(dataSource)

        String query = """
SELECT
 job.queued AS latestQueued
 FROM cluster_job AS job
 ORDER BY job.queued DESC
 LIMIT 1
"""

        Long latestJobDateAsLong = null

        sql.eachRow(query) {
            latestJobDateAsLong = it.latestQueued
        }

        return latestJobDateAsLong ? TimeUtils.fromMillis(latestJobDateAsLong) : null
    }

    /**
     * returns an amount of labels aligned for scatter charts in dependence to a given maximum value
     * @return List of dates as Strings and Integer-values
     * e.g. max = 8000 and quot = 10, return = [["800", "1600", "2400", ... , "8000"], [800, 1600, 2400, ... , 8000] ]
     */
    List<List> getLabels(Long max, int quot) {
        List<Double> labels = (1..quot).collect {
            (max / quot * it).toDouble().round(FACTOR_2)
        }
        List<String> labelsAsString = labels*.toString()

        return [labelsAsString, labels]
    }

    /**
     * Helper to convert dates and use it
     */
    static class DateTimeInterval {
        final ZonedDateTime startDate
        final ZonedDateTime endDate

        DateTimeInterval(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate.atStartOfDay(ZoneId.systemDefault())
            this.endDate = endDate.atStartOfDay(ZoneId.systemDefault()).plusDays(1)
        }

        List<Long> asMillis() {
            return [
                    TimeUtils.toMillis(startDate),
                    TimeUtils.toMillis(endDate),
            ]
        }
    }

    static class DateTimeIntervalWithHourBuckets {

        @Delegate
        final DateTimeInterval dateTimeInterval

        final List<ZonedDateTime> hourBuckets

        DateTimeIntervalWithHourBuckets(LocalDate startDate, LocalDate endDate) {
            dateTimeInterval = new DateTimeInterval(startDate, endDate)
            hourBuckets = getDaysAndHoursBetween(dateTimeInterval).asImmutable()
        }

        /**
         * returns all dates and hours between the two given dates as ZonedDateTime
         * e.g startDate = 2000-01-01, endDate = 2000-01-02
         * result = [2000-01-01 00:00:00, 2000-01-01 01:00:00, 2000-01-01 02:00:00, ... , 2000-03-01 00:00:00]
         */
        private List<ZonedDateTime> getDaysAndHoursBetween(DateTimeInterval startEndDateTime) {
            List<ZonedDateTime> daysArr = []
            ZonedDateTime date = startEndDateTime.startDate
            ZonedDateTime endDate = startEndDateTime.endDate.plusHours(1)

            while (date < endDate) {
                daysArr << date
                date = date.plusHours(1)
            }

            return daysArr
        }

        List<String> formattedHourBuckets() {
            return hourBuckets.collect {
                TimeFormats.DATE_TIME.getFormattedZonedDateTime(it)
            }
        }
    }
}
