package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.flowcontrol.ws.api.pbs.JobInfo
import de.dkfz.tbi.flowcontrol.ws.api.response.JobInfos
import de.dkfz.tbi.flowcontrol.ws.client.ClientKeys
import de.dkfz.tbi.flowcontrol.ws.client.FlowControlClient
import de.dkfz.tbi.otp.infrastructure.ClusterJob.Status
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import groovy.sql.Sql
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.LocalDate
import org.joda.time.Period

import javax.sql.DataSource
import javax.xml.ws.soap.SOAPFaultException

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static java.util.concurrent.TimeUnit.HOURS

class ClusterJobService {

    DataSource dataSource

    private static Map<Object, FlowControlClient> clientCache = [:]
    public static final String FORMAT_STRING = "yyyy-MM-dd HH:mm:ss"
    public static final Long HOURS_TO_MILLIS = HOURS.toMillis(1)

    /**
     * creates a cluster job object with at this time known attributes
     */
    public ClusterJob createClusterJob(Realm realm, String clusterJobId,
                                       ProcessingStep processingStep, SeqType seqType = null,
                                       String clusterJobName = processingStep.getPbsJobDescription(),
                                       String jobClass = processingStep.nonQualifiedJobClass) {
        ClusterJob job = new ClusterJob(
                                    processingStep: processingStep,
                                    realm: realm,
                                    clusterJobId: clusterJobId,
                                    clusterJobName: clusterJobName,
                                    jobClass: jobClass,
                                    seqType: seqType,
                                    queued: new DateTime()
                                ).save(flush: true)
        assert job != null
        return job
    }

    /**
     * completes the specific cluster job object of the given jobID
     * with the missing attributes via flowcontrol API
     */
    public void completeClusterJob(ClusterJobIdentifier jobIdentifier) {
        ClusterJob job
        if (jobIdentifier.realm != null) {
            job = ClusterJob.findByClusterJobIdentifier(jobIdentifier)
        } else {
            job = exactlyOneElement(ClusterJob.findAllByClusterJobId(jobIdentifier.clusterJobId))
        }
        JobInfo info = getClusterJobInformation(job)
        if (info) {
            job.exitStatus = Status.valueOf(info.getState() as String)
            job.exitCode = info.getExitcode()
            job.started = new DateTime(info.getStarted())
            job.ended = new DateTime(info.getEnded())
            job.usedCores = null
            job.cpuTime = new Duration(info.getCputimeMS())
            job.usedMemory = info.getMemoryUsedKB()
            job.requestedWalltime = new Duration(info.getWalltimeRequestedMS())
            job.requestedMemory = info.getMemoryRequestedKB()
            job.requestedCores = info.getCores()
            assert job.save(flush: true)
        }
    }

    /**
    * returns all information stored on the cluster
    * for the given job through its jobID
    *
    * returns null if the connection properties are not defined for the {@link Realm} of the given {@link ClusterJobIdentifier}
    *
    * @return JobInfo object
    */
    public JobInfo getClusterJobInformation(ClusterJob clusterJob) {
        JobInfo info
        try {
            info = getClusterJobInfo(clusterJob)
        } catch (SOAPFaultException e) {
            def cacheKey = Collections.unmodifiableList([clusterJob.realm.flowControlHost, clusterJob.realm.flowControlPort, clusterJob.realm.flowControlKey])
            synchronized (clientCache) {
                clientCache.remove(cacheKey)
            }
            info = getClusterJobInfo(clusterJob)
        }
        return info
    }

    private JobInfo getClusterJobInfo(ClusterJob clusterJob) {
        FlowControlClient client = getFlowControlClient(clusterJob.realm)
        if (client == null) {
            return null
        }
        JobInfos infos = client.requestJobInfos(clusterJob.clusterJobId)
        JobInfo info = infos.getJobInfo(clusterJob.clusterJobId)
        if (info == null) {
            throw new RuntimeException("FlowControl returned no information for ${clusterJob}.")
        }
        return info
    }

