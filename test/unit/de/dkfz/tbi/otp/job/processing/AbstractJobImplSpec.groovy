package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

@Mock([
        ClusterJob,
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingStep,
        ProcessingStepUpdate,
        Realm,
])
class AbstractJobImplSpec extends Specification {

    AbstractJobImpl abstractJobImpl
    TestConfigService configService

    Map processingStepHierarchy

    void setup() {
        processingStepHierarchy = createProcessingStepWithHierarchy()
    }

    void "test failedOrNotFinishedClusterJobs, no send step, throws RunTimeException"() {
        given:
        abstractJobImpl = [
                getProcessingStep : { return DomainFactory.createProcessingStep() },
        ] as AbstractJobImpl

        when:
        abstractJobImpl.failedOrNotFinishedClusterJobs()

        then:
        RuntimeException e = thrown()
        e.message.contains("No sending processing step found for")

    }

    void "test failedOrNotFinishedClusterJobs, no ClusterJob for send step, throws RunTimeException"() {
        given:
        abstractJobImpl = [
                getProcessingStep : { return processingStepHierarchy.validate },
        ] as AbstractJobImpl
        abstractJobImpl.jobStatusLoggingService = new JobStatusLoggingService()

        DomainFactory.createClusterJob(processingStep: processingStepHierarchy.validate)

        when:
        abstractJobImpl.failedOrNotFinishedClusterJobs()

        then:
        RuntimeException e = thrown()
        e.message.contains("No ClusterJobs found for")

    }

    void "test failedOrNotFinishedClusterJobs return list of failed or not finished jobs"() {
        given:
        configService = new TestConfigService()
        abstractJobImpl = [
                getProcessingStep : { return processingStepHierarchy.validate },
        ] as AbstractJobImpl
        abstractJobImpl.jobStatusLoggingService = new JobStatusLoggingService()
        abstractJobImpl.jobStatusLoggingService.configService = configService

        ClusterJob clusterJobSend = DomainFactory.createClusterJob(processingStep: processingStepHierarchy.send)

        expect:
        [clusterJobSend] == abstractJobImpl.failedOrNotFinishedClusterJobs()
    }

    private Map createProcessingStepWithHierarchy() {
        ProcessingStep send = DomainFactory.createProcessingStepWithUpdates()

        ProcessingStep wait = DomainFactory.createProcessingStep(process: send.process, previous: send)
        DomainFactory.createProcessingStepWithUpdates(wait)

        ProcessingStep validate = DomainFactory.createProcessingStep(process: wait.process, previous: wait)
        DomainFactory.createProcessingStepWithUpdates(validate)

        return [
                send : send,
                wait: wait,
                validate: validate,
        ]
    }
}
