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

package de.dkfz.tbi.otp.job.scheduler

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

@Mock([
        ClusterJob,
        Realm,
])
class ClusterJobMonitorSpec extends Specification {

    private ClusterJob createMockedClusterJob(Map map = [:]) {
        return DomainFactory.createClusterJob([
                processingStep: new ProcessingStep(),
                seqType       : new SeqType(),
                individual    : new Individual(),
        ] + map)
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
    @SuppressWarnings(['AbcMetric', 'MethodSize'])
    void "check multiple cases for checking jobs"() {
        given:
        Realm realm1 = DomainFactory.createRealm()
        Realm realm2 = DomainFactory.createRealm()
        Realm realmFailingGetValues = DomainFactory.createRealm()

        createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.CREATED,
        ])
        createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.CREATED,
                realm      : realm1,
        ])
        ClusterJob clusterJobCheckingRealm1 = createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
                realm      : realm1,
        ])
        ClusterJob clusterJobCheckingRealm2a = createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
                realm      : realm2,
        ])
        ClusterJob clusterJobCheckingRealm2b = createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
                realm      : realm2,
        ])
        createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.FINISHED,
                realm      : realm2,
        ])
        ClusterJob clusterJobCheckingRealmFailingGetValues = createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
                realm      : realmFailingGetValues,
        ])
        createMockedClusterJob([
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

        ClusterJobMonitor clusterJobMonitor = new ClusterJobMonitor()
        clusterJobMonitor.schedulerService = Mock(SchedulerService) {
            1 * isActive() >> true
            1 * getJobForProcessingStep(clusterJobCheckingRealm1.processingStep) >> monitoringJob1
            1 * getJobForProcessingStep(clusterJobCheckingRealm2b.processingStep) >> monitoringJob2
            0 * _
        }
        clusterJobMonitor.persistenceContextUtils = Mock(PersistenceContextUtils) {
            1 * doWithPersistenceContext(_) >> { Closure closure ->
                return closure.call()
            }
            0 * _
        }
        clusterJobMonitor.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * retrieveKnownJobsWithState(realm1) >> { Realm realm ->
                return [
                        (new ClusterJobIdentifier(realm1, clusterJobCheckingRealm1.clusterJobId)): ClusterJobStatus.COMPLETED,
                ]
            }
            1 * retrieveKnownJobsWithState(realm2) >> { Realm realm ->
                return [
                        (new ClusterJobIdentifier(realm2, clusterJobCheckingRealm2a.clusterJobId)): ClusterJobStatus.NOT_COMPLETED,
                        (new ClusterJobIdentifier(realm2, clusterJobCheckingRealm2b.clusterJobId)): ClusterJobStatus.COMPLETED,
                ]
            }
            1 * retrieveKnownJobsWithState(realmFailingGetValues) >> { Realm realm ->
                throw new OtpRuntimeException('No values')
            }
            1 * retrieveAndSaveJobStatisticsAfterJobFinished(clusterJobCheckingRealm1)
            1 * retrieveAndSaveJobStatisticsAfterJobFinished(clusterJobCheckingRealm2b) >> {
                throw new OtpRuntimeException()
            }
            0 * _
        }
        clusterJobMonitor.scheduler = Mock(Scheduler) {
            2 * doWithErrorHandling(_, _, _) >> { MonitoringJob job, Closure closure, boolean rethrow ->
                return closure.call()
            }
            2 * doInJobContext(_, _) >> { MonitoringJob job, Closure closure ->
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
        clusterJobCheckingRealmFailingGetValues.checkStatus == ClusterJob.CheckStatus.CHECKING
    }

    void "check, if scheduler is inactive, do nothing "() {
        given:
        createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.CHECKING,
        ])

        ClusterJobMonitor clusterJobMonitor = new ClusterJobMonitor()
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
        createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.CREATED,
        ])
        createMockedClusterJob([
                checkStatus: ClusterJob.CheckStatus.FINISHED,
        ])

        ClusterJobMonitor clusterJobMonitor = new ClusterJobMonitor()
        clusterJobMonitor.schedulerService = Mock(SchedulerService) {
            1 * isActive() >> true
            0 * _
        }
        clusterJobMonitor.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            0 * _
        }
        clusterJobMonitor.persistenceContextUtils = Mock(PersistenceContextUtils) {
            1 * doWithPersistenceContext(_) >> { Closure closure ->
                return closure.call()
            }
            0 * _
        }

        when:
        clusterJobMonitor.check()

        then:
        noExceptionThrown()
    }
}
