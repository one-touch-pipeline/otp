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

import org.joda.time.*
import org.joda.time.format.*

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.gorm.mapper.PersistentDateTimeAsMillis
import de.dkfz.tbi.otp.gorm.mapper.PersistentDurationAsMillis
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * A ClusterJob represents a single submitted or finished job on the cluster.
 *
 * ClusterJob objects get created after sending job requests to the cluster.
 * Information that are known at this point like "clusterJobName" or "queued" get
 * instantly filled.
 * Other information are stored as soon as the cluster job is finished. Depending on
 * the used cluster job scheduler, not all values may be be available.
 *
 * all timestamps using joda-time, e.g. DateTime queued, get saved as UTC-timezone
 */
class ClusterJob implements Entity {

    static final String NOT_AVAILABLE = "N/A"

    static final int DIGIT_COUNT = 2

    static final PeriodFormatter HH_MM_SS = new PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(DIGIT_COUNT)
            .appendHours()
            .appendLiteral(":")
            .printZeroAlways()
            .minimumPrintedDigits(DIGIT_COUNT)
            .appendMinutes()
            .appendLiteral(":")
            .printZeroAlways()
            .minimumPrintedDigits(DIGIT_COUNT)
            .appendSeconds()
            .toFormatter()

    /**
     * @deprecated old workflow system
     */
    @Deprecated
    ProcessingStep processingStep

    enum Status {
        FAILED, COMPLETED
    }

    /**
     * The different states for checking.
     */
    enum CheckStatus {
        /**
         * The job was created, but not started to check.
         * This is necessary, because jobs are created one by one, but should first start to check if all are created.
         */
        CREATED,
        /**
         * The job should currently be checked
         */
        CHECKING,
        /**
         * The job has finished on the cluster.
         */
        FINISHED,
    }

    /**
     * Used for {@link AbstractMultiJob}s. Is set to true after {@link AbstractMultiJob#execute(Collection)} returns.
     */
    boolean validated = false

    /**
     * Helper flag for the transition between the old and new workflow system.
     *
     * We need to differentiate between jobs belonging to the old or new system.
     */
    boolean oldSystem = true

    /**
     * The state of checking
     *
     * @see CheckStatus
     */
    CheckStatus checkStatus = CheckStatus.CREATED

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
     * Location of the job log on the file system (absolute path)
     */
    String jobLog
    /**
     * class of the {@link Job} that submitted this cluster job
     */
    String jobClass
    /**
     * sequence type used for this cluster job
     */
    SeqType seqType
    /**
     * number of bases of all {@link SeqTrack} that belong to this job
     */
    Long nBases
    /**
     * number of reads of all {@link SeqTrack} that belong to this job
     */
    Long nReads
    /**
     * file size of all {@link DataFile} that belong to this job
     */
    Long fileSize
    /**
     * the bases per bytes factor that is used to calculate the file size in bases,
     * in case bases are not known at this time (all jobs before FastQC-WF)
     * the factor can change from time to time and is currently calculated by hand
     * the latest value, as well as obsolete values are stored
     * in the {@link ProcessingOption} with name {@link ProcessingOption.OptionName#STATISTICS_BASES_PER_BYTES_FASTQ}
     * this value represents the factor that was used when this job was completed
     */
    Float basesPerBytesFastq
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
    /** date, job became eligible to run */
    DateTime eligible
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
    /**
     * swap space used
     */
    Integer usedSwap
    /**
     * cluster node the job was executed on
     */
    String node
    /**
     * the account that was requested when submitting the job (not the user account of the user submitting the job)
     */
    String accountName
    /**
     * duration the job was in suspended state, caused by the system
     */
    Duration systemSuspendStateDuration
    /**
     * duration the job was in suspended state, caused by the user
     */
    Duration userSuspendStateDuration
    /**
     * how often the job was started
     */
    Integer startCount

    Set<ClusterJob> dependencies = [] as Set<ClusterJob>

    Individual individual

    WorkflowStep workflowStep

    static hasMany = [
            dependencies: ClusterJob,
    ]

    static belongsTo = [
            processingStep: ProcessingStep,
    ]

