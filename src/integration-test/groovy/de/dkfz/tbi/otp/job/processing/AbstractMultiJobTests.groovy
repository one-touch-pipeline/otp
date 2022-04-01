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
package de.dkfz.tbi.otp.job.processing

import grails.testing.mixin.integration.Integration
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.restarting.RestartCheckerService
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import static de.dkfz.tbi.otp.job.scheduler.SchedulerIntegrationTests.assertSucceeded
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.*
import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertTrue

@Integration
class AbstractMultiJobTests implements UserAndRoles {

    static final String CLUSTER_JOB_1_ID = '123'
    static final String CLUSTER_JOB_2_ID = '456'
    static final String CLUSTER_JOB_3_ID = '789'

    @Autowired
    ApplicationContext applicationContext
    OldClusterJobMonitor oldClusterJobMonitor
    RestartCheckerService restartCheckerService
    Scheduler scheduler

    ProcessingStep step
    Realm realm
    ClusterJobIdentifier clusterJob1
    ClusterJobIdentifier clusterJob2
    ClusterJobIdentifier clusterJob3
    Collection<ClusterJobIdentifier> clusterJobs1
    Collection<ClusterJobIdentifier> clusterJobs2

    final Semaphore semaphore = new Semaphore(0)
    final AtomicInteger atomicPhase = new AtomicInteger(0)
    final AtomicBoolean suspendCancelled = new AtomicBoolean(false)

    AbstractMultiJob job
    Collection<ClusterJobIdentifier> monitoredJobs

    void setupData() {
        SessionUtils.withTransaction {
            step = createAndSaveProcessingStep()
            realm = createRealm()
            clusterJob1 = new ClusterJobIdentifier(realm, CLUSTER_JOB_1_ID)
            clusterJob2 = new ClusterJobIdentifier(realm, CLUSTER_JOB_2_ID)
            clusterJob3 = new ClusterJobIdentifier(realm, CLUSTER_JOB_3_ID)
            clusterJobs1 = [clusterJob1, clusterJob2]
            clusterJobs2 = [clusterJob3]

            restartCheckerService.metaClass.canWorkflowBeRestarted = { ProcessingStep step -> false }

            assert scheduler.schedulerService.running.empty
        }
    }

    @After
    void tearDown() {
        SessionUtils.withTransaction {
            TestCase.removeMetaClass(RestartCheckerService, restartCheckerService)
            scheduler.schedulerService.running.clear()

            // Without cleaning the database, tests in other test classes fail.
            JobDefinition.withTransaction {
                ProcessingStepUpdate.list().each {
                    it.error = null
                    it.save(flush: true)
                }
                JobExecutionPlan.list().each {
                    it.firstJob = null
                    it.save(flush: true)
                }
                [
                        ClusterJob,
                        Realm,
                        ProcessingError,
                        ProcessingStepUpdate,
                        ProcessingStep,
                        Process,
                        JobDefinition,
                        JobExecutionPlan,
                ].each {
                    it.list(sort: "id", order: "desc").each {
                        it.delete(flush: true)
                    }
                }
            }
        }
    }

    /***
     * Check that a multi job work correctly.
     *
     * The test use a job following steps:
     * - sending two cluster jobs
     * - wait for cluster jobs
     * - validate finished cluster jobs plus send one additional cluster job
     * - wait for cluster jobs
     * - validate cluster job and finished successful
     */
    @Test
    void testSucceedingJobWithoutResuming() {
        setupData()
        succeedingJob(false)
    }

    /**
     * Check that a multi job work correctly together with suspending and resuming.
     *
     * The test extend the test {link #testSucceedingJobWithoutResuming} with suspending/resuming.
     */
    @Test
    void testSucceedingJobWithResuming() {
        setupData()
        succeedingJob(true)
    }

