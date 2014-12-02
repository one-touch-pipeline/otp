package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqScan
import org.joda.time.Period
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.PeriodFormat

import java.text.SimpleDateFormat

import de.dkfz.tbi.otp.infrastructure.ClusterJob.Status

import de.dkfz.tbi.flowcontrol.ws.api.pbs.JobInfo
import de.dkfz.tbi.flowcontrol.ws.api.response.JobInfos
import de.dkfz.tbi.flowcontrol.ws.client.ClientKeys
import de.dkfz.tbi.flowcontrol.ws.client.FlowControlClient

import de.dkfz.tbi.otp.job.processing.ProcessingStep

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType


import javax.xml.ws.soap.SOAPFaultException

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import org.joda.time.Days
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormatter

class ClusterJobService {

    private static Map<Object, FlowControlClient> clientCache = [:]

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
     * @return Individual
     */
    public Individual getIndividualByClusterJob(ClusterJob job) {
        SeqScan seqScan = SeqScan.findBySeqType(job.seqType)
        return seqScan.sample.individual
    }

    /**
     * returns an ArrayList of exit codes and their appearance over all cluster jobs
     * @return ArrayList [[exitCode, number of occurrences], ...]
     */
    public ArrayList getAllExitCodes() {
        def c = ClusterJob.createCriteria()
        def result = c.list {
            order('exitCode', 'asc')
            projections {
                groupProperty('exitCode')
                count('id', 'exitCode')
            }
        }
        return result
    }

    /**
     * returns an ArrayList of exit statuses and their appearance over all cluster jobs
     * @return ArrayList [[exitStatus, number of occurrences], ...]
     */
    public ArrayList getAllExitStatuses() {
        def c = ClusterJob.createCriteria()
        def result = c.list {
            order('exitStatus', 'asc')
            projections {
                groupProperty('exitStatus')
                count('id', 'exitStatus')
            }
        }
        return result
    }

    /**
     * returns all failed jobs and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 0 jobs have been queued, 4 jobs failed processing
     */
    public Map getAllFailedByDate(LocalDate sDate, LocalDate eDate) {
        ArrayList<String> days = getDaysBetween(sDate, eDate)
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def data = []
        def c = ClusterJob.createCriteria()
        def results = c.list {
            between('ended', startDate, endDate)
            eq('exitStatus', Status.FAILED)
        }
        for (int i = 0; i <= days.size() - 1; i++) {
            Date day = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(days[i])
            def cData = 0
            for (r in results) {
                if (r.started.getMillis() <= day.getTime() && r.ended.getMillis() > day.getTime()) {
                    cData ++
                }
            }
            data.push(cData)
        }
        Map result = ["days": days, "data": data]
        return result
    }
    /**
     * returns all states and their number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: ['queued': [0, ...], 'started': [1, ...], 'ended': [0, ...]]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 0 jobs have been queued, 1 job has been started and 0 jobs have been ended
     */
    public Map getAllStatesByDate(LocalDate sDate, LocalDate eDate) {
        ArrayList<String> days = getDaysBetween(sDate, eDate)
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def states = ['queued', 'started', 'ended']
        def c
        Map results = [:]
        Map data = ["queued":[], "started":[], "ended":[]]
        for (state in states) {
            c = ClusterJob.createCriteria()
            results.put(state, c.list {
                between(state, startDate, endDate)
            })
        }
        for (int i = 0; i <= days.size() - 1; i++) {
            for (state in states) {
                def cData = 0
                for (s in results.getAt(state)) {
                    if (s."${state}".toString("yyyy-MM-dd hh") == days[i].split(":")[0]) {
                        cData ++
                    }
                }
                data.get(state).push(cData)
            }
        }
        Map result = ["days": days, "data": data]
        return result
    }

    /**
     * returns the average core usages and its number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [4, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 4 cores were used to process the jobs
     */
    public Map getAllAvgCoreUsageByDate(LocalDate sDate, LocalDate eDate) {
        ArrayList<String> days = getDaysBetween(sDate, eDate)
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def data = []
        def c = ClusterJob.createCriteria()
        def results = c.list {
            between('started', startDate, endDate)
        }
        for (int i = 0; i <= days.size() - 1; i++) {
            Date day = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(days[i])
            def cData = 0
            for (r in results) {
                if (r.started.getMillis() <= day.getTime() && r.ended.getMillis() > day.getTime()) {
                    cData += r.getCpuAvgUtilised()
                }
            }
            data.push(cData)
        }
        Map result = ["days": days, "data": data]
        return result
    }

