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
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.LocalDate
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

import javax.xml.ws.soap.SOAPFaultException

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ClusterJobService {

    private static Map<Object, FlowControlClient> clientCache = [:]
    public static final String FORMAT_STRING = "yyyy-MM-dd HH:mm:ss"

    /**
     * creates a cluster job object with at this time known attributes
     */
    public ClusterJob createClusterJob(Realm realm, String clusterJobId, ProcessingStep processingStep, SeqType seqType, String clusterJobName = null) {
        String cName = clusterJobName ?: processingStep.getPbsJobDescription()
        ClusterJob job = new ClusterJob(
                                    processingStep: processingStep,
                                    realm: realm,
                                    clusterJobId: clusterJobId,
                                    clusterJobName: cName,
                                    jobClass: processingStep.nonQualifiedJobClass,
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
            job = exactlyOneElement(ClusterJob.findAllByRealmAndClusterJobId(jobIdentifier.realm, jobIdentifier.clusterJobId))
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
    public Individual getIndividualByClusterJob(ClusterJob job) {
        Individual individual = atMostOneElement(ProcessParameter.findAllByProcess(job.processingStep.process))?.toObject()?.individual
        return individual
    }

    /**
     * returns an ArrayList of exit codes and their appearance over all cluster jobs
     * @return ArrayList [[exitCode, number of occurrences], ...]
     */
    public List getAllExitCodes() {
        return groupAndCountBy('exitCode')
    }

    /**
     * returns an ArrayList of exit statuses and their appearance over all cluster jobs
     * @return ArrayList [[exitStatus, number of occurrences], ...]
     */
    public List getAllExitStatuses() {
        return groupAndCountBy('exitStatus')
    }

    private List groupAndCountBy(String property) {
        def c = ClusterJob.createCriteria()
        def result = c.list {
            order(property, 'asc')
            projections {
                groupProperty(property)
                count('id')
            }
        }
        return result
    }

    /**
     * returns all failed jobs and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
<<<<<<< HEAD
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 4 jobs failed processing
     */
    public Map getAllFailedByDate(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)
        def results = ClusterJob.createCriteria().list {
            ge('queued', startDate)
            lt('ended', endDate)
            eq('exitStatus', Status.FAILED)
        }
        def dateTimeFormatter = DateTimeFormat.forPattern(FORMAT_STRING)
        def data = hourBuckets.collect { cHour ->
            DateTime currentHour = dateTimeFormatter.parseDateTime(cHour);
            def nextHour = currentHour.plusHours(1)

            def jobsThisHour = []
            def iter = results.iterator() // use plain-old java version, so we can do it.remove for optimisation
            while (iter.hasNext()) {
                def job = iter.next()

                // jobs ended in this bucket must be counted
                // and can be ignored for subsequent buckets
                if (currentHour <= job.ended && job.ended < nextHour) {
                    jobsThisHour << job
                    iter.remove() // reduce search space for next hourbucket
                }
            }

            return jobsThisHour.size()
        }
        return ["days": hourBuckets, "data": data]
    }

    /**
     * returns all states and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: ['queued': [0, ...], 'started': [1, ...], 'ended': [0, ...]]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 0 jobs have been queued, 1 job has been started and 0 jobs have been ended
     */
    public Map getAllStatesByDate(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)
        Map data = ["queued":[], "started":[], "ended":[]]
        def dateTimeFormatter = DateTimeFormat.forPattern(FORMAT_STRING)
        data.keySet().each { state ->
            def results = ClusterJob.createCriteria().list {
                ge(state, startDate)
                lt(state, endDate)
            }
            data."${state}".addAll(hourBuckets.collect({ cHour ->
                DateTime currentHour = dateTimeFormatter.parseDateTime(cHour);
                def nextHour = currentHour.plusHours(1)
                def jobsThisHour = results.grep {
                    it."${state}" >= currentHour && it."${state}" <  nextHour
                }
                return jobsThisHour.size()
            }))
        }
        return ["days": hourBuckets, "data": data]
    }

    /**
     * returns the average core usages and its number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 4 cores were used to process the jobs
     */
    public Map getAllAvgCoreUsageByDate(LocalDate sDate, LocalDate eDate) {
        return groupAndAnalyseClusterJobsByHour(sDate, eDate, {jobList -> jobList*.getCpuAvgUtilised().sum() as Integer ?: 0})
    }

    /**
     * returns the memory usages and its number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [2048, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 2048 mb memory was used to process the jobs
     */
    public Map getAllMemoryUsageByDate(LocalDate sDate, LocalDate eDate) {
        return groupAndAnalyseClusterJobsByHour(sDate, eDate, {jobList -> jobList ? (jobList*.usedMemory.sum().div(1024 * 1024)) as Integer : 0})
    }

    private LinkedHashMap<String, ArrayList<String>> groupAndAnalyseClusterJobsByHour(LocalDate sDate, LocalDate eDate, Closure analysis) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)
        def results = clusterJobsInsideInterval(startDate, endDate)
        def dateTimeFormatter = DateTimeFormat.forPattern(FORMAT_STRING)
        def data = hourBuckets.collect { cHour ->
            DateTime currentHour = dateTimeFormatter.parseDateTime(cHour)
            def nextHour = currentHour.plusHours(1)

            def jobsThisHour = []
            def iter = results.iterator() // use plain-old java version, so we can do it.remove for optimisation
            while (iter.hasNext()) {
                def job = iter.next()

                // jobs ended in this bucket must be counted
                // and can be ignored for subsequent buckets
                if (currentHour <= job.ended && job.ended < nextHour) {
                    jobsThisHour << job
                    iter.remove() // reduce search space for next hourbucket
                }

                // jobs running and/or started now, but not ending now must be counted
                // but cannot be removed, because they need to be counted again in next hour
                if (job.started < nextHour && nextHour < job.ended) {
                    jobsThisHour << job
                }
            }

            return analysis(jobsThisHour)
        }
        return ["days": hourBuckets, "data": data]
    }

    private List<ClusterJob> clusterJobsInsideInterval(DateTime startDate, DateTime endDate) {
        return ClusterJob.createCriteria().list {
            ge('queued', startDate)
            lt('ended', endDate)
        }
    }

    /**
     * returns the time in queue and the processing time as sum of all jobs
     * @return map ["queue": [percentQueue, queueFormatted], "process": [percentProcess, processFormatted]]
     */
    public Map getAllStatesTimeDistribution() {
        def results = ClusterJob.createCriteria().list {
            isNotNull('started')
            isNotNull('ended')
        }

        long queue = 0
        long process = 0

        results.each { job ->
            queue += new Duration(job.queued, job.started).getMillis()
            process += new Duration(job.started, job.ended).getMillis()
        }

        int percentQueue = Math.round(100/ (queue + process) * queue)
        int percentProcess = 100 - percentQueue

        String queueFormatted = new Period(queue).getHours().toString() + " hours"
        String processFormatted = new Period(process).getHours().toString() + " hours"

        Map result = ["queue": [percentQueue, queueFormatted], "process": [percentProcess, processFormatted]]
        return result
    }

    /**
     * returns a unique list of sequencing types
     * existing in the Cluster Job table
     */
    public List getSeqTypes(String jobClass) {
        def c = ClusterJob.createCriteria()
        def result = c.list {
            eq('jobClass', jobClass)
            projections {
                distinct("seqType")
                order('seqType', 'asc')
            }
        }
        return result
    }

    /**
     * returns a unique list of job classes
     * existing in the Cluster Job table
     */
    public List getJobClasses(String sDate, String eDate) {
        DateTime startDate = LocalDate.parse(sDate).toDateTimeAtStartOfDay()
        DateTime endDate = LocalDate.parse(eDate).plusDays(1).toDateTimeAtStartOfDay()
        def result = ClusterJob.createCriteria().list {
            ge('started', startDate)
            lt('ended', endDate)
            projections {
                distinct ('jobClass')
                order('jobClass', 'asc')
            }
        }
        return result
    }

    /**
     * returns a list of exit codes and their count of
     * a specific job class and sequencing type, in a specific time span
     * @return list of arrays containing the exitCode and its count
     * [0] = exitCode
     * [1] = count of exitCode
     */
    public List getJobTypeSpecificExitCodes(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        return groupClusterJobsBetweenByProperty(sDate, eDate, jobClass, seqType, 'exitCode')
    }

    /**
     * returns a list of exit statuses and their count of
     * a specific job class and sequencing type, in a specific time span
     * @return list of arrays containing the exitStatuses and its count
     * [0] = exitCode
     * [1] = count of exitCode
     */
    public List getJobTypeSpecificExitStatuses(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        return groupClusterJobsBetweenByProperty(sDate, eDate, jobClass, seqType, 'exitStatus')
    }

    private def groupClusterJobsBetweenByProperty(LocalDate sDate, LocalDate eDate, String jobClass, SeqType seqType, String property) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def results = ClusterJob.createCriteria().list {
            eq('jobClass', jobClass)
            eq('seqType', seqType)
            ge('queued', startDate)
            lt('ended', endDate)
            order(property, 'asc')
            projections {
                groupProperty(property)
                count('id')
            }
        }
        return results
    }

    /**
     * returns all states per hour at a given time span of a specific job class and sequencing type and
     * and array of all dates (per hour) existing in the given time span
     * @return ["days": [day1Hour0, day2Hour1, ...], "data": ["queued": [...], "started": [...], "ended":[...]]]
     */
    public Map getJobTypeSpecificStates(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)
        Map data = ["queued":[], "started":[], "ended":[]]
        data.keySet().each { state ->
            def results = ClusterJob.createCriteria().list {
                eq('jobClass', jobClass)
                eq('seqType', seqType)
                ge(state, startDate)
                lt(state, endDate)
            }
            def dateTimeFormatter = DateTimeFormat.forPattern(FORMAT_STRING)
            data."${state}".addAll(hourBuckets.collect({ cHour ->
                DateTime currentHour = dateTimeFormatter.parseDateTime(cHour);
                def nextHour = currentHour.plusHours(1)
                def jobsThisHour = results.grep {
                    it."${state}" >= currentHour && it."${state}" <  nextHour
                }
                return jobsThisHour.size()
            }))
        }
        Map result = ["days": hourBuckets, "data": data]
        return result
    }

    /**
     * returns requested and elapsed walltimes of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    public Map getJobTypeSpecificWalltimes(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        List<ClusterJob> results = clusterJobsInsideIntervalBy(jobClass, seqType, sDate, eDate)

        def finalResults = results.collect ({
            return [it.elapsedWalltime.getMillis(), it.requestedWalltime.getMillis(), it.id]
        })

        def xAxisMax = finalResults ? finalResults*.get(0).max() : 0
        def labels = xAxisMax != 0 ? getLabels(xAxisMax, 10) : []

        return ["data": finalResults, "labels": labels, "xMax": xAxisMax]
    }

    /**
     * returns requested and elapsed memories of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    public Map getJobTypeSpecificMemories(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        List<ClusterJob> results = clusterJobsInsideIntervalBy(jobClass, seqType, sDate, eDate)

        def finalResults = results.collect ({
            return [(it.usedMemory.div(1024 * 1024) as double).round(2), (it.requestedMemory.div(1024 * 1024) as double).round(2), it.id]
        })

        def xAxisMax = finalResults ? finalResults*.get(0).max() : 0
        def labels = xAxisMax != 0 ? getLabels(xAxisMax, 10) : []

        return ["data": finalResults, "labels": labels, "xMax": xAxisMax]
    }

    /**
     * returns the average core usage of a specific jobclass and seq type
     * @return double
     */
    public double getJobTypeSpecificAvgCoreUsage(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        List<ClusterJob> results = clusterJobsInsideIntervalBy(jobClass, seqType, sDate, eDate)

        double avgCpuUsage = results*.getCpuAvgUtilised().sum()/results.size()

        return avgCpuUsage.round(2)
    }

    private List<ClusterJob> clusterJobsInsideIntervalBy(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        return ClusterJob.createCriteria().list {
            eq('jobClass', jobClass)
            eq('seqType', seqType)
            ge('queued', startDate)
            lt('ended', endDate)
        }
    }

    /**
     * return the average memory usage of a specific jobclass and seqtype
     * @return int
     */
    public int getJobTypeSpecificAvgMemory(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()

        def result = ClusterJob.createCriteria().list {
            eq('jobClass', jobClass)
            eq('seqType', seqType)
            ge('queued', startDate)
            lt('ended', endDate)
            projections {
                avg('usedMemory')
            }
        }

        return Math.round(result[0])
    }

    /**
     * returns the average time in queue and average processing time for a specific jobclass and seqtype
     * @return map ["avgQueue": avgQueue, "avgProcess": avgProcess]
     */
    public Map getJobTypeSpecificAvgStatesTimeDistribution(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        List<ClusterJob> results = clusterJobsInsideIntervalBy(jobClass, seqType, sDate, eDate)

        def queue = 0
        def process = 0

        results.each { job ->
            queue += new Duration(job.queued, job.started).getMillis()
            process += new Duration(job.started, job.ended).getMillis()
        }

        // prevents negative values that could appear cause of rounding errors with milliseconds values
        if (queue < 0) queue = 0

        def avgQueue = PeriodFormat.getDefault().print(new Period(Math.round(queue/results.size())))
        def avgProcess = PeriodFormat.getDefault().print(new Period(Math.round(process/results.size())))

        return ["avgQueue": avgQueue, "avgProcess": avgProcess]
    }

    /**
     * returns the time in queue and the processing time of a specific job
     * @return map ["queue": [percentQueue, queueFormatted], "process": [percentProcess, processFormatted]]
     */
    public Map getJobSpecificStatesTimeDistribution (int id) {
        ClusterJob job = ClusterJob.get(id)
        Duration queue = new Duration(job.queued, job.started)
        Duration process = new Duration(job.started, job.ended)

        int queueMillis = queue.getMillis()
        int processMillis = process.getMillis()

        // prevents negative values that could appear cause of rounding errors with milliseconds values
        if(queueMillis < 0) { queueMillis = 0 }
        if(processMillis < 0) { processMillis = 0}

        long total = queueMillis + processMillis

        int percentProcess = Math.round(100/ total * processMillis)
        int percentQueue = 100 - percentProcess

        String queueFormatted = PeriodFormat.getDefault().print(new Period(queueMillis))
        String processFormatted = PeriodFormat.getDefault().print(new Period(processMillis))

        Map result = ["queue": [percentQueue, queueFormatted], "process": [percentProcess, processFormatted]]
        return result
    }

    /**
     * returns all dates and hours between the two given dates as Strings
     * e.g startDate = 2000-01-01, endDate = 2000-01-02
     * result = [2000-01-01 00:00:00, 2000-01-01 01:00:00, 2000-01-01 02:00:00, ... , 2000-03-01 00:00:00]
     */
    public List getDaysAndHoursBetween(LocalDate startDate, LocalDate endDate) {
        def daysArr = []

        DateTime startD = startDate.toDateTimeAtStartOfDay()
        DateTime endD = endDate.toDateTimeAtStartOfDay()

        while (startD < endD.plusDays(1).plusHours(1)) {
            daysArr << startD.toString(FORMAT_STRING)
            startD = startD.plusHours(1)
        }
        return daysArr
    }

    /**
     * returns the latest Job Date as formatted String
     */
    public String getLatestJobDate() {
        def result = ClusterJob.createCriteria().list {
            order('queued', 'desc')
            maxResults(1)
        }
        return result.first().queued.toString(FORMAT_STRING)
    }

    /**
     * returns an amount of labels aligned for scatter RGraphs in dependence to a given maximum value
     * e.g. max = 8000 and quot = 10, return = [800, 1600, 2400, ... , 8000]
     */
    public List getLabels(max, quot) {
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
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        return [startDate, endDate, hourBuckets]
    }
}