    /**
     * returns null if the connection properties are not defined for the given {@link Realm}
     *
     * @return FlowControlClient object
     */
    public FlowControlClient getFlowControlClient(Realm realm) {
        if (!realm.flowControlHost && !realm.flowControlPort && !realm.flowControlKey) {
            return null
        }
        def cacheKey = Collections.unmodifiableList([realm.flowControlHost, realm.flowControlPort, realm.flowControlKey])
        FlowControlClient client
        synchronized (clientCache) {
            client = clientCache.get(cacheKey)
            if (client == null) {
                client = createFlowControlClient(realm.flowControlKey, realm.flowControlHost, realm.flowControlPort)
                clientCache.put(cacheKey, client)
            }
        }
        return client
    }

    /**
     * builds a FlowControlClient object
     * @return FlowControlClient object
     */
    public FlowControlClient createFlowControlClient(String key, String host, int port) {
        return new FlowControlClient.Builder(new ClientKeys(key)).port(port).host(host).build()
    }

    /**
     * return the specific Individual to a cluster job
     * @return Individual or null
     */
    public Individual findIndividualByClusterJob(ClusterJob job) {
        return atMostOneElement(ProcessParameter.findAllByProcess(job.processingStep.process))?.toObject()?.individual
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
 job.queued >= ?
 AND job.ended < ?
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
 job.queued >= ?
 AND job.ended < ?
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
 job.queued >= ?
 AND job.ended < ?
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
 job.queued >= ?
 AND job.ended < ?
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

        Map data = ["queued":[], "started":[], "ended":[]]

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
 job.queued >= ?
 AND job.ended < ?
 AND job.exit_status != 'FAILED'
 AND job.ended > job.started
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
 job.queued >= ?
 AND job.ended < ?
 AND job.exit_status != 'FAILED'
 GROUP BY job.started / $HOURS_TO_MILLIS, job.ended / $HOURS_TO_MILLIS
"""

        List results = []

        sql.eachRow(query, [startDate.millis, endDate.millis]) {
            results << [startDate: new DateTime(it.hourStarted * HOURS_TO_MILLIS), endDate: new DateTime(it.hourEnded * HOURS_TO_MILLIS), memoryAvgUsed: it.sumAvgMemoryUsed.div(1024 * 1024)]
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
 job.queued >= ?
 AND job.ended < ?
 AND job.queued IS NOT NULL
 AND job.ended IS NOT NULL
 AND job.exit_status != 'FAILED'
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
 job.queued >= ?
 AND job.ended < ?
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
 job.queued >= ?
 AND job.ended < ?
 AND job.job_class = ?
 AND job.seq_type_id = ?
 GROUP BY job.exit_code
 ORDER BY job.exit_code
"""

        List exitCodeOccurenceList = []

        sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType.id]) {
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
 job.queued >= ?
 AND job.ended < ?
 AND job.job_class = ?
 AND job.seq_type_id = ?
 GROUP BY job.exit_status
 ORDER BY job.exit_status
"""

        List exitStatusOccurenceList = []

        sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType.id]) {
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

        Map data = ["queued":[], "started":[], "ended":[]]

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

            sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType.id]) {
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
 job.ended - job.started AS elapsedWalltime,
 job.requested_walltime AS requestedWalltime,
 job.id AS id
 FROM cluster_job AS job
 WHERE
 job.queued >= ?
 AND job.ended < ?
 AND job.queued IS NOT NULL
 AND job.ended IS NOT NULL
 AND job.exit_status != 'FAILED'
 AND job.job_class = ?
 AND job.seq_type_id = ?
"""

        def walltimeData = []

        sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType.id]) {
            walltimeData << [it.elapsedWalltime, it.requestedWalltime, it.id]
        }

        def xAxisMax = walltimeData ? walltimeData*.get(0).max() : 0
        def labels = xAxisMax != 0 ? getLabels(xAxisMax, 10) : []

        return ["data": walltimeData, "labels": labels, "xMax": xAxisMax]
    }

    /**
     * returns requested and elapsed memories of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    public Map findJobClassAndSeqTypeSpecificMemoriesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 job.used_memory AS usedMemory,
 job.requested_memory AS requestedMemory,
 job.id AS id
 FROM cluster_job AS job
 WHERE
 job.queued >= ?
 AND job.ended < ?
 AND job.queued IS NOT NULL
 AND job.ended IS NOT NULL
 AND job.exit_status != 'FAILED'
 AND job.job_class = ?
 AND job.seq_type_id = ?
"""

        def memoryData = []

        sql.eachRow(query, [startDate.millis, endDate.millis, jobClass, seqType.id]) {
            memoryData << [it.usedMemory, it.requestedMemory, it.id]
        }

        def xAxisMax = memoryData ? memoryData*.get(0).max() : 0
        def labels = xAxisMax != 0 ? getLabels(xAxisMax, 10) : []

        return ["data": memoryData, "labels": labels, "xMax": xAxisMax]
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
 job.queued >= ?
 AND job.ended < ?
 AND job.exit_status != 'FAILED'
 AND job.job_class = ?
 AND job.seq_type_id = ?
"""

        Double avgCpu

        sql.query(query, [startDate.millis, endDate.millis, jobClass, seqType.id], {
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
 job.queued >= ?
 AND job.ended < ?
 AND job.exit_status != 'FAILED'
 AND job.job_class = ?
 AND job.seq_type_id = ?
"""

        Integer avgMemory

        sql.query(query, [startDate.millis, endDate.millis, jobClass, seqType.id], {
            it.next()
            avgMemory = it.getLong('avgMemory')
        })

        return avgMemory ?: 0
    }

    /**
     * returns the average time in queue and average processing time for a specific jobclass and seqtype
     * @return map ["avgQueue": avgQueue, "avgProcess": avgProcess]
     */
    public Map findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def sql = new Sql(dataSource)

        String query = """
SELECT
 CAST (AVG (job.started - job.queued) AS int) AS avgQueue,
 CAST (AVG (job.ended - job.started) AS int) AS avgProcess
 FROM cluster_job AS job
 WHERE
 job.queued >= ?
 AND job.ended < ?
 AND job.queued IS NOT NULL
 AND job.ended IS NOT NULL
 AND job.exit_status != 'FAILED'
 AND job.job_class = ?
 AND job.seq_type_id = ?
"""

        Long avgQueue, avgProcess

        // prevents negative values that could appear cause of rounding errors with milliseconds values
        // OTP-1304, queued gets set through OTP, started & ended gets set by the cluster
        sql.query(query, [startDate.millis, endDate.millis, jobClass, seqType.id], {
            it.next()
            avgQueue =  Math.max(0, it.getLong('avgQueue') ?: 0)
            avgProcess = Math.max(0, it.getLong('avgProcess') ?: 0)
        })

        return ["avgQueue": avgQueue, "avgProcess": avgProcess]
    }

    /**
     * returns the time in queue and the processing time of a specific job
     * @return map ["queue": [percentQueue, queueInMillis], "process": [percentProcess, processInMillis]]
     */
    public Map findJobSpecificStatesTimeDistributionByJobId(Long id) {
        def sql = new Sql(dataSource)

        ClusterJob job = ClusterJob.get(id)
        if(!job) {return null}

        Duration queue = new Duration(job.queued, job.started)
        Duration process = new Duration(job.started, job.ended)

        def queueMillis = Math.max(0, queue.getMillis())
        def processMillis = Math.max(0, process.getMillis())

        long total = queueMillis + processMillis

        int pProcess = Math.round(100/ total * processMillis)
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
            (max/quot*it).toDouble().round(2)
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