    /**
     * create a test job using given closure as execute method
     */
    private AbstractMultiJob createJob(final Closure mainLogic) {
        final AbstractMultiJob jobBean = applicationContext.getBean("testMultiJob")
        jobBean.processingStep = step

        //update the execute method of the test job for this test
        jobBean.metaClass.executeImpl = { final Collection<? extends ClusterJob> finishedClusterJobs ->
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
        SessionUtils.withTransaction {
            final Closure mainLogic = { final int phase, final Collection<? extends ClusterJob> finishedClusterJobs ->
                List<ClusterJobIdentifier> clusterJobIdentifiers = ClusterJobIdentifier.asClusterJobIdentifierList(finishedClusterJobs)
                switch (phase) {
                    case 1:
                        assert clusterJobIdentifiers == []
                        assert !job.resumable
                        clusterJobs1.each {
                            createClusterJob(step, it)
                        }
                        return NextAction.WAIT_FOR_CLUSTER_JOBS
                    case 2:
                        if (withResuming) {
                            assert suspendCancelled.get()
                        }
                        TestCase.assertContainSame(clusterJobIdentifiers, clusterJobs1)
                        assert !job.resumable
                        clusterJobs2.each {
                            createClusterJob(step, it)
                        }
                        return NextAction.WAIT_FOR_CLUSTER_JOBS
                    case 3:
                        TestCase.assertContainSame(clusterJobIdentifiers, clusterJobs2)
                        assert !job.resumable
                        return NextAction.SUCCEED
                    default:
                        throw new UnsupportedOperationException("Phase ${phase} is not implemented.")
                }
            }
            job = createJob(mainLogic)

            //start the job
            scheduler.schedulerService.running.add(job)
            scheduler.executeJob(job)

            //check that job is in phase 1
            assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
            assert atomicPhase.get() == 1
            assert job.state == AbstractJobImpl.State.STARTED
            assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
            assert job.resumable

            monitoredJobs = ClusterJobIdentifier.asClusterJobIdentifierList(ClusterJob.findAllByCheckStatus(ClusterJob.CheckStatus.CHECKING))
            TestCase.assertContainSame(monitoredJobs, clusterJobs1)

            if (withResuming) {
                //plan otp shutdown
                job.planSuspend()
                assert job.resumable
                //cancel otp shutdown, no otp restart, use same job instance
                job.cancelSuspend()
            }

            //first cluster job of phase 1 finished
            ClusterJob clusterJob = CollectionUtils.atMostOneElement(ClusterJob.findAllByClusterJobId(CLUSTER_JOB_2_ID))
            clusterJob.checkStatus = ClusterJob.CheckStatus.FINISHED
            clusterJob.save(flush: true)

            //check that the expected cluster jobs are in checking
            assert ClusterJob.findAllByCheckStatus(ClusterJob.CheckStatus.CHECKING)*.clusterJobId == [CLUSTER_JOB_1_ID]

            oldClusterJobMonitor.handleFinishedClusterJobs(clusterJob)

            //still in phase 1, since cluster 2 two of phase 1 still run
            assert atomicPhase.get() == 1
            assert job.state == AbstractJobImpl.State.STARTED
            assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
            assert job.resumable

            //handling of suspending/resuming
            if (withResuming) {
                //plan otp shutdown
                job.planSuspend()
                assert job.resumable
                //cancel otp shutdown from another thread, no otp restart, use same job instance
                new TestThread(suspendCancelled, job).start()
            }

            //second cluster job of phase 1 finished
            clusterJob = CollectionUtils.atMostOneElement(ClusterJob.findAllByClusterJobId(CLUSTER_JOB_1_ID))
            clusterJob.checkStatus = ClusterJob.CheckStatus.FINISHED
            clusterJob.save(flush: true)

            //check that no further jobs are in checking
            assert ClusterJob.findAllByCheckStatus(ClusterJob.CheckStatus.CHECKING).empty

            oldClusterJobMonitor.handleFinishedClusterJobs(clusterJob)

            //check that job is in phase 2, since all cluster jobs of phase 1 finished
            assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
            assert atomicPhase.get() == 2
            assert job.state == AbstractJobImpl.State.STARTED
            assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
            assert job.resumable

            //check that the expected cluster job is in checking
            monitoredJobs = ClusterJobIdentifier.asClusterJobIdentifierList(ClusterJob.findAllByCheckStatus(ClusterJob.CheckStatus.CHECKING))
            TestCase.assertContainSame(monitoredJobs, clusterJobs2)

            //handling of suspending/resuming
            if (withResuming) {
                //plan otp shutdown
                job.planSuspend()
                assert job.resumable
                assert createProcessingStepUpdate(step, ExecutionState.SUSPENDED).save(flush: true)

                //simulate OTP restart, so new job instance are used
                final AbstractMultiJob newJobInstance = createJob(mainLogic)
                assert !newJobInstance.is(job)
                scheduler.schedulerService.running.remove(job)
                scheduler.schedulerService.running.add(newJobInstance)
                job = newJobInstance

                //init new job instance with state
                assert createProcessingStepUpdate(step, ExecutionState.RESUMED).save(flush: true)
                scheduler.executeJob(job)
                assert atomicPhase.get() == 2
                assert job.state == AbstractJobImpl.State.STARTED
                assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED
                assert job.resumable
                monitoredJobs = ClusterJobIdentifier.asClusterJobIdentifierList(ClusterJob.findAllByCheckStatus(ClusterJob.CheckStatus.CHECKING))
                TestCase.assertContainSame(monitoredJobs, clusterJobs2)
            }

            //cluster job of phase 2 finished
            clusterJob = CollectionUtils.atMostOneElement(ClusterJob.findAllByClusterJobId(CLUSTER_JOB_3_ID))
            clusterJob.checkStatus = ClusterJob.CheckStatus.FINISHED
            clusterJob.save(flush: true)

            //check that no further cluster job is in checking
            assert ClusterJob.findAllByCheckStatus(ClusterJob.CheckStatus.CHECKING).empty

            oldClusterJobMonitor.handleFinishedClusterJobs(clusterJob)

            //check that job is in phase 3, since all cluster jobs of phase 1 finished
            assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
            assert atomicPhase.get() == 3
            assert job.state == AbstractJobImpl.State.FINISHED
            assert step.latestProcessingStepUpdate.state == ExecutionState.SUCCESS

            //test for handling of suspending/resuming
            if (withResuming) {
                //check that all expected state updates were created
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
                ProcessingStep refreshedStep = ProcessingStep.get(step.id)
                assertTrue(refreshedStep.process.finished)
            } else {
                assertSucceeded(job)
            }
            scheduler.schedulerService.running.remove(job)
        }
    }

    @Test
    void testFailingJobInPhase1() {
        setupData()
        SessionUtils.withTransaction {
            String message = HelperUtils.uniqueString
            job = createJob { final int phase, final Collection<? extends ClusterJob> finishedClusterJobs ->
                assert phase == 1
                throw new NumberFormatException(message)
            }
            scheduler.schedulerService.running.add(job)
            TestCase.shouldFail(NumberFormatException) {
                scheduler.executeJob(job)
            }
            assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
            assert atomicPhase.get() == 1
            SchedulerIntegrationTests.assertFailed(job, message)
        }
    }

    @Test
    void testFailingJobInPhase2() {
        setupData()
        SessionUtils.withTransaction {
            String message = HelperUtils.uniqueString
            job = createJob { final int phase, final Collection<? extends ClusterJob> finishedClusterJobs ->
                switch (phase) {
                    case 1:
                        clusterJobs1.each {
                            createClusterJob(step, it)
                        }
                        return NextAction.WAIT_FOR_CLUSTER_JOBS
                    case 2:
                        throw new NumberFormatException(message)
                    default:
                        throw new UnsupportedOperationException("Phase ${phase} is not implemented.")
                }
            }
            scheduler.schedulerService.running.add(job)

            scheduler.executeJob(job)
            assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
            assert atomicPhase.get() == 1
            assert job.state == AbstractJobImpl.State.STARTED
            assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED

            //first cluster job of phase 1 finished
            ClusterJob clusterJob = CollectionUtils.atMostOneElement(ClusterJob.findAllByClusterJobId(CLUSTER_JOB_1_ID))
            clusterJob.checkStatus = ClusterJob.CheckStatus.FINISHED
            clusterJob.save(flush: true)

            oldClusterJobMonitor.handleFinishedClusterJobs(clusterJob)
            assert atomicPhase.get() == 1
            assert job.state == AbstractJobImpl.State.STARTED
            assert step.latestProcessingStepUpdate.state == ExecutionState.STARTED

            //second cluster job of phase 1 finished
            clusterJob = CollectionUtils.atMostOneElement(ClusterJob.findAllByClusterJobId(CLUSTER_JOB_2_ID))
            clusterJob.checkStatus = ClusterJob.CheckStatus.FINISHED
            clusterJob.save(flush: true)

            oldClusterJobMonitor.handleFinishedClusterJobs(clusterJob)
            assert semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
            assert atomicPhase.get() == 2

            SchedulerIntegrationTests.assertFailed(job, message)
        }
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
        SessionUtils.withTransaction {
            job.cancelSuspend()
        }
    }
}
