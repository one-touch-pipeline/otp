package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.job.processing.ProcessingStep;
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster

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
    static belongsTo = [processingStep: ProcessingStep]

    // TODO check for API's own enum
    enum Status {
        RUNNING, FAILED, DONE, COMPLETED
    }
    /**
     * Used for MultiJobs. TODO: OTP-981
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
    Date queued
    /**
     * date, job started
     */
    Date started
    /**
     * date, job ended
     */
    Date ended
    /**
     * requested walltime for the job in milliseconds
     */
    Long requestedWalltime
    /**
     * elapsed walltime for the job in milliseconds
     */
    Long elapsedWalltime
    /**
     * difference of requested and elapsed walltime in milliseconds
     */
    Long walltimeDiff
    /**
     * cores used for processing the job
     */
    Integer requestedCores
    /**
     * time, all cpu's used to process the job in milliseconds
     */
    Long cpuTime
    /**
     * cpu time per core in milliseconds
     */
    Long cpuTimePerCore
    /**
     * average cpu cores utilized
     */
    Double cpuAvgUtilised
    /**
     * requested memory in KiB
     */
    Long requestedMemory
    /**
     * actually used memory to process the job in KiB
     */
    Long usedMemory
    /**
     * describes how efficient the memory was used
     * {@link #usedMemory} divided by {@link #requestedMemory}
     */
    Double memoryEfficiency

    static constraints = {
        validated(nullable:false)
        realm(nullable: false)
        clusterJobId(blank: false, nullable: false)
        clusterJobName(blank: false, nullable: false)
        exitStatus(nullable: true)                      // gets filled after initialization, must be nullable
        exitCode(nullable: true)                        // gets filled after initialization, must be nullable
        queued(nullable: false)
        started(nullable: true)                         // gets filled after initialization, must be nullable
        ended(nullable: true)                           // gets filled after initialization, must be nullable
        requestedWalltime(nullable: false)
        elapsedWalltime(nullable: true)                 // gets filled after initialization, must be nullable
        walltimeDiff(nullable: true)                    // gets filled after initialization, must be nullable
        requestedCores(nullable: false)
        cpuTime(nullable: true)                         // gets filled after initialization, must be nullable
        cpuTimePerCore(nullable: true)
        cpuAvgUtilised(nullable: true)                  // gets filled after initialization, must be nullable
        requestedMemory(nullable: false)
        usedMemory(nullable: true)                      // gets filled after initialization, must be nullable
        memoryEfficiency(nullable: true)                // gets filled after initialization, must be nullable
    }

    private static final String elapsedWalltimeFormula = "(CASE WHEN ended IS NOT NULL THEN DATEDIFF('MILLISECOND', started, ended) ELSE DATEDIFF('MILLISECOND', started, CURRENT_DATE()) END)"

    static mapping = {
        memoryEfficiency formula: '(used_memory * 1.0/ requested_memory)'
        cpuTimePerCore formula: '(cpu_time / requested_cores)'
        cpuAvgUtilised formula: "(cpu_time * 1.0 / DATEDIFF('MILLISECOND', started, ended))"
        elapsedWalltime formula: elapsedWalltimeFormula
        walltimeDiff formula: "(requested_walltime - " + elapsedWalltimeFormula + ")"
    }

    public Cluster getCluster() {
        return getRealm().cluster
    }
}

