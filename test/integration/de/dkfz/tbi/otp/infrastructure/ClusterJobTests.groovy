package de.dkfz.tbi.otp.infrastructure

import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

import static org.junit.Assert.*

class ClusterJobTests {

    static final int REQUESTED_MEMORY = 1000
    static final int USED_MEMORY = 800
    static final int CPU_TIME = 12 * 60 * 60 * 1000
    static final int REQUESTED_CORES = 10
    static final int USED_CORES = 10
    static final int REQUESTED_WALLTIME = 24 * 60 * 60 * 1000
    static final int ELAPSED_WALLTIME = 24 * 60 * 60 * 1000
    static final DateTime QUEUED = new DateTime(1993, 5, 15, 12, 0, 0)
    static final DateTime STARTED = QUEUED.plusDays(1)
    static final DateTime ENDED = STARTED.plusDays(1)

    TestConfigService configService
    ProcessingOptionService processingOptionService
    ProcessingStep step
    Realm realm

    @Before
    void before() {
        configService = new TestConfigService()

        JobExecutionPlan plan = new JobExecutionPlan(name: "testFormula", obsoleted: true, planVersion: 0)

        assertNotNull(plan.save(flush: true))

        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo")

        assertNotNull(process.save(flush: true))

        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "foo", plan: plan)

        assertNotNull(jobDefinition.save(flush: true))

        step = new ProcessingStep(id: 1, process: process, jobDefinition: jobDefinition, jobClass: 'de.dkfz.tbi.otp.test.job.jobs.NonExistentDummyJob')

        assertNotNull(step.save(flush: true))

        realm = DomainFactory.createRealm()

        assertNotNull(realm.save(flush: true))

        ProcessingOption option = new ProcessingOption([name: OptionName.STATISTICS_BASES_PER_BYTES_FASTQ, type: null, project: null, value: "1.0", comment: "some comment"])

        assertNotNull(option.save(flush: true))
    }

    @After
    void after() {
        configService.clean()
    }

    @Test
    void testGetter () {

        ClusterJob clusterJob = new ClusterJob(
                                                    processingStep: step,
                                                    realm: realm,
                                                    clusterJobId: "testID",
                                                    userName: configService.getSshUser(),
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
    void testNullable () {

        ClusterJob clusterJob = new ClusterJob(
                                                    processingStep: null,
                                                    realm: null,
                                                    clusterJobId: null,
                                                    userName: configService.getSshUser(),
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
                                                    userName: configService.getSshUser(),
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
    void testBeforeValidate_WhenNBasesIsNullAndFileSizeGiven_ShouldFillNBases() {
        Long fileSize = 100L
        double basesPerBytesFastQFactor = processingOptionService.findOptionAsDouble(OptionName.STATISTICS_BASES_PER_BYTES_FASTQ)
        ClusterJob clusterJob = new ClusterJob(
                processingStep: step,
                realm: realm,
                clusterJobId: "testID",
                userName: configService.getSshUser(),
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
    void testBeforeValidate_WhenNBasesIsNotNullAndFileSizeGiven_ShouldNotFillBases() {
        Long fileSize = 1000L
        Long nBases = 100L
        ClusterJob clusterJob = new ClusterJob(
                processingStep: step,
                realm: realm,
                clusterJobId: "testID",
                userName: configService.getSshUser(),
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
    void testBeforeValidate_WhenClusterJobExistsAndNBasesIsNullAndFileSizeGiven_ShouldFillNBases() {
        Long fileSize = 100L
        double basesPerBytesFastQFactor = processingOptionService.findOptionAsDouble(OptionName.STATISTICS_BASES_PER_BYTES_FASTQ)
        ClusterJob clusterJob = new ClusterJob(
                processingStep: step,
                realm: realm,
                clusterJobId: "testID",
                userName: configService.getSshUser(),
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
