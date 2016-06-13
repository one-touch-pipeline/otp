package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.PbsService.ClusterJobStatus
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import grails.test.mixin.Mock
import spock.lang.Specification


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

    void "knownJobsWithState, when qstat fails, throws exception"(String stderr, int exitCode) {
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
        thrown(IllegalStateException)

        where:
        stderr  | exitCode
        "ERROR" | 0
        ""      | 1
        "ERROR" | 1
    }


    void "knownJobsWithState, when qstat output is empty, returns empty map"() {
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
        Map<ClusterJobIdentifier, ClusterJobStatus> result = service.retrieveKnownJobsWithState(realm, realm.unixUser)

        then:
        result.isEmpty()
    }


    void "knownJobsWithState, when job appears in qstat output, returns correct status"(ClusterJobStatus status) {
        given:
        PbsService service = new PbsService()
        Realm realm = DomainFactory.createRealmDataProcessing()
        String jobId = "5075615"
        service.executionService = [
                executeCommandReturnProcessOutput: { realm1, String command, userName -> new ProcessOutput(
                        stdout: qstatOutput(jobId, status) + command.tokenize().last(),
                        stderr: "",
                        exitCode: 0,
                ) },
        ] as ExecutionService

        when:
        Map<ClusterJobIdentifier, ClusterJobStatus> result = service.retrieveKnownJobsWithState(realm, realm.unixUser)

        then:
        def job = new ClusterJobIdentifier(realm, jobId, realm.unixUser)
        result.containsKey(job)
        result.get(job) == status

        where:
        status                       | _
        ClusterJobStatus.COMPLETED   | _
        ClusterJobStatus.EXITED      | _
        ClusterJobStatus.HELD        | _
        ClusterJobStatus.QUEUED      | _
        ClusterJobStatus.RUNNING     | _
        ClusterJobStatus.BEING_MOVED | _
        ClusterJobStatus.WAITING     | _
        ClusterJobStatus.SUSPENDED   | _
    }


    private String qstatOutput(String jobId, ClusterJobStatus status) {
        return """\

clust_head.long-domain:
                                                                                  Req'd       Req'd       Elap
Job ID                  Username    Queue    Jobname          SessID  NDS   TSK   Memory      Time    S   Time
----------------------- ----------- -------- ---------------- ------ ----- ------ --------- --------- - ---------
${jobId}.clust_head.ine  OtherUnixUser    fast     r160224_18005293    --      1      1     750mb  00:10:00 ${status.code}       --
"""
    }

    void "test validateQstatResult, no jobs returned, succeeds"() {
        when:
        String end = HelperUtils.getUniqueString()
        PbsService.validateQstatResult(end, end)

        then:
        notThrown(IllegalStateException)
    }


    void "test validateQstatResult, one job returned, succeeds"() {
        when:
        String end = HelperUtils.getUniqueString()
        PbsService.validateQstatResult(qstatOutput("5075615", ClusterJobStatus.COMPLETED) + end, end)

        then:
        notThrown(IllegalStateException)
    }


    void "test validateQstatResult, missing end string"() {
        when:
        PbsService.validateQstatResult(qstatOutput("5075615", ClusterJobStatus.COMPLETED), HelperUtils.getUniqueString())

        then:
        thrown(IllegalStateException)
    }

    void "test validateQstatResult, invalid format"() {
        when:
        String end = HelperUtils.getUniqueString()
        PbsService.validateQstatResult("Invalid QStat output." + end, end)

        then:
        thrown(IllegalStateException)
    }


    void "test extractPbsId, succeeds"(String input, String result) {
        given:
        PbsService service = new PbsService()

        expect:
        result == service.extractPbsId(input)

        where:
        input                                      | result
        "4943549.headnode.long-domain" | "4943549"
        "49435.headnode.long-domain\n" | "49435"
        "1.headnode.long-domain"       | "1"
    }


    void "test extractPbsId, fails"(String input) {
        given:
        PbsService service = new PbsService()

        when:
        service.extractPbsId(input)

        then:
        def e = thrown(RuntimeException)
        e.message == "Could not extract exactly one pbs id from '${input}'"

        where:
        input                               | _
        "4943549.headnode.long-domain\n4943542.headnode.long-domain" | _
        "asdf"                              | _
        "123\n123"                          | _
        ""                                  | _
        ".headnode.long-domain" | _
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
        String result = service.executeJob(realm, "run the job", "")

        then:
        1 * service.executionService.executeCommandReturnProcessOutput(realm, _ as String) >> out
        1 * service.clusterJobService.createClusterJob(realm, clusterJobId, realm.unixUser, step, seqType, _ as String) >> clusterJob
        1 * service.clusterJobLoggingService.createAndGetLogDirectory(_, _) >> {TestCase.uniqueNonExistentPath}
        result == clusterJobId
    }
}
