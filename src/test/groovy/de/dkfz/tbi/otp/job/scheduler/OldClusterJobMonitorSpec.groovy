/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.job.scheduler

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.JobMailService
import de.dkfz.tbi.otp.job.jobs.MonitoringTestJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.restarting.RestartCheckerService
import de.dkfz.tbi.otp.job.restarting.RestartHandlerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterStatisticService

import static de.dkfz.tbi.otp.ngsdata.DomainFactory.createAndSaveProcessingStep

class OldClusterJobMonitorSpec extends Specification implements DataTest {

    private OldClusterJobMonitor clusterJobMonitor

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ClusterJob,
                Realm,
                ProcessingStep,
                ProcessingStepUpdate,
                JobExecutionPlan,
        ]
    }

    void setupData() {
        SchedulerService schedulerService = new SchedulerService(
                processService: new ProcessService()
        )
        clusterJobMonitor = new OldClusterJobMonitor([
                clusterJobSchedulerService: Mock(ClusterJobSchedulerService),
                scheduler: new Scheduler([
                        jobMailService: Mock(JobMailService),
                        restartHandlerService: new RestartHandlerService([
                                restartCheckerService: Mock(RestartCheckerService) {
                                    canWorkflowBeRestarted(_) >> false
                                }
                        ]),
                        schedulerService: schedulerService,
                        processService: new ProcessService(),
                        errorLogService: Mock(ErrorLogService) {
                            _ * log(_) >> "1234"
                        },
                ]),
                schedulerService: schedulerService,
        ])
    }

    /**
     * Since the setup is long, this test considered multiple cases:
     * - state of cluster jobs
     *      - created
     *      - running
     *      - finished
     * - jobs on cluster
     *      - finished
     *      - running
     * - job state from cluster
     *      - successful
     *      - fail to get values
     * - cluster check for realm
     *      - successful
     *      - fail
     */
    void "check multiple cases for checking jobs"() {
        given:
        DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.CREATED,
        ])
        DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.CREATED,
        ])
        ClusterJob clusterJobCheckingRealm1 = DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
        ])
        ClusterJob clusterJobCheckingRealm2a = DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
        ])
        ClusterJob clusterJobCheckingRealm2b = DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
        ])
        DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.FINISHED,
        ])
        ClusterJob clusterJobCheckingRealmFailingGetValues = DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
        ])
        DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.FINISHED,
        ])

        MonitoringJob monitoringJob1 = Mock(MonitoringJob) {
            1 * getEndState() >> { throw new InvalidStateException() }
            1 * finished(clusterJobCheckingRealm1)
            0 * _
        }
        MonitoringJob monitoringJob2 = Mock(MonitoringJob) {
            1 * getEndState() >> { throw new InvalidStateException() }
            1 * finished(clusterJobCheckingRealm2b)
            0 * _
        }
        MonitoringJob monitoringJobF = Mock(MonitoringJob) {
            1 * getEndState() >> { throw new InvalidStateException() }
            1 * finished(clusterJobCheckingRealmFailingGetValues)
            0 * _
        }

        clusterJobMonitor = new OldClusterJobMonitor()
        clusterJobMonitor.schedulerService = Mock(SchedulerService) {
            1 * isActive() >> true
            1 * getJobForProcessingStep(clusterJobCheckingRealm1.processingStep) >> monitoringJob1
            1 * getJobForProcessingStep(clusterJobCheckingRealm2b.processingStep) >> monitoringJob2
            1 * getJobForProcessingStep(clusterJobCheckingRealmFailingGetValues.processingStep) >> monitoringJobF
            0 * _
        }
        GroovySpy(Realm, global: true)
        clusterJobMonitor.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * retrieveKnownJobsWithState() >> { ->
                return [
                        (clusterJobCheckingRealm1.clusterJobId): JobState.COMPLETED_SUCCESSFUL,
                        (clusterJobCheckingRealm2a.clusterJobId): JobState.RUNNING,
                        (clusterJobCheckingRealm2b.clusterJobId): JobState.COMPLETED_SUCCESSFUL,
                ]
            }
            0 * _
        }
        clusterJobMonitor.clusterStatisticService = Mock(ClusterStatisticService) {
            1 * retrieveAndSaveJobStatisticsAfterJobFinished(clusterJobCheckingRealm1)
            1 * retrieveAndSaveJobStatisticsAfterJobFinished(clusterJobCheckingRealm2b) >> { ClusterJob clusterJob ->
                throw new OtpRuntimeException("")
            }
        }
        clusterJobMonitor.scheduler = Mock(Scheduler) {
            3 * doWithErrorHandling(_, _, _) >> { MonitoringJob job, Closure closure, boolean rethrow ->
                return closure.call()
            }
            3 * doInJobContext(_, _) >> { MonitoringJob job, Closure closure ->
                return closure.call()
            }
            0 * _
        }

        when:
        clusterJobMonitor.check()

        then:
        clusterJobCheckingRealm1.refresh()
        clusterJobCheckingRealm1.checkStatus == ClusterJob.CheckStatus.FINISHED

        clusterJobCheckingRealm2a.refresh()
        clusterJobCheckingRealm2a.checkStatus == ClusterJob.CheckStatus.CHECKING

        clusterJobCheckingRealm2b.refresh()
        clusterJobCheckingRealm2b.checkStatus == ClusterJob.CheckStatus.FINISHED

        clusterJobCheckingRealmFailingGetValues.refresh()
        clusterJobCheckingRealmFailingGetValues.checkStatus == ClusterJob.CheckStatus.FINISHED

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(Realm)
    }

    void "check, if scheduler is inactive, do nothing "() {
        given:
        DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
        ])

        OldClusterJobMonitor clusterJobMonitor = new OldClusterJobMonitor()
        clusterJobMonitor.schedulerService = Mock(SchedulerService) {
            1 * isActive() >> false
            0 * _
        }
        clusterJobMonitor.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            0 * _
        }

        when:
        clusterJobMonitor.check()

        then:
        noExceptionThrown()
    }

    void "check, if no job is state checking exist, do nothing "() {
        given:
        DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.CREATED,
        ])
        DomainFactory.createClusterJob([
                checkStatus: ClusterJob.CheckStatus.FINISHED,
        ])

        OldClusterJobMonitor clusterJobMonitor = new OldClusterJobMonitor()
        clusterJobMonitor.schedulerService = Mock(SchedulerService) {
            1 * isActive() >> true
            0 * _
        }
        clusterJobMonitor.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            0 * _
        }
        GroovySpy(Realm, global: true)

        when:
        clusterJobMonitor.check()

        then:
        noExceptionThrown()

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(Realm)
    }

    void "test notifyJobAboutFinishedClusterJob success"() {
        given:
        setupData()

        when:
        notifyJobAboutFinishedClusterJob(false)

        then:
        ProcessingError.list().empty
    }

    void "test notifyJobAboutFinishedClusterJob failure"() {
        given:
        setupData()

        when:
        notifyJobAboutFinishedClusterJob(true)

        then:
        ProcessingError processingError = CollectionUtils.exactlyOneElement(ProcessingError.list())
        assert processingError.errorMessage == TestConstants.ARBITRARY_MESSAGE
    }

    private MonitoringJob notifyJobAboutFinishedClusterJob(final boolean fail) {
        ProcessingStep processingStep = createAndSaveProcessingStep()
        ClusterJob clusterJob = DomainFactory.createClusterJob(processingStep: processingStep)
        Job testJob = new MonitoringTestJob(processingStep, clusterJobMonitor.schedulerService, clusterJob, fail)

        clusterJobMonitor.scheduler.executeJob(testJob)
        assert clusterJobMonitor.schedulerService.jobExecutedByCurrentThread == null
        clusterJobMonitor.schedulerService.running.add(testJob)
        assert LogThreadLocal.threadLog == null

        clusterJobMonitor.handleFinishedClusterJobs(clusterJob)

        assert clusterJobMonitor.schedulerService.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
        assert testJob.executed
        return testJob
    }
}
