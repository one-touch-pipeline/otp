/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.cluster

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.job.scheduler.ClusterJobStatus
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.cluster.logs.ClusterLogQueryResultFileService

class ClusterStatisticServiceSpec extends Specification implements ServiceUnitTest<ClusterStatisticService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ClusterJob,
                Realm,
                WorkflowStep,
        ]
    }

    @Unroll
    void "retrieveKnownJobsWithState, when status #jobState appears, then returns correct clusterJobStatus #clusterJobStatus"() {
        given:
        String jobId = "${nextId}"
        Realm realm = createRealm()
        service.clusterJobManagerFactoryService = Mock(ClusterJobManagerFactoryService) {
            1 * getJobManager(realm) >> Mock(BatchEuphoriaJobManager) {
                1 * queryJobStatusAll() >> [
                        (new BEJobID(jobId)): jobState]
            }
        }
        service.fileService = Mock(FileService) {
            1 * createFileWithContentOnDefaultRealm(_, _)
        }
        service.clusterLogQueryResultFileService = Mock(ClusterLogQueryResultFileService) {
            1 * logFileWithCreatingDirectory()
        }

        ClusterJobIdentifier job = new ClusterJobIdentifier(realm, jobId)

        when:
        Map<ClusterJobIdentifier, ClusterJobStatus> result = service.retrieveKnownJobsWithState(realm)

        then:
        result.containsKey(job)
        result.get(job) == clusterJobStatus

        where:
        jobState                      || clusterJobStatus
        JobState.COMPLETED_SUCCESSFUL || ClusterJobStatus.COMPLETED
        JobState.COMPLETED_UNKNOWN    || ClusterJobStatus.COMPLETED
        JobState.FAILED               || ClusterJobStatus.COMPLETED
        JobState.ABORTED              || ClusterJobStatus.COMPLETED

        JobState.UNSTARTED            || ClusterJobStatus.NOT_COMPLETED
        JobState.HOLD                 || ClusterJobStatus.NOT_COMPLETED
        JobState.QUEUED               || ClusterJobStatus.NOT_COMPLETED
        JobState.RUNNING              || ClusterJobStatus.NOT_COMPLETED
        JobState.SUSPENDED            || ClusterJobStatus.NOT_COMPLETED

        JobState.UNKNOWN              || ClusterJobStatus.UNKNOWN
    }

    @Unroll
    void "retrieveAndSaveJobInformationAfterJobStarted, #name"() {
        given:
        String clusterId = "${nextId}"
        Realm realm = new Realm()
        ClusterJob job = new ClusterJob([
                clusterJobId: clusterId,
                realm       : realm,
        ])

        int amendClusterJobCallCount = calledAmendClusterJob ? 1 : 0

        int counter = 0
        service.clusterJobManagerFactoryService = Mock(ClusterJobManagerFactoryService) {
            1 * getJobManager(realm) >> Mock(BatchEuphoriaJobManager) {
                queryExtendedJobStateByIdCallCount * queryExtendedJobStateById(_) >> { List<BEJobID> jobIds ->
                    assert jobIds.size() == 1
                    if (counter < exceptionCount) {
                        counter++
                        throw new OtpRuntimeException()
                    }
                    return [(new BEJobID(clusterId)): new GenericJobInfo(null, null, null, null, null)]
                }
            }
        }
        service.clusterJobService = Mock(ClusterJobService) {
            amendClusterJobCallCount * amendClusterJob(_, _)
        }
        service.logService = Mock(LogService) {
            exceptionCount * addSimpleLogEntry(_, _)
        }

        expect:
        service.retrieveAndSaveJobInformationAfterJobStarted(job)

        where:
        name                        | exceptionCount || queryExtendedJobStateByIdCallCount | calledAmendClusterJob
        'get value the first time'  | 0              || 1                                  | true
        'get value the second time' | 1              || 2                                  | true
        'do not get the value'      | 2              || 2                                  | false
    }

    void "retrieveAndSaveJobStatisticsAfterJobFinished, when getting data is fine, then fill cluster job"() {
        given:
        String clusterId = "${nextId}"
        Realm realm = new Realm()
        ClusterJob job = new ClusterJob([
                clusterJobId: clusterId,
                realm       : realm,
        ])

        service.clusterJobManagerFactoryService = Mock(ClusterJobManagerFactoryService) {
            1 * getJobManager(realm) >> Mock(BatchEuphoriaJobManager) {
                1 * queryExtendedJobStateById(_) >> { List<BEJobID> jobIds ->
                    assert jobIds.size() == 1
                    return [(new BEJobID(clusterId)): new GenericJobInfo(null, null, null, null, null)]
                }
            }
        }
        service.clusterJobService = Mock(ClusterJobService) {
            1 * completeClusterJob(_, _, _)
        }

        expect:
        service.retrieveAndSaveJobStatisticsAfterJobFinished(job)
    }
}
