package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.*
import org.junit.*

import static org.junit.Assert.*

class ClusterJobTests {

    public static final int REQUESTED_MEMORY = 1000
    public static final int USED_MEMORY = 800
    public static final int CPU_TIME = 12 * 60 * 60 * 1000
    public static final int REQUESTED_CORES = 10
    public static final int USED_CORES = 10
    public static final int REQUESTED_WALLTIME = 24 * 60 * 60 * 1000
    public static final int ELAPSED_WALLTIME = 24 * 60 * 60 * 1000
    public static final DateTime QUEUED = new DateTime(1993, 5, 15, 12, 0, 0)
    public static final DateTime STARTED = QUEUED.plusDays(1)
    public static final DateTime ENDED = STARTED.plusDays(1)

    ProcessingStep step
    Realm realm

    @Before
    void before() {

        JobExecutionPlan plan = new JobExecutionPlan(name: "testFormula", obsoleted: true, planVersion: 0)

        assertNotNull(plan.save(flush: true))

        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")

        assertNotNull(process.save(flush: true))

        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "foo", plan: plan)

        assertNotNull(jobDefinition.save(flush: true))

        step = new ProcessingStep(id: 1, process: process, jobDefinition: jobDefinition, jobClass: 'de.dkfz.tbi.otp.test.job.jobs.NonExistentDummyJob')

        assertNotNull(step.save(flush: true))

        realm = DomainFactory.createRealmDataManagement()

        assertNotNull(realm.save(flush: true))

        ProcessingOption option = new ProcessingOption([name: OptionName.STATISTICS_BASES_PER_BYTES_FASTQ, type: null, project: null, value: "1.0", comment: "some comment"])

        assertNotNull(option.save(flush: true))
    }

    @Test
    public void testGetter () {

        ClusterJob clusterJob = new ClusterJob(
                                                    processingStep: step,
                                                    realm: realm,
                                                    clusterJobId: "testID",
                                                    userName: realm.unixUser,
                                                    clusterJobName: "testName_${step.nonQualifiedJobClass}",
                                                    jobClass: step.nonQualifiedJobClass,
                                                    queued: QUEUED,
                                                    started: STARTED,
                                                    ended: ENDED,
                                                    requestedWalltime: new Duration(REQUESTED_WALLTIME),
                                                    requestedCores: REQUESTED_CORES,
                                                    usedCores: USED_CORES,
                                                    cpuTime: new Duration(CPU_TIME),
                                                    requestedMemory: REQUESTED_MEMORY,
                                                    usedMemory: USED_MEMORY
                                              )

        assertNotNull(clusterJob.save(flush: true))

        clusterJob.refresh()

        assertEquals(clusterJob.memoryEfficiency, (USED_MEMORY / REQUESTED_MEMORY), 0d)
        assertEquals(clusterJob.cpuTimePerCore, ((CPU_TIME) / USED_CORES), 0d)
        assertEquals(clusterJob.cpuAvgUtilised, ((CPU_TIME) / ELAPSED_WALLTIME), 0d)
        assertEquals(clusterJob.elapsedWalltime.millis, (ELAPSED_WALLTIME), 0)
        assertEquals(clusterJob.walltimeDiff.millis, (REQUESTED_WALLTIME - ELAPSED_WALLTIME), 0)
    }

    @Test
    public void testNullable () {

        ClusterJob clusterJob = new ClusterJob(
                                                    processingStep: null,
                                                    realm: null,
                                                    clusterJobId: null,
                                                    userName: realm.unixUser,
                                                    clusterJobName: null,
                                                    jobClass: null,
                                                    queued: null,
                                                    started: null,
                                                    ended: null,
                                                    requestedWalltime: null,
                                                    requestedCores: null,
                                                    requestedMemory: null,
                                              )

        assertFalse(clusterJob.validate())

        ClusterJob clusterJob2 = new ClusterJob(
                                                    processingStep: step,
                                                    realm: realm,
                                                    clusterJobId: "testID",
                                                    userName: realm.unixUser,
                                                    clusterJobName: "testName_${step.nonQualifiedJobClass}",
                                                    jobClass: step.nonQualifiedJobClass,
                                                    seqType: null,
                                                    exitStatus: null,
                                                    exitCode: null,
                                                    queued: QUEUED,
                                                    started: null,
                                                    ended: null,
                                                    requestedWalltime: new Duration (REQUESTED_WALLTIME),
                                                    requestedCores: 10,
                                                    usedCores: null,
                                                    cpuTime: new Duration(CPU_TIME),
                                                    requestedMemory: 1000,
                                                    usedMemory: null,
                                              )

        assertTrue(clusterJob2.validate())
    }

    @Test
    public void testBeforeValidate_WhenNBasesIsNullAndFileSizeGiven_ShouldFillNBases() {
        Long fileSize = 100L
        Float basesPerBytesFastQFactor = (ProcessingOptionService.findOptionObject(ProcessingOption.OptionName.STATISTICS_BASES_PER_BYTES_FASTQ, null, null).value as float)
        ClusterJob clusterJob = new ClusterJob(
                processingStep: step,
                realm: realm,
                clusterJobId: "testID",
                userName: realm.unixUser,
                clusterJobName: "testName_${step.nonQualifiedJobClass}",
                jobClass: step.nonQualifiedJobClass,
                queued: QUEUED,
                fileSize: fileSize,
                nBases: null
        )
        assert clusterJob.save(flush: true, failOnError: true)
        assert clusterJob.nBases == (fileSize * basesPerBytesFastQFactor) as Long
        assert clusterJob.basesPerBytesFastq == basesPerBytesFastQFactor
    }

    @Test
    public void testBeforeValidate_WhenNBasesIsNotNullAndFileSizeGiven_ShouldNotFillBases() {
        Long fileSize = 1000L
        Long nBases = 100L
        ClusterJob clusterJob = new ClusterJob(
                processingStep: step,
                realm: realm,
                clusterJobId: "testID",
                userName: realm.unixUser,
                clusterJobName: "testName_${step.nonQualifiedJobClass}",
                jobClass: step.nonQualifiedJobClass,
                queued: QUEUED,
                fileSize: fileSize,
                nBases: nBases
        )
        assert clusterJob.save(flush: true, failOnError: true)
        assert clusterJob.nBases == nBases
        assertNull(clusterJob.basesPerBytesFastq)
    }

    @Test
    public void testBeforeValidate_WhenClusterJobExistsAndNBasesIsNullAndFileSizeGiven_ShouldFillNBases() {
        Long fileSize = 100L
        Float basesPerBytesFastQFactor = (ProcessingOptionService.findOptionObject(OptionName.STATISTICS_BASES_PER_BYTES_FASTQ, null, null).value as float)
        ClusterJob clusterJob = new ClusterJob(
                processingStep: step,
                realm: realm,
                clusterJobId: "testID",
                userName: realm.unixUser,
                clusterJobName: "testName_${step.nonQualifiedJobClass}",
                jobClass: step.nonQualifiedJobClass,
                queued: QUEUED,
                fileSize: null,
                nBases: null
        )
        assert clusterJob.save(flush: true, failOnError: true)
        clusterJob.fileSize = fileSize
        assert clusterJob.save(flush: true, failOnError: true)
        assert clusterJob.nBases == (fileSize * basesPerBytesFastQFactor) as Long
        assert clusterJob.basesPerBytesFastq == basesPerBytesFastQFactor
    }
}