    static constraints = {
        validated(nullable: false)
        realm(nullable: false)
        clusterJobId(blank: false, nullable: false)
        userName blank: false
        clusterJobName(blank: false, nullable: false)
        jobClass(blank: false, nullable: false)
        seqType(nullable: true)
        nBases(nullable: true)
        nReads(nullable: true)
        fileSize(nullable: true)
        basesPerBytesFastq(nullable: true)
        xten(nullable: true)
        queued(nullable: false)
        // can't use OtpPath.isValidAbsolutePath(it) here, because path may contain ":"
        jobLog nullable: true, validator: { !it || Paths.get(it).absolute }
        // the following values must be nullable because they get filled after the job is finished
        // and may not be available from every cluster job scheduler
        exitStatus(nullable: true)
        exitCode(nullable: true)
        eligible nullable: true
        started(nullable: true)
        ended(nullable: true)
        requestedWalltime(nullable: true, min: new Duration(1))
        requestedCores(nullable: true, min: 1)
        usedCores(nullable: true, min: 1)
        cpuTime(nullable: true)
        requestedMemory(nullable: true, min: 1L)
        usedMemory(nullable: true)
        node nullable: true, blank: false
        usedSwap nullable: true
        accountName nullable: true, blank: false
        systemSuspendStateDuration nullable: true
        userSuspendStateDuration nullable: true
        startCount nullable: true, min: 1
        individual(nullable: true)

        oldSystem validator: { val, obj ->
            if (val ^ !obj.workflowStep) {
                return false
            }
            if (!val ^ !obj.processingStep) {
                return false
            }
        }
        workflowStep nullable: true
        processingStep nullable: true
    }

    static mapping = {
        processingStep index: "cluster_job_processing_step_idx"
        queued type: PersistentDateTimeAsMillis
        eligible type: PersistentDateTimeAsMillis
        started type: PersistentDateTimeAsMillis
        ended type: PersistentDateTimeAsMillis
        requestedWalltime type: PersistentDurationAsMillis
        cpuTime type: PersistentDurationAsMillis
        systemSuspendStateDuration type: PersistentDurationAsMillis
        userSuspendStateDuration type: PersistentDurationAsMillis
        jobLog type: 'text'

        clusterJobId index: "cluster_job_cluster_job_id_idx"
        clusterJobName index: "cluster_job_cluster_job_name_idx"
        workflowStep index: "cluster_job_workflow_step_idx"
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
            basesPerBytesFastq = ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.STATISTICS_BASES_PER_BYTES_FASTQ, null, null) as float
            nBases = fileSize * basesPerBytesFastq
        }
    }

    /**
     * describes how efficient the memory was used
     * {@link #usedMemory} divided by {@link #requestedMemory}
     */
    Double getMemoryEfficiency() {
        if (usedMemory != null && requestedMemory != null) {
            return ((double) usedMemory) / requestedMemory
        }
        return null
    }

    /**
     * cpu time per core
     */
    Double getCpuTimePerCore() {
        if (cpuTime != null && usedCores != null) {
            return cpuTime.millis / usedCores
        }
        return null
    }

    /**
     * average cpu cores utilized
     */
    Double getCpuAvgUtilised() {
        if (cpuTime != null && elapsedWalltime != null && elapsedWalltime.millis != 0) {
            return ((double) cpuTime.millis) / elapsedWalltime.millis
        }
        return null
    }

    /**
     * elapsed walltime for the job
     */
    Duration getElapsedWalltime() {
        if (ended != null && started != null) {
            return new Duration(started, ended)
        }
        return null
    }

    /**
     * difference of requested and elapsed walltime
     */
    Duration getWalltimeDiff() {
        if (requestedWalltime != null && elapsedWalltime != null) {
            return requestedWalltime - elapsedWalltime
        }
        return null
    }

    String getRequestedWalltimeAsISO() {
        return formatPeriodAsISOString(requestedWalltime)
    }

    String getElapsedWalltimeAsISO() {
        return formatPeriodAsISOString(elapsedWalltime)
    }

    String getElapsedWalltimeAsHhMmSs() {
        return formatPeriodAsHhMmSs(elapsedWalltime)
    }

    String getWalltimeDiffAsISO() {
        return formatPeriodAsISOString(walltimeDiff)
    }

    String getCpuTimeAsISO() {
        return formatPeriodAsISOString(cpuTime)
    }

    private String formatPeriodAsISOString(Duration value) {
        return value ? PeriodFormat.default.print(new Period(value.millis)) : NOT_AVAILABLE
    }

    private String formatPeriodAsHhMmSs(Duration value) {
        return value ? HH_MM_SS.print(new Period(value.millis)) : NOT_AVAILABLE
    }

    static ClusterJob findByClusterJobIdentifier(ClusterJobIdentifier identifier, ProcessingStep processingStep) {
        return exactlyOneElement(findAllWhere(
                realm: identifier.realm,
                clusterJobId: identifier.clusterJobId,
                processingStep: processingStep,
        ))
    }

    @Override
    String toString() {
        return "Cluster job ${clusterJobId} on realm ${realm}"
    }
}

