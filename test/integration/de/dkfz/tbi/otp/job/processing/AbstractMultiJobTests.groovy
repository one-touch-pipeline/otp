package de.dkfz.tbi.otp.job.processing

import org.apache.commons.logging.impl.NoOpLog

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.scheduler.PbsJobInfo
import de.dkfz.tbi.otp.job.scheduler.PbsMonitorService
import de.dkfz.tbi.otp.job.scheduler.Scheduler
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import org.junit.AfterClass
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import static de.dkfz.tbi.TestConstants.ARBITRARY_MESSAGE
import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.assertFailed
import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.assertSucceeded
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.*
import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertTrue

class AbstractMultiJobTests {

    static final String CLUSTER_JOB_1_ID = '123'
    static final String CLUSTER_JOB_2_ID = '456'
    static final String CLUSTER_JOB_3_ID = '789'

    @Autowired
    ApplicationContext applicationContext
    PbsMonitorService pbsMonitorService
    Scheduler scheduler

    final ProcessingStep step = createAndSaveProcessingStep()
    final Realm realm1 = DomainFactory.createRealmDataProcessing()
    final Realm realm2 = DomainFactory.createRealmDataProcessing()
    final PbsJobInfo clusterJob1 = new PbsJobInfo(realm: realm1, pbsId: CLUSTER_JOB_1_ID)
    final PbsJobInfo clusterJob2 = new PbsJobInfo(realm: realm2, pbsId: CLUSTER_JOB_2_ID)
    final PbsJobInfo clusterJob3 = new PbsJobInfo(realm: realm1, pbsId: CLUSTER_JOB_3_ID)
    final Collection<PbsJobInfo> clusterJobs1 = [clusterJob1, clusterJob2]
    final Collection<PbsJobInfo> clusterJobs2 = [clusterJob3]

    final Semaphore semaphore = new Semaphore(0)
    final AtomicInteger atomicPhase = new AtomicInteger(0)
    final AtomicBoolean suspendCancelled = new AtomicBoolean(false)

    AbstractMultiJob job
    Collection<PbsJobInfo> monitoredJobs

    @Before
    void before() {
        assert realm1.save(flush: true)
        assert realm2.save(flush: true)
        pbsMonitorService.queuedJobs = [:]
    }

    @AfterClass
    static void afterClass() {
        // Without cleaning the database, tests in other test classes fail.
        JobDefinition.withTransaction {
            ClusterJob          .list(sort: "id", order: "desc").each { it.delete(flush: true) }
            Realm               .list(sort: "id", order: "desc").each { it.delete(flush: true) }
            ProcessingError     .list(sort: "id", order: "desc").each { it.delete(flush: true) }
            ProcessingStepUpdate.list(sort: "id", order: "desc").each { it.delete(flush: true) }
            ProcessingStep      .list(sort: "id", order: "desc").each { it.delete(flush: true) }
            Process             .list(sort: "id", order: "desc").each { it.delete(flush: true) }
            JobExecutionPlan    .list().each { it.firstJob = null; it.save(flush: true) }
            JobDefinition       .list(sort: "id", order: "desc").each { it.delete(flush: true) }
            JobExecutionPlan    .list(sort: "id", order: "desc").each { it.delete(flush: true) }
        }
    }

    @Test
    void testSucceedingJobWithoutResuming() {
        succeedingJob(false)
    }

    @Test
    void testSucceedingJobWithResuming() {
        succeedingJob(true)
    }

    private AbstractMultiJob createJob(final Closure mainLogic) {
        final AbstractMultiJob jobBean = applicationContext.getBean("testMultiJob", step, null)
        jobBean.log = new NoOpLog()
        jobBean.metaClass.executeImpl = { final Collection<? extends ClusterJobIdentifier> finishedClusterJobs ->
            try {
                assert delegate.is(job)
                final int phase = atomicPhase.incrementAndGet()
                mainLogic(phase, finishedClusterJobs)
            } finally {
                semaphore.release()
            }
        }
        return jobBean
    }

