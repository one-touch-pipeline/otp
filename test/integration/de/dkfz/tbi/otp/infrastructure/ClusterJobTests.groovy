package de.dkfz.tbi.otp.infrastructure

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import org.joda.time.DateTime
import org.joda.time.Duration

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

        realm = DomainFactory.createRealmDataManagementBioQuant()

        assertNotNull(realm.save(flush: true))
    }

    @Test
    public void testGetter () {

        ClusterJob clusterJob = new ClusterJob(
                                                    processingStep: step,
                                                    realm: realm,
                                                    clusterJobId: "testID",
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

}
