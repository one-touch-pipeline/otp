package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.gorm.mapper.PersistentDateTimeAsMillis
import de.dkfz.tbi.otp.gorm.mapper.PersistentDurationAsMillis
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import de.dkfz.tbi.otp.ngsdata.SeqType
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

/**
 * A ClusterJob represents a single submitted or finished job on the cluster.
 *
 * ClusterJob objects get created after sending job requests to the cluster.
 * Information that are known at this point like "clusterJobName" or "queued" get
 * instantly filled.
 * Other information can be accessed through the flowcontrol-API and get filled
 * in the PbsMonitorService as soon as the cluster job is finished.
 *
 * all timestamps using joda-time, e.g. DateTime queued, get saved as UTC-timezone
 **/
class ClusterJob {

    public static final String JOB_INFO_NOT_SET_MESSAGE = "Job info is not set (yet)."

    ProcessingStep processingStep
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
     * user executing the job
     */
    String userName
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
     * number of bases of all {@link de.dkfz.tbi.otp.ngsdata.SeqTrack} that belong to this job
     */
    Long nBases
    /**
     * number of reads of all {@link de.dkfz.tbi.otp.ngsdata.SeqTrack} that belong to this job
     */
    Long nReads
    /**
     * file size of all {@link de.dkfz.tbi.otp.ngsdata.DataFile} that belong to this job
     */
    Long fileSize
    /**
     * the bases per bytes factor that is used to calculate the file size in bases,
     * in case bases are not known at this time (all jobs before FastQC-WF)
     * the factor can change from time to time and is currently calculated by hand
     * the latest value, as well as obsolate values are stored
     * in the {@link de.dkfz.tbi.otp.dataprocessing.ProcessingOption} with name basesPerBytesFastQ
     * this value represents the factor that was used when this job was completed
     */
    Float basesPerBytesFastq
    /**
     * multiplexing was used for processing
     * WARNING: This field has been filled with incorrect values (OTP-1973)
     */
    Boolean multiplexing
    /**
     * processed with HiSeq X Ten machine
     */
    Boolean xten
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
     *
     * currently default null, because the API gives no exact information about used cores
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
        userName blank: false
        clusterJobName(blank: false, nullable: false, validator: { clusterJobName, clusterJob -> clusterJobName.endsWith("_${clusterJob.jobClass}") } )
        jobClass(blank: false, nullable: false)
        seqType(nullable: true)                                 // gets filled after initialization, must be nullable
        nBases(nullable: true)
        nReads(nullable: true)
        fileSize(nullable: true)
        basesPerBytesFastq(nullable: true)
        multiplexing(nullable: true)
        xten(nullable: true)
        exitStatus(nullable: true)                              // gets filled after initialization, must be nullable
        exitCode(nullable: true)                                // gets filled after initialization, must be nullable
        queued(nullable: false)
        started(nullable: true)                                 // gets filled after initialization, must be nullable
        ended(nullable: true)                                   // gets filled after initialization, must be nullable
        requestedWalltime(nullable: true, min: new Duration(1)) // gets filled after initialization, must be nullable
        requestedCores(nullable: true, min: 1)                  // gets filled after initialization, must be nullable
        usedCores(nullable:true)                                // gets filled after initialization, must be nullable
        cpuTime(nullable: true)                                 // gets filled after initialization, must be nullable
        requestedMemory(nullable: true, min: 1L)                // gets filled after initialization, must be nullable
        usedMemory(nullable: true)                              // gets filled after initialization, must be nullable
    }

    static mapping = {
        processingStep index: "cluster_job_processing_step_idx"
        queued type: PersistentDateTimeAsMillis
        started type: PersistentDateTimeAsMillis
        ended type: PersistentDateTimeAsMillis
        requestedWalltime type: PersistentDurationAsMillis
        cpuTime type: PersistentDurationAsMillis

        clusterJobId index: "cluster_job_cluster_job_id_idx"
        clusterJobName index: "cluster_job_cluster_job_name_idx"
    }

    /*
     * Due missing number of bases (SeqTrack.nBasePairs) before FastQC-WF, we have to calculate this value from the file size
     * of the specific input files. The calculation is done with the approximation for bases per bytes stored in the
     * ProcessingOption with name "basesPerBytesFastQ". This value should be kept in ProcessingOption to have an
     * easy way for updating. The "bases per bytes"-value is currently calculated by hand. See OTP-1754 for more information.
     *
     * The calculation should be done automatically when nBases are not given and the results should be stored
     * immediately in the database, because of the use of sql queries in ClusterJobService.
     */
    def beforeValidate() {
        if (fileSize && !nBases) {
            basesPerBytesFastq = ProcessingOptionService.findOptionObject('basesPerBytesFastQ', null, null).value as float
            nBases = fileSize * basesPerBytesFastq
        }
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
    public Duration getElapsedWalltime () {
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

    public String getRequestedWalltimeAsISO () {
        return formatPeriodAsISOString(requestedWalltime.getMillis())
    }

    public String getElapsedWalltimeAsISO () {
        return formatPeriodAsISOString(elapsedWalltime.getMillis())
    }

    public String getWalltimeDiffAsISO () {
        return formatPeriodAsISOString(walltimeDiff.getMillis())
    }

    public String getCpuTimeAsISO () {
        return formatPeriodAsISOString(cpuTime.getMillis())
    }

    public String getCpuTimePerCoreAsISO () {
        return formatPeriodAsISOString(new Duration(Math.round(cpuTimePerCore)))
    }

    private String formatPeriodAsISOString (value) {
        return PeriodFormat.getDefault().print(new Period(value))
    }

    public static ClusterJob findByClusterJobIdentifier(ClusterJobIdentifier identifier) {
        return exactlyOneElement(findAllWhere(
                realm: identifier.realm,
                clusterJobId: identifier.clusterJobId,
        ))
    }

    @Override
    public String toString() {
        return "Cluster job ${clusterJobId} on realm ${realm}"
    }
}