    /**
     * returns the memory usages and its number of occurrences per hour at a given time span
     * @return map [days: ['2000-01-01 00:00:00', ...], data: [2048, ...]]
     * => at January 1st 2000, between 00:00:00 and 01:00:00, 2048 mb memory was used to process the jobs
     */
    public Map getAllMemoryUsageByDate(LocalDate sDate, LocalDate eDate) {
        ArrayList<String> days = getDaysBetween(sDate, eDate)
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def data = []
        def c = ClusterJob.createCriteria()
        def results = c.list {
            between('started', startDate, endDate)
        }
        for (int i = 0; i <= days.size() - 1; i++) {
            Date day = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(days[i])
            def cData = 0
            for (r in results) {
                if (r.started.getMillis() <= day.getTime() && r.ended.getMillis() > day.getTime()) {
                    cData += r.usedMemory/1048576
                }
            }
            data.push(cData)
        }
        Map result = ["days": days, "data": data]
        return result
    }

    /**
     * returns the time in queue and the processing time as sum of all jobs
     * @return map ["queue": [percentQueue, queueFormatted], "process": [percentProcess, processFormatted]]
     */
    public Map getAllStatesTimeDistribution() {

        Map data = ["queue": 0, "process": 0]

        def c = ClusterJob.createCriteria().list {
            isNotNull('started')
            isNotNull('ended')
        }

        for(job in c) {
            Duration queue = new Duration(job.queued, job.started)
            Duration process = new Duration(job.started, job.ended)
            data['queue'] += queue.getMillis()
            data['process'] += process.getMillis()
        }

        long max = data['queue'] + data['process']
        int percentQueue = Math.round(100/ max * data["queue"])
        int percentProcess = 100 - percentQueue

        String queueFormatted = new Period(data['queue']).getHours().toString() + " hours"
        String processFormatted = new Period(data['process']).getHours().toString() + " hours"

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
            like('jobClass', jobClass)
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
            le('ended', endDate)
            projections {
                distinct ('jobClass')
                order('jobClass', 'asc')
            }
        }
        return result
    }

    /**
     * returns a list of exit codes and their appearance of
     * a specific job class and sequencing type, in a specific time span
     */
    public List getJobTypeSpecificExitCodes(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def results = ClusterJob.createCriteria().list {
            like('jobClass', jobClass)
            eq('seqType', seqType)
            between('started', startDate, endDate)
            order('exitCode', 'asc')
            projections {
                groupProperty('exitCode')
                count('id', 'exitCode')
            }
        }
        return results
    }

    /**
     * returns a list of exit statuses and their appearance of
     * a specific job class and sequencing type, in a specific time span
     */
    public List getJobTypeSpecificExitStatuses(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def results = ClusterJob.createCriteria().list {
            like('jobClass', jobClass)
            eq('seqType', seqType)
            between('started', startDate, endDate)
            order('exitStatus', 'asc')
            projections {
                groupProperty('exitStatus')
                count('id', 'exitStatus')
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
        ArrayList<String> days = getDaysBetween(sDate, eDate)
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        def states = ['queued', 'started', 'ended']
        def c
        Map results = [:]
        Map data = ["queued":[], "started":[], "ended":[]]
        for (state in states) {
            c = ClusterJob.createCriteria()
            results.put(state, c.list {
                like('jobClass', jobClass)
                eq('seqType', seqType)
                between(state, startDate, endDate)
            })
        }
        for (int i = 0; i <= days.size() - 1; i++) {
            for (state in states) {
                def cData = 0
                for (s in results.getAt(state)) {
                    if (fmt.print(s."${state}").split(':')[0] == days[i].split(":")[0]) {
                        cData ++
                    }
                }
                data.get(state).push(cData)
            }
        }
        Map result = ["days": days, "data": data]
        return result
    }

    /**
     * returns requested and elapsed walltimes of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    public Map getJobTypeSpecificWalltimes(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def results = ClusterJob.createCriteria().list {
            like('jobClass', jobClass)
            eq('seqType', seqType)
            between('started', startDate, endDate)
            projections {
                property('id')
                property('requestedWalltime')
            }
        }
        def finalResults = []
        for(r in results) {
            def resultsCache = [ClusterJob.get(r[0]).elapsedWalltime.getMillis(), r[1].getMillis(), r[0]]
            finalResults.add(resultsCache)
        }

        def xAxisMax = 0
        for (result in finalResults) {
            if (result[0] > xAxisMax) { xAxisMax = result[0]}
        }

        def labels = []
        if(xAxisMax != 0) {
            labels = getLabels(xAxisMax, 10)
        }

        return ["data": finalResults, "labels": labels, "xMax": xAxisMax]
    }

    /**
     * returns requested and elapsed cores of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    public Map getJobTypeSpecificCores(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def results = ClusterJob.createCriteria().list {
            like('jobClass', jobClass)
            eq('seqType', seqType)
            between('started', startDate, endDate)
            projections {
                property('usedCores')
                property('requestedCores')
                property('id')
            }
        }
        def xAxisMax = 0
        for (result in results) {
            if (result[0] > xAxisMax) { xAxisMax = result[0]}
        }

        def labels = []
        if(xAxisMax != 0) {
            labels = getLabels(xAxisMax, 10)
        }

        return ["data": results, "labels": labels, "xMax": xAxisMax,]
    }

    /**
     * returns requested and elapsed memories of a specific job class and sequencing type,
     * the maximum values for both, to align the graphics and
     * aligned labels for the graphic
     */
    public Map getJobTypeSpecificMemories(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()
        def results = ClusterJob.createCriteria().list {
            like('jobClass', jobClass)
            eq('seqType', seqType)
            between('started', startDate, endDate)
            projections {
                property('usedMemory')
                property('requestedMemory')
                property('id')
            }
        }
        def xAxisMax = 0
        for (result in results) {
            if (result[0] > xAxisMax) { xAxisMax = result[0]}
        }

        def labels = []
        if(xAxisMax != 0) {
            labels = getLabels(xAxisMax, 10)
        }

        return ["data": results, "labels": labels, "xMax": xAxisMax]
    }

    /**
     * returns the average core usage of a specific jobclass and seq type
     * @return double
     */
    public double getJobTypeSpecificAvgCoreUsage(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()

        def result = ClusterJob.createCriteria().list {
            like('jobClass', jobClass)
            eq('seqType', seqType)
            ge('queued', startDate)
            le('ended', endDate)
        }

        double avgCpuUsage = 0

        for(r in result) {
            double cpuAvg = r.getCpuAvgUtilised()
            avgCpuUsage += cpuAvg
        }

        avgCpuUsage = avgCpuUsage/result.size()

        return avgCpuUsage.round(2)
    }

    /**
     * return the average memory usage of a specific jobclass and seqtype
     * @return int
     */
    public int getJobTypeSpecificAvgMemory(String jobClass, SeqType seqType, LocalDate sDate, LocalDate eDate) {
        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()

        def result = ClusterJob.createCriteria().list {
            like('jobClass', jobClass)
            eq('seqType', seqType)
            ge('queued', startDate)
            le('ended', endDate)
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

        DateTime startDate = sDate.toDateTimeAtStartOfDay()
        DateTime endDate = eDate.plusDays(1).toDateTimeAtStartOfDay()

        def result = ClusterJob.createCriteria().list {
            like('jobClass', jobClass)
            eq('seqType', seqType)
            ge('queued', startDate)
            le('ended', endDate)
        }

        def queue = 0
        def process = 0

        for(job in result) {
            queue += new Duration(job.queued, job.started).getMillis()
            process += new Duration(job.started, job.ended).getMillis()
        }

        if (queue < 0) queue = 0

        def avgQueue = PeriodFormat.getDefault().print(new Period(Math.round(queue/result.size())))
        def avgProcess = PeriodFormat.getDefault().print(new Period(Math.round(process/result.size())))

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

        if(queueMillis < 0) { queueMillis = 0 }
        if(processMillis < 0) { processMillis = 0}

        long max = queueMillis + processMillis

        int percentProcess = Math.round(100/ max * processMillis)
        int percentQueue = 100 - percentProcess

        String queueFormatted = PeriodFormat.getDefault().print(new Period(queueMillis))
        String processFormatted = PeriodFormat.getDefault().print(new Period(processMillis))

        Map result = ["queue": [percentQueue, queueFormatted], "process": [percentProcess, processFormatted]]
        return result
    }

    /**
     * returns all dates and hours between the two given dates as Strings
     * e.g startDate = 2000-01-01, endDate = 2000-01-02
     * result = [2000-01-01 00:00:00, 2000-01-01 01:00:00, 2000-01-01 02:00:00, ...]
     */
    public List getDaysBetween(LocalDate startDate, LocalDate endDate) {
        ArrayList<String> daysAr = new ArrayList<String>()
        String pattern = "yyyy-MM-dd HH:mm:ss"
        DateTime startD = startDate.toDateTimeAtStartOfDay()
        DateTime endD = endDate.toDateTimeAtStartOfDay()
        int days = Days.daysBetween(startD, endD).getDays()
        startD = startD.minusDays(1)
        for (int i = 0; i <= days; i++) {
            startD = startD.plusDays(1)
            daysAr.add(startD.toString(pattern))
            DateTime dayCache = startD
            for (k in 1..23) {
                dayCache = dayCache.plusHours(1)
                daysAr.add(dayCache.toString(pattern))
            }
        }
        return daysAr
    }

    /**
     * returns the latest Job Date as formatted String
     */
    public String getLatestJobDate() {
        def result = ClusterJob.createCriteria().list {
            order('queued', 'desc')
        }
        String pattern = "yyyy-MM-dd"
        return result[0].queued.toString(pattern)
    }

    /**
     * returns an amount of labels aligned for scatter RGraphs in dependence to a given maximum value
     * e.g. max = 8000 and quot = 10, return = [800, 1600, 2400, ... , 8000]
     */
    public ArrayList getLabels(max, quot) {
        def labels = []
        boolean isDivisible = max % quot == 0;
        for (double i = max/quot; i <= max; i = i + (max/quot)) {
            def label = []
            isDivisible ? label.push(i.intValue().toString()) : label.push(i.round(1).toString())
            isDivisible ? label.push(i.intValue()) : label.push(i.round(1))
            labels.push(label)
        }
        return labels
    }
}
