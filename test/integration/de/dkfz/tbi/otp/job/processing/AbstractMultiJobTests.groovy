package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.restarting.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import org.apache.commons.logging.impl.*
import org.junit.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*

import java.util.concurrent.*
import java.util.concurrent.atomic.*

import static de.dkfz.tbi.TestConstants.*
import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.*
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.*
import static junit.framework.TestCase.*

class AbstractMultiJobTests {

    static final String CLUSTER_JOB_1_ID = '123'
    static final String CLUSTER_JOB_2_ID = '456'
    static final String CLUSTER_JOB_3_ID = '789'

    @Autowired
    ApplicationContext applicationContext
    ClusterJobMonitoringService clusterJobMonitoringService
    RestartCheckerService restartCheckerService
    Scheduler scheduler

    final ProcessingStep step = createAndSaveProcessingStep()
    final Realm realm1 = DomainFactory.createRealmDataProcessing()
    final Realm realm2 = DomainFactory.createRealmDataProcessing()
    final ClusterJobIdentifier clusterJob1 = new ClusterJobIdentifier(realm1, CLUSTER_JOB_1_ID, realm1.unixUser)
    final ClusterJobIdentifier clusterJob2 = new ClusterJobIdentifier(realm2, CLUSTER_JOB_2_ID, realm2.unixUser)
    final ClusterJobIdentifier clusterJob3 = new ClusterJobIdentifier(realm1, CLUSTER_JOB_3_ID, realm1.unixUser)
    final Collection<ClusterJobIdentifier> clusterJobs1 = [clusterJob1, clusterJob2]
    final Collection<ClusterJobIdentifier> clusterJobs2 = [clusterJob3]

    final Semaphore semaphore = new Semaphore(0)
    final AtomicInteger atomicPhase = new AtomicInteger(0)
    final AtomicBoolean suspendCancelled = new AtomicBoolean(false)

    AbstractMultiJob job
    Collection<ClusterJobIdentifier> monitoredJobs

    @Before
    void before() {
        assert realm1.save(flush: true)
        assert realm2.save(flush: true)
        clusterJobMonitoringService.queuedJobs = [:]
        restartCheckerService.metaClass.canWorkflowBeRestarted = { ProcessingStep step -> false }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(RestartCheckerService, restartCheckerService)
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
                    TestCase.assertContainSame(finishedClusterJobs, clusterJobs1)
                    assert !job.resumable
                    clusterJobs2.each {
                        assert createClusterJob(step, it).save(flush: true)
                    }
                    return NextAction.WAIT_FOR_CLUSTER_JOBS
                case 3:
                    TestCase.assertContainSame(finishedClusterJobs, clusterJobs2)
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
        monitoredJobs = clusterJobMonitoringService.queuedJobs[job]
        TestCase.assertContainSame(monitoredJobs, clusterJobs1)
        clusterJobMonitoringService.queuedJobs = [:]

        if (withResuming) {
            job.planSuspend()
            assert job.resumable
            job.cancelSuspend()
        }

        clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(job, clusterJob2)
        assert atomicPhase.get() == 1
        assert job.state == AbstractJobImpl.State.STARTED
        assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
        assert job.resumable
        assert clusterJobMonitoringService.queuedJobs.isEmpty()

        if (withResuming) {
            job.planSuspend()
            assert job.resumable
            new TestThread(suspendCancelled, job).start()
        }

        clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(job, clusterJob1)
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 2
        assert job.state == AbstractJobImpl.State.STARTED
        assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
        assert job.resumable
        monitoredJobs = clusterJobMonitoringService.queuedJobs[job]
        TestCase.assertContainSame(monitoredJobs, clusterJobs2)
        clusterJobMonitoringService.queuedJobs = [:]

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
            monitoredJobs = clusterJobMonitoringService.queuedJobs[job]
            TestCase.assertContainSame(monitoredJobs, clusterJobs2)
            clusterJobMonitoringService.queuedJobs = [:]
        }

        clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(job, clusterJob3)
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 3
        assert job.state == AbstractJobImpl.State.FINISHED
        assert step.latestProcessingStepUpdate.state == ExecutionState.SUCCESS
        assert clusterJobMonitoringService.queuedJobs.isEmpty()
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

        clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(job, clusterJob1)
        assert atomicPhase.get() == 1
        assert job.state == AbstractJobImpl.State.STARTED
        assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED

        clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(job, clusterJob2)
        assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
        assert atomicPhase.get() == 2

        assertFailed(job, ARBITRARY_MESSAGE)
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
