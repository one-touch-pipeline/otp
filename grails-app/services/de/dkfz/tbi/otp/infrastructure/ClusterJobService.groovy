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
import org.joda.time.format.DateTimeFormat

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
    public Individual findIndividualByClusterJob(ClusterJob job) {
        return atMostOneElement(ProcessParameter.findAllByProcess(job.processingStep.process))?.toObject()?.individual
    }

    /**
     * returns a List of Cluster Jobs in a specific time span
     * @return List [clusterJob1, clusterJob2, ...]
     */
    public List findAllClusterJobsByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        return ClusterJob.createCriteria().list {
            order('queued', 'asc')
            ge('queued', startDate)
            le('ended', endDate)
        }
    }

    /**
     * returns a unique list of job classes in a specific time-span
     * existing in the Cluster Job table
     */
    public List findAllJobClassesByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)
        return ClusterJob.createCriteria().list {
            ge('queued', startDate)
            lt('ended', endDate)
            projections {
                distinct ('jobClass')
                order('jobClass', 'asc')
            }
        }
    }

    /**
     * returns a List of exit codes and their occurrence in a specific time span
     * @return ArrayList [[exitCode, number of occurrences], ...]
     */
    public List findAllExitCodesByDateBetween(LocalDate sDate, LocalDate eDate) {
        return findAllByPropertyAndDateBetween("exitCode", sDate, eDate)
    }

    /**
     * returns a List of exit statuses and their occurrence in a specific time span
     * @return ArrayList [[exitStatus, number of occurrences], ...]
     */
    public List findAllExitStatusesByDateBetween(LocalDate sDate, LocalDate eDate) {
        return findAllByPropertyAndDateBetween("exitStatus", sDate, eDate)
    }

    /**
     * returns all failed jobs and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 4 jobs failed processing
     */
    public Map findAllFailedByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)
        def dateTimeFormatter = DateTimeFormat.forPattern(FORMAT_STRING)

        def result = ClusterJob.createCriteria().list {
            ge('queued', startDate)
            lt('ended', endDate)
            eq('exitStatus', Status.FAILED)
        }

        def data = hourBuckets.collect { cHour ->
            DateTime currentHour = dateTimeFormatter.parseDateTime(cHour);
            DateTime nextHour = currentHour.plusHours(1)

            return result.count {
                currentHour <= it.ended && it.ended < nextHour
            }
        }

        return ["days": hourBuckets, "data": data]
    }

    /**
     * returns all states and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: ['queued': [0, ...], 'started': [1, ...], 'ended': [0, ...]]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 0 jobs have been queued, 1 job has been started and 0 jobs have been ended
     */
    public Map findAllStatesByDateBetween(LocalDate sDate, LocalDate eDate) {
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
    public Map findAllAvgCoreUsageByDateBetween(LocalDate sDate, LocalDate eDate) {
        return groupAndAnalyseClusterJobsByHour(sDate, eDate, {jobList -> jobList*.getCpuAvgUtilised().sum() as Integer ?: 0})
    }

    /**
     * returns the memory usages and its number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [2048, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 2048 mb memory was used to process the jobs
     */
    public Map findAllMemoryUsageByDateBetween(LocalDate sDate, LocalDate eDate) {
        return groupAndAnalyseClusterJobsByHour(sDate, eDate, {jobList -> jobList ? (jobList*.usedMemory.sum().div(1024 * 1024)) as Integer : 0})
    }

    public Map findAllStatesTimeDistributionByDateBetween(LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def result = ClusterJob.createCriteria().list {
            isNotNull('started')
            isNotNull('ended')
            ge('queued', startDate)
            lt('ended', endDate)
        }

        long queue = 0
        long process = 0

        result.each {
            queue += new Duration(it.queued, it.started).getMillis()
            process += new Duration(it.started, it.ended).getMillis()
        }

        int percentQueue = 0
        int percentProcess = 0

        if(queue != 0 && process != 0) {
            percentQueue = Math.round(100 / (queue + process) * queue)
            percentProcess = 100 - percentQueue
        }

        String queuePeriod = new Period(queue).getHours().toString()
        String processPeriod = new Period(process).getHours().toString()

        return ["queue": [percentQueue, queuePeriod], "process": [percentProcess, processPeriod]]
    }

    /**
     * returns a unique list of sequencing types
     * existing in the Cluster Job table
     */
    public List findJobClassSpecificSeqTypesByDateBetween(String jobClass, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        return ClusterJob.createCriteria().list {
            ge('queued', startDate)
            lt('ended', endDate)
            eq('jobClass', jobClass)
            projections {
                distinct("seqType")
                order('seqType', 'asc')
            }
        }
    }

    /**
     * returns a list of exit codes and their count of
     * a specific job class and sequencing type, in a specific time span
     * @return list of arrays containing the exitCode and its count
     * [0] = exitCode
     * [1] = count of exitCode
     */
    public List findJobClassAndSeqTypeSpecificExitCodesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        return groupClusterJobsBetweenByProperty(sDate, eDate, jobClass, seqType, 'exitCode')
    }

    /**
     * returns a list of exit statuses and their count of
     * a specific job class and sequencing type, in a specific time span
     * @return list of arrays containing the exitStatuses and its count
     * [0] = exitStatus
     * [1] = count of exitCode
     */
    public List findJobClassAndSeqTypeSpecificExitStatusesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        return groupClusterJobsBetweenByProperty(sDate, eDate, jobClass, seqType, 'exitStatus')
    }

    /**
     * returns all states per hour at a given time span of a specific job class and sequencing type and
     * and array of all dates (per hour) existing in the given time span
     * @return ["days": [day1Hour0, day2Hour1, ...], "data": ["queued": [...], "started": [...], "ended":[...]]]
     */
    public Map findJobClassAndSeqTypeSpecificStatesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
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

        return ["days": hourBuckets, "data": data]
    }

    /**
     * returns requested and elapsed walltimes of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    public Map findJobClassAndSeqTypeSpecificWalltimesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
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
    public Map findJobClassAndSeqTypeSpecificMemoriesByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        List<ClusterJob> results = clusterJobsInsideIntervalBy(jobClass, seqType, sDate, eDate)

        def finalResults = results.collect ({
            return [it.usedMemory, it.requestedMemory, it.id]
        })

        def xAxisMax = finalResults ? finalResults*.get(0).max() : 0
        def labels = xAxisMax != 0 ? getLabels(xAxisMax, 10) : []

        return ["data": finalResults, "labels": labels, "xMax": xAxisMax]
    }

    /**
     * returns the average core usage of a specific jobclass and seq type
     * @return double
     */
    public double findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        List<ClusterJob> results = clusterJobsInsideIntervalBy(jobClass, seqType, sDate, eDate)

        double avgCpuUsage = results ? results*.getCpuAvgUtilised().sum() / results.size() : 0

        return avgCpuUsage.round(2)
    }

    /**
     * return the average memory usage of a specific jobclass and seqtype
     * @return int
     */
    public int findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        def result = ClusterJob.createCriteria().list {
            eq('jobClass', jobClass)
            eq('seqType', seqType)
            ge('queued', startDate)
            lt('ended', endDate)
            projections {
                avg('usedMemory')
            }
        }

        return Math.round(result[0] ?: 0)
    }

    /**
     * returns the average time in queue and average processing time for a specific jobclass and seqtype
     * @return map ["avgQueue": avgQueue, "avgProcess": avgProcess]
     */
    public Map findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        List<ClusterJob> results = clusterJobsInsideIntervalBy(jobClass, seqType, sDate, eDate)

        def queue = 0
        def process = 0

        results.each { job ->
            queue += new Duration(job.queued, job.started).getMillis()
            process += new Duration(job.started, job.ended).getMillis()
        }

        // prevents negative values that could appear cause of rounding errors with milliseconds values
        // OTP-1304, queued gets set through OTP, started & ended gets set by the cluster

        def avgQueue = Math.max(0, Math.round(queue / results.size()))
        def avgProcess = Math.max(0, Math.round(process / results.size()))

        return ["avgQueue": avgQueue, "avgProcess": avgProcess]
    }

    /**
     * returns the time in queue and the processing time of a specific job
     * @return map ["queue": [percentQueue, queueFormatted], "process": [percentProcess, processFormatted]]
     */
    public Map findJobSpecificStatesTimeDistributionByJobId(Long id) {
        ClusterJob job = ClusterJob.get(id)
        if(!job) {return null}

        Duration queue = new Duration(job.queued, job.started)
        Duration process = new Duration(job.started, job.ended)

        // prevents negative values that could appear cause of rounding errors with milliseconds values
        // OTP-1304, queued gets set through OTP, started & ended gets set by the cluster

        def queueMillis = Math.max(0, queue.getMillis())
        def processMillis = Math.max(0, process.getMillis())

        long total = queueMillis + processMillis

        int percentProcess = Math.round(100/ total * processMillis)
        int percentQueue = 100 - percentProcess

        return ["queue": [percentQueue, queueMillis], "process": [percentProcess, processMillis]]
    }

    /**
     * returns all dates and hours between the two given dates as Strings
     * e.g startDate = 2000-01-01, endDate = 2000-01-02
     * result = [2000-01-01 00:00:00, 2000-01-01 01:00:00, 2000-01-01 02:00:00, ... , 2000-03-01 00:00:00]
     */
    public List getDaysAndHoursBetween(LocalDate sDate, LocalDate eDate) {
        def daysArr = []

        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        while (startDate < endDate.plusHours(1)) {
            daysArr << startDate.toString(FORMAT_STRING)
            startDate = startDate.plusHours(1)
        }

        return daysArr
    }

    /**
     * returns the latest Job Date
     * @return latest Job Date (queued)
     */
    public LocalDate getLatestJobDate() {
        def result = ClusterJob.createCriteria().list {
            order('queued', 'desc')
            maxResults(1)
        }

        return result ? new LocalDate(result.first().queued) : null
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
     * returns a List of property-values and their occurrence in a specific time span
     * @return List of property-values and their occurence
     * e.g. exitStatus => ["completed": 5, "failed": 2]
     */
    private List findAllByPropertyAndDateBetween(String property, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        return ClusterJob.createCriteria().list {
            ge('queued', startDate)
            lt('ended', endDate)
            order(property, 'asc')
            projections {
                groupProperty(property)
                count('id')
            }
        }
    }

    /**
     * returns a map that contains a list of 24 formatted hour labels between sDate and eDate and a List with the occurence
     * of jobs in this time-span dependent of the analysis-closure
     * @return Map["days": [], "data": []]
     * days = list of 24 formatted hour labels
     * data = list with the occurence of jobs
     */
    private Map<String, ArrayList<String>> groupAndAnalyseClusterJobsByHour(LocalDate sDate, LocalDate eDate, Closure analysis) {
        def (DateTime startDate, DateTime endDate, ArrayList<String> hourBuckets) = parseArgs(sDate, eDate)
        def results = clusterJobsInsideInterval(startDate, endDate)
        def dateTimeFormatter = DateTimeFormat.forPattern(FORMAT_STRING)

        def data = hourBuckets.collect { cHour ->
            DateTime currentHour = dateTimeFormatter.parseDateTime(cHour)
            def nextHour = currentHour.plusHours(1)

            def jobsThisHour = results.findAll {
                currentHour <= it.ended && it.ended < nextHour || it.started < nextHour && nextHour < it.ended
            }

            return analysis(jobsThisHour)
        }

        return ["days": hourBuckets, "data": data]
    }

    /**
     * returns a list of ClusterJobs that queued later startDate and ended before endDate
     * @return List of ClusterJobs
     */
    private List<ClusterJob> clusterJobsInsideInterval(DateTime startDate, DateTime endDate) {
        return ClusterJob.createCriteria().list {
            ge('queued', startDate)
            lt('ended', endDate)
        }
    }

    /**
     * returns a list of ClusterJobs that queued later startDate and ended before endDate dependent on jobClass and seqType
     * @return List of ClusterJobs
     */
    private List<ClusterJob> clusterJobsInsideIntervalBy(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        return ClusterJob.createCriteria().list {
            eq('jobClass', jobClass)
            eq('seqType', seqType)
            ge('queued', startDate)
            lt('ended', endDate)
        }
    }

    /**
     * returns a list of property-values and their occurence of clusterJobs that queued later startDate and ended before endDate dependent on jobClass and sequencing type
     * @return List of property-values and their occurence
     * e.g. exitStatuses => ["completed": 5, "failed": 2]
     */
    private def groupClusterJobsBetweenByProperty(LocalDate sDate, LocalDate eDate, String jobClass, SeqType seqType, String property) {
        def (DateTime startDate, DateTime endDate) = parseDateArgs(sDate, eDate)

        return ClusterJob.createCriteria().list {
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
