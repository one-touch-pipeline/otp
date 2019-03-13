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

import grails.test.mixin.Mock
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.scheduler.ClusterJobStatus
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessOutput

@Mock([
        ClusterJob,
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingOption,
        ProcessingStep,
        ProcessingStepUpdate,
        Realm,
        SeqType,
])
class ClusterJobSchedulerServiceSpec extends Specification {

    static final String SSHUSER = "user"

    void "retrieveKnownJobsWithState, when qstat fails, throws exception"(String stderr, int exitCode) {
        given:
        Realm realm = DomainFactory.createRealm()
        ClusterJobSchedulerService service = new ClusterJobSchedulerService()
        service.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
        }

        when:
        service.retrieveKnownJobsWithState(realm)

        then:
        thrown(Exception)

        where:
        stderr  | exitCode
        ""      | 1
        "ERROR" | 1
    }


    void "retrieveKnownJobsWithState, when qstat output is empty, returns empty map"() {
        given:
        Realm realm = DomainFactory.createRealm()
        File logFolder = TestCase.uniqueNonExistentPath
        ClusterJobSchedulerService service = new ClusterJobSchedulerService()
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { realm1, command ->
                    new ProcessOutput(
                            stdout: command.tokenize().last(),
                            stderr: "",
                            exitCode: 0,
                    )
                },
        ] as RemoteShellHelper
        service.clusterJobManagerFactoryService.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
        }
        service.configService = Mock(ConfigService) {
            1 * getLoggingRootPath() >> logFolder
        }
        service.fileService = Mock(FileService) {
            1 * createFileWithContent(_, _)
        }

        when:
        Map<ClusterJobIdentifier, ClusterJobStatus> result = service.retrieveKnownJobsWithState(realm)

        then:
        result.isEmpty()
    }

    @Unroll
    void "retrieveKnownJobsWithState, when status #pbsStatus appears in qstat, returns correct status #status"(String pbsStatus, ClusterJobStatus status) {
        given:
        Realm realm = DomainFactory.createRealm()
        File logFolder = TestCase.uniqueNonExistentPath
        String jobId = "5075615"
        ClusterJobSchedulerService service = new ClusterJobSchedulerService()
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { realm1, String command ->
                    new ProcessOutput(
                            stdout: qstatOutput(jobId, pbsStatus),
                            stderr: "",
                            exitCode: 0,
                    )
                },
        ] as RemoteShellHelper
        service.clusterJobManagerFactoryService.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
        }
        service.configService = Mock(ConfigService) {
            1 * getLoggingRootPath() >> logFolder
        }
        service.fileService = Mock(FileService) {
            1 * createFileWithContent(_, _)
        }

        when:
        Map<ClusterJobIdentifier, ClusterJobStatus> result = service.retrieveKnownJobsWithState(realm)

        then:
        def job = new ClusterJobIdentifier(realm, jobId)
        result.containsKey(job)
        result.get(job) == status

        where:
        pbsStatus | status
        "C"       | ClusterJobStatus.COMPLETED
        "E"       | ClusterJobStatus.COMPLETED
        "H"       | ClusterJobStatus.NOT_COMPLETED
        "Q"       | ClusterJobStatus.NOT_COMPLETED
        "R"       | ClusterJobStatus.NOT_COMPLETED
        "T"       | ClusterJobStatus.NOT_COMPLETED
        "W"       | ClusterJobStatus.NOT_COMPLETED
        "S"       | ClusterJobStatus.NOT_COMPLETED
    }


    private static String qstatOutput(String jobId, String status) {
        return """\

host.long-domain:
                                                                                  Req'd       Req'd       Elap
Job ID                  Username    Queue    Jobname          SessID  NDS   TSK   Memory      Time    S   Time
----------------------- ----------- -------- ---------------- ------ ----- ------ --------- --------- - ---------
${jobId}.host.long-doma  someUser    fast     r160224_18005293    --      1      1     750mb  00:10:00 ${status}       --
"""
    }


    void "test executeJob, succeeds"() {
        given:
        ClusterJobSchedulerService service = new ClusterJobSchedulerService()
        SeqType seqType = DomainFactory.createSeqType()
        Realm realm = DomainFactory.createRealm()
        String clusterJobId = "123"

        ProcessParameterObject ppo = Stub(ProcessParameterObject) {
            getSeqType() >> seqType
            getProcessingPriority() >> ProcessingPriority.NORMAL.priority
        }
        ProcessingStep step = Stub(ProcessingStep) {
            getProcessParameterObject() >> ppo
        }
        Job job = Stub(Job) {
            getProcessingStep() >> step
        }
        service.schedulerService = Stub(SchedulerService) {
            getJobExecutedByCurrentThread() >> job
        }
        service.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
        }
        service.clusterJobSubmissionOptionsService = Stub(ClusterJobSubmissionOptionsService)
        service.jobStatusLoggingService = Stub(JobStatusLoggingService)
        service.clusterJobService = Mock(ClusterJobService)
        service.clusterJobLoggingService = Mock(ClusterJobLoggingService)
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.remoteShellHelper = Mock(RemoteShellHelper)
        service.clusterJobManagerFactoryService.configService = Mock(ConfigService) {
            getSshUser() >> SSHUSER
        }

        ProcessOutput out = new ProcessOutput("${clusterJobId}.pbs", "", 0)
        ClusterJob clusterJob = DomainFactory.createClusterJob(clusterJobId: clusterJobId, realm: realm)
        DomainFactory.createProcessingStepUpdate(processingStep: clusterJob.processingStep)

        when:
        String result = service.executeJob(realm, "run the job")

        then:
        4 * service.clusterJobManagerFactoryService.remoteShellHelper.executeCommandReturnProcessOutput(realm, _ as String) >> out
        1 * service.clusterJobService.createClusterJob(realm, clusterJobId, SSHUSER, step, seqType, _ as String) >> clusterJob
        1 * service.clusterJobLoggingService.createAndGetLogDirectory(_, _) >> { TestCase.uniqueNonExistentPath }
        result == clusterJobId
    }


    private List createDataFor_retrieveAndSaveJobInformationAfterJobStarted(int queryExtendedJobStateByIdCallCount, boolean calledAmendClusterJob) {
        String clusterId = 1234
        Realm realm = new Realm()
        ClusterJob job = new ClusterJob([
                clusterJobId: clusterId,
                realm       : realm,
        ])

        int amendClusterJobCallCount = calledAmendClusterJob ? 1 : 0

        int counter = 0
        ClusterJobSchedulerService clusterJobSchedulerService = new ClusterJobSchedulerService([
                clusterJobManagerFactoryService: Mock(ClusterJobManagerFactoryService) {
                    1 * getJobManager(realm) >> {
                        return Mock(BatchEuphoriaJobManager) {
                            queryExtendedJobStateByIdCallCount * queryExtendedJobStateById(_) >> { List<BEJobID> jobIds ->
                                assert jobIds.size() == 1
                                counter++
                                if (queryExtendedJobStateByIdCallCount == counter && calledAmendClusterJob) {
                                    return [(new BEJobID(clusterId)): new GenericJobInfo(null, null, null, null, null)]
                                } else {
                                    throw new RuntimeException()
                                }
                            }
                        }
                    }
                },
                clusterJobService              : Mock(ClusterJobService) {
                    amendClusterJobCallCount * amendClusterJob(_, _)
                }
        ])
        return [
                job,
                clusterJobSchedulerService,
        ]
    }


    @Unroll
    void "retrieveAndSaveJobInformationAfterJobStarted, #name"() {
        given:
        String clusterId = 1234
        Realm realm = new Realm()
        ClusterJob job = new ClusterJob([
                clusterJobId: clusterId,
                realm       : realm,
        ])

        int amendClusterJobCallCount = calledAmendClusterJob ? 1 : 0

        int counter = 0
        ClusterJobSchedulerService clusterJobSchedulerService = new ClusterJobSchedulerService([
                clusterJobManagerFactoryService: Mock(ClusterJobManagerFactoryService) {
                    1 * getJobManager(realm) >> {
                        return Mock(BatchEuphoriaJobManager) {
                            queryExtendedJobStateByIdCallCount * queryExtendedJobStateById(_) >> { List<BEJobID> jobIds ->
                                assert jobIds.size() == 1
                                counter++
                                if (queryExtendedJobStateByIdCallCount == counter && calledAmendClusterJob) {
                                    return [(new BEJobID(clusterId)): new GenericJobInfo(null, null, null, null, null)]
                                } else {
                                    throw new RuntimeException()
                                }
                            }
                        }
                    }
                },
                clusterJobService              : Mock(ClusterJobService) {
                    amendClusterJobCallCount * amendClusterJob(_, _)
                }
        ])

        expect:
        clusterJobSchedulerService.retrieveAndSaveJobInformationAfterJobStarted(job)

        where:
        name                        | queryExtendedJobStateByIdCallCount | calledAmendClusterJob
        'get value the first time'  | 1                                  | true
        'get value the second time' | 2                                  | true
        'do not get the'            | 2                                  | false
    }
}