    void succeedingJob(final boolean withResuming) {
        final Closure mainLogic = { final int phase, final Collection<? extends ClusterJobIdentifier> finishedClusterJobs ->
            switch (phase) {
                case 1:
                    assert finishedClusterJobs == null
                    assert !job.resumable
                    clusterJobs1.each {
                        assert createClusterJob(step, it).save(flush: true)
                    }
                    return NextAction.WAIT_FOR_CLUSTER_JOBS
                case 2:
                    if (withResuming) {
                        assert suspendCancelled.get()
                    }
                    assert containSame(finishedClusterJobs, clusterJobs1)
                    assert !job.resumable
                    clusterJobs2.each {
                        assert createClusterJob(step, it).save(flush: true)
                    }
                    return NextAction.WAIT_FOR_CLUSTER_JOBS
                case 3:
                    assert containSame(finishedClusterJobs, clusterJobs2)
                    assert !job.resumable
                    return NextAction.SUCCEED
                default:
                    throw new UnsupportedOperationException("Phase ${phase} is not implemented.")
            }
        }
        job = createJob(mainLogic)

        scheduler.executeJob(job)
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 1
        assert job.state == AbstractJobImpl.State.STARTED
        assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
        assert job.resumable
        monitoredJobs = pbsMonitorService.queuedJobs[job]
        assert containSame(monitoredJobs, clusterJobs1)
        pbsMonitorService.queuedJobs = [:]

        if (withResuming) {
            job.planSuspend()
            assert job.resumable
            job.cancelSuspend()
        }

        pbsMonitorService.notifyJobAboutFinishedClusterJob(job, clusterJob2)
        assert atomicPhase.get() == 1
        assert job.state == AbstractJobImpl.State.STARTED
        assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
        assert job.resumable
        assert pbsMonitorService.queuedJobs.isEmpty()

        if (withResuming) {
            job.planSuspend()
            assert job.resumable
            new TestThread(suspendCancelled, job).start()
        }

        pbsMonitorService.notifyJobAboutFinishedClusterJob(job, clusterJob1)
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 2
        assert job.state == AbstractJobImpl.State.STARTED
        assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
        assert job.resumable
        monitoredJobs = pbsMonitorService.queuedJobs[job]
        assert containSame(monitoredJobs, clusterJobs2)
        pbsMonitorService.queuedJobs = [:]

        if (withResuming) {
            job.planSuspend()
            assert job.resumable
            assert createProcessingStepUpdate(step, ExecutionState.SUSPENDED).save(flush: true)

            final AbstractMultiJob newJobInstance = createJob(mainLogic)
            assert !newJobInstance.is(job)
            job = newJobInstance

            assert createProcessingStepUpdate(step, ExecutionState.RESUMED).save(flush: true)
            scheduler.executeJob(job)
            assert atomicPhase.get() == 2
            assert job.state == AbstractJobImpl.State.STARTED
            assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
            assert job.resumable
            monitoredJobs = pbsMonitorService.queuedJobs[job]
            assert containSame(monitoredJobs, clusterJobs2)
            pbsMonitorService.queuedJobs = [:]
        }

        pbsMonitorService.notifyJobAboutFinishedClusterJob(job, clusterJob3)
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 3
        assert job.state == AbstractJobImpl.State.FINISHED
        assert step.latestProcessingStepUpdate.state == ExecutionState.SUCCESS
        assert pbsMonitorService.queuedJobs.isEmpty()
        if (withResuming) {
            assertEquals(ExecutionState.SUCCESS, job.endState)
            List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
            assert updates.size() == 7
            assertEquals(ExecutionState.CREATED, updates[0].state)
            assertEquals(ExecutionState.STARTED, updates[1].state)
            assertEquals(ExecutionState.SUSPENDED, updates[2].state)
            assertEquals(ExecutionState.RESUMED, updates[3].state)
            assertEquals(ExecutionState.STARTED, updates[4].state)
            assertEquals(ExecutionState.FINISHED, updates[5].state)
            assertEquals(ExecutionState.SUCCESS, updates[6].state)
            assertTrue(step.process.finished)
        } else {
            assertSucceeded(job)
        }
    }

    @Test
    void testFailingJobInPhase1() {
        job = createJob { final int phase, final Collection<? extends ClusterJobIdentifier> finishedClusterJobs ->
            assert phase == 1
            throw new NumberFormatException(ARBITRARY_MESSAGE)
        }
        shouldFail NumberFormatException, { scheduler.executeJob(job) }
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 1
        assertFailed(job, ARBITRARY_MESSAGE)
    }

    @Test
    void testFailingJobInPhase2() {
        job = createJob { final int phase, final Collection<? extends ClusterJobIdentifier> finishedClusterJobs ->
            switch (phase) {
                case 1:
                    clusterJobs1.each {
                        assert createClusterJob(step, it).save(flush: true)
                    }
                    return NextAction.WAIT_FOR_CLUSTER_JOBS
                case 2:
                    throw new NumberFormatException(ARBITRARY_MESSAGE)
                default:
                    throw new UnsupportedOperationException("Phase ${phase} is not implemented.")
            }
        }

        scheduler.executeJob(job)
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 1
        assert job.state == AbstractJobImpl.State.STARTED
        assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED

        pbsMonitorService.notifyJobAboutFinishedClusterJob(job, clusterJob1)
        assert atomicPhase.get() == 1
        assert job.state == AbstractJobImpl.State.STARTED
        assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED

        pbsMonitorService.notifyJobAboutFinishedClusterJob(job, clusterJob2)
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 2

        assertFailed(job, ARBITRARY_MESSAGE)
    }

    static boolean containSame(final Collection<? extends ClusterJobIdentifier> c1, final Collection<? extends ClusterJobIdentifier> c2) {
        return TestCase.containSame(
                ClusterJobIdentifierImpl.asClusterJobIdentifierImplList(c1),
                ClusterJobIdentifierImpl.asClusterJobIdentifierImplList(c2))
    }
}

class TestThread extends Thread {

    AtomicBoolean suspendCancelled
    AbstractMultiJob job

    TestThread(AtomicBoolean suspendCancelled, AbstractMultiJob job) {
        this.suspendCancelled = suspendCancelled
        this.job = job
    }

    @Override
    void run() {
        Thread.sleep(200)
        suspendCancelled.set(true)
        job.cancelSuspend()
    }
}
