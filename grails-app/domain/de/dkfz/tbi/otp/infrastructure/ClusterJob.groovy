package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import org.jadira.usertype.dateandtime.joda.*
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * A ClusterJob represents a single submitted or finished job on the cluster.
 *
 * ClusterJob objects get created after sending job requests to the cluster.
 * Information that are known at this point like "clusterJobName" or "queued" get
 * instantly filled.
 * Other information can be accessed through the flowcontrol-API and get filled
 * in the PbsMonitorService as soon as the cluster job is finished.
 **/
class ClusterJob implements ClusterJobIdentifier{

    public static final String JOB_INFO_NOT_SET_MESSAGE = "Job info is not set (yet)."

    static belongsTo = [processingStep: ProcessingStep]

    enum Status {
        FAILED, COMPLETED
    }
    /**
     * Used for {@link AbstractMultiJob}s. Is set to true after {@link AbstractMultiJob#execute(Collection)} returns.
     */
    boolean validated = false
    /**
     * Reference to the used realm
     */
    Realm realm
    /**
     * Id of the cluster job
     */
    String clusterJobId
    /**
     * name of the cluster job
     */
    String clusterJobName
    /**
     * class of the {@link Job} that submitted this cluster job
     */
    String jobClass
    /**
     * sequence type used for this cluster job
     */
    SeqType seqType
    /**
     * exit status of job
     */
    Status exitStatus
    /**
     * exit code, containing information about the ended job
     */
    Integer exitCode
    /**
     * date, job was submitted
     */
    DateTime queued
    /**
     * date, job started
     */
    DateTime started
    /**
     * date, job ended
     */
    DateTime ended
    /**
     * requested walltime for the job
     */
    Duration requestedWalltime
    /**
     * cores requested for processing the job
     */
    Integer requestedCores
    /**
     * cores used for processing the job
     */
    Integer usedCores
    /**
     * time, all cpu's used to process the job
     */
    Duration cpuTime
    /**
     * requested memory in KiB
     */
    Long requestedMemory
    /**
     * actually used memory to process the job in KiB
     */
    Long usedMemory

    static constraints = {
        validated(nullable:false)
        realm(nullable: false)
        clusterJobId(blank: false, nullable: false)
        clusterJobName(blank: false, nullable: false, validator: { clusterJobName, clusterJob -> clusterJobName.endsWith("_${clusterJob.jobClass}") } )
        jobClass(blank: false, nullable: false)
        seqType(nullable: true)                         // gets filled after initialization, must be nullable
        exitStatus(nullable: true)                      // gets filled after initialization, must be nullable
        exitCode(nullable: true)                        // gets filled after initialization, must be nullable
        queued(nullable: false)
        started(nullable: true)                         // gets filled after initialization, must be nullable
        ended(nullable: true)                           // gets filled after initialization, must be nullable
        requestedWalltime(nullable: false, min: new Duration(1))
        requestedCores(nullable: false, min: 1)
        usedCores(nullable:true)                        // gets filled after initialization, must be nullable
        cpuTime(nullable: true)                         // gets filled after initialization, must be nullable
        requestedMemory(nullable: false, min: 1L)
        usedMemory(nullable: true)                      // gets filled after initialization, must be nullable
    }

    static mapping = {
        queued type: PersistentDateTime
        started type: PersistentDateTime
        ended type: PersistentDateTime
        requestedWalltime type: PersistentDurationAsString
        cpuTime type: PersistentDurationAsString
    }

    public Cluster getCluster() {
        return getRealm().cluster
    }

    /**
     * describes how efficient the memory was used
     * {@link #usedMemory} divided by {@link #requestedMemory}
     */
    public double getMemoryEfficiency () {
        if (usedMemory != null && requestedMemory != null) {
             return usedMemory * 1.0 / requestedMemory
        } else {
            throw new IllegalStateException(JOB_INFO_NOT_SET_MESSAGE)
        }
    }

    /**
     * cpu time per core
     */
    public double getCpuTimePerCore () {
        if (cpuTime != null && usedCores != null) {
            return cpuTime.millis / usedCores
        } else {
            throw new IllegalStateException(JOB_INFO_NOT_SET_MESSAGE)
        }
    }

    /**
     * average cpu cores utilized
     */
    public double getCpuAvgUtilised () {
        if (cpuTime != null) {
            (cpuTime.millis * 1.0) / getElapsedWalltime().millis
        } else {
            throw new IllegalStateException(JOB_INFO_NOT_SET_MESSAGE)
        }
    }

    /**
     * elapsed walltime for the job
     */
    public Duration getElapsedWalltime() {
        if (ended != null && started != null) {
            return new Duration(started, ended)
        } else {
            throw new IllegalStateException(JOB_INFO_NOT_SET_MESSAGE)
        }
    }

    /**
     * difference of requested and elapsed walltime
     */
    public Duration getWalltimeDiff () {
        if (requestedWalltime != null) {
            return requestedWalltime.minus(elapsedWalltime)
        } else {
            throw new IllegalStateException(JOB_INFO_NOT_SET_MESSAGE)
        }
    }

    @Override
    public String toString() {
        return "Cluster job ${clusterJobId} on realm ${realm}"
    }
}

