package de.dkfz.tbi.otp.job.processing

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LocalShellHelper.ProcessOutput
import grails.test.mixin.*
import spock.lang.*

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
        service.retrieveKnownJobsWithState(realm, SSHUSER)

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
        Map<ClusterJobIdentifier, ClusterJobMonitoringService.Status> result = service.retrieveKnownJobsWithState(realm, SSHUSER)

        then:
        result.isEmpty()
    }

    @Unroll
    void "retrieveKnownJobsWithState, when status #pbsStatus appears in qstat, returns correct status #status"(String pbsStatus, ClusterJobMonitoringService.Status status) {
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
        Map<ClusterJobIdentifier, ClusterJobMonitoringService.Status> result = service.retrieveKnownJobsWithState(realm, SSHUSER)

        then:
        def job = new ClusterJobIdentifier(realm, jobId, SSHUSER)
        result.containsKey(job)
        result.get(job) == status

        where:
        pbsStatus | status
        "C"       | ClusterJobMonitoringService.Status.COMPLETED
        "E"       | ClusterJobMonitoringService.Status.COMPLETED
        "H"       | ClusterJobMonitoringService.Status.NOT_COMPLETED
        "Q"       | ClusterJobMonitoringService.Status.NOT_COMPLETED
        "R"       | ClusterJobMonitoringService.Status.NOT_COMPLETED
        "T"       | ClusterJobMonitoringService.Status.NOT_COMPLETED
        "W"       | ClusterJobMonitoringService.Status.NOT_COMPLETED
        "S"       | ClusterJobMonitoringService.Status.NOT_COMPLETED
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
