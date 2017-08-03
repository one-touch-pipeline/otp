package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import grails.test.mixin.*
import spock.lang.*


@Mock([
        ClusterJob,
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingStep,
        ProcessingStepUpdate,
        Realm,
        SeqType,
])
class PbsServiceSpec extends Specification {

    void "retrieveKnownJobsWithState, when qstat fails, throws exception"(String stderr, int exitCode) {
        given:
        PbsService service = new PbsService()
        Realm realm = DomainFactory.createRealmDataProcessing()
        service.executionService = [
                executeCommandReturnProcessOutput: { realm1, command, userName -> new ProcessOutput(
                        stdout: "",
                        stderr: stderr,
                        exitCode: exitCode,
                ) },
        ] as ExecutionService

        when:
        service.retrieveKnownJobsWithState(realm, realm.unixUser)

        then:
        thrown(Exception)

        where:
        stderr  | exitCode
        ""      | 1
        "ERROR" | 1
    }


    void "retrieveKnownJobsWithState, when qstat output is empty, returns empty map"() {
        given:
        PbsService service = new PbsService()
        Realm realm = DomainFactory.createRealmDataProcessing()
        service.executionService = [
                executeCommandReturnProcessOutput: { realm1, command, userName -> new ProcessOutput(
                        stdout: command.tokenize().last(),
                        stderr: "",
                        exitCode: 0,
                ) },
        ] as ExecutionService

        when:
        Map<ClusterJobIdentifier, PbsMonitorService.Status> result = service.retrieveKnownJobsWithState(realm, realm.unixUser)

        then:
        result.isEmpty()
    }

    @Unroll
    void "retrieveKnownJobsWithState, when status #pbsStatus appears in qstat, returns correct status #status"(String pbsStatus, PbsMonitorService.Status status) {
        given:
        PbsService service = new PbsService()
        Realm realm = DomainFactory.createRealmDataProcessing()
        String jobId = "5075615"
        service.executionService = [
                executeCommandReturnProcessOutput: { realm1, String command, userName -> new ProcessOutput(
                        stdout: qstatOutput(jobId, pbsStatus),
                        stderr: "",
                        exitCode: 0,
                ) },
        ] as ExecutionService

        when:
        Map<ClusterJobIdentifier, PbsMonitorService.Status> result = service.retrieveKnownJobsWithState(realm, realm.unixUser)

        then:
        def job = new ClusterJobIdentifier(realm, jobId, realm.unixUser)
        result.containsKey(job)
        result.get(job) == status

        where:
        pbsStatus | status
        "C"       | PbsMonitorService.Status.COMPLETED
        "E"       | PbsMonitorService.Status.COMPLETED
        "H"       | PbsMonitorService.Status.NOT_COMPLETED
        "Q"       | PbsMonitorService.Status.NOT_COMPLETED
        "R"       | PbsMonitorService.Status.NOT_COMPLETED
        "T"       | PbsMonitorService.Status.NOT_COMPLETED
        "W"       | PbsMonitorService.Status.NOT_COMPLETED
        "S"       | PbsMonitorService.Status.NOT_COMPLETED
    }


    private static String qstatOutput(String jobId, String status) {
        return """\

clust_head.long-domain:
                                                                                  Req'd       Req'd       Elap
Job ID                  Username    Queue    Jobname          SessID  NDS   TSK   Memory      Time    S   Time
----------------------- ----------- -------- ---------------- ------ ----- ------ --------- --------- - ---------
${jobId}.clust_head.ine  OtherUnixUser    fast     r160224_18005293    --      1      1     750mb  00:10:00 ${status}       --
"""
    }


    void "test executeJob, succeeds"() {
        given:
        PbsService service = new PbsService()
        SeqType seqType = DomainFactory.createSeqType()
        Realm realm = DomainFactory.createRealmDataProcessing()
        String clusterJobId = "123"

        ProcessParameterObject ppo = Stub(ProcessParameterObject) {
            getSeqType() >> seqType
            getProcessingPriority() >> ProcessingPriority.NORMAL_PRIORITY
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

        service.pbsOptionMergingService = Stub(PbsOptionMergingService)
        service.jobStatusLoggingService = Stub(JobStatusLoggingService)
        service.executionService = Mock(ExecutionService)
        service.clusterJobService = Mock(ClusterJobService)
        service.clusterJobLoggingService = Mock(ClusterJobLoggingService)

        ProcessOutput out = new ProcessOutput("${clusterJobId}.pbs", "", 0)
        ClusterJob clusterJob = DomainFactory.createClusterJob(clusterJobId: clusterJobId, realm: realm)
        DomainFactory.createProcessingStepUpdate(processingStep: clusterJob.processingStep)

        when:
        String result = service.executeJob(realm, "run the job")

        then:
        2 * service.executionService.executeCommandReturnProcessOutput(realm, _ as String, _ as String) >> out
        1 * service.clusterJobService.createClusterJob(realm, clusterJobId, realm.unixUser, step, seqType, _ as String) >> clusterJob
        1 * service.clusterJobLoggingService.createAndGetLogDirectory(_, _) >> {TestCase.uniqueNonExistentPath}
        result == clusterJobId
    }
}
