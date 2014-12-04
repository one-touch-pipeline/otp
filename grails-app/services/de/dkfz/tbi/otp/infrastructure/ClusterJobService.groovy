package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.infrastructure.ClusterJob.Status
import de.dkfz.tbi.flowcontrol.ws.api.pbs.JobInfo
import de.dkfz.tbi.flowcontrol.ws.api.response.JobInfos
import de.dkfz.tbi.flowcontrol.ws.client.ClientKeys
import de.dkfz.tbi.flowcontrol.ws.client.FlowControlClient
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import org.joda.time.DateTime
import org.joda.time.Duration

import javax.xml.ws.soap.SOAPFaultException

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class ClusterJobService {

    private static Map<Object, FlowControlClient> clientCache = [:]

    /**
     * creates a cluster job object with at this time known attributes,
     * usually "queued", "requestedWalltime", "requestedCores", "requestedMemory and
     * the attributes already needed for calling this methode
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
}
