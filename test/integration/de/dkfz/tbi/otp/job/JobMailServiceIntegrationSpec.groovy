package de.dkfz.tbi.otp.job

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.apache.commons.logging.impl.*
import spock.lang.*

class JobMailServiceIntegrationSpec extends Specification {


    @Unroll
    void "sendErrorNotification, when with #completedCount completed cluster jobs and with #failedCount failed cluster job, send expected email"() {
        given:
        DomainFactory.createProcessingOptionForStatisticRecipient()
        ProcessingStep step = DomainFactory.createProcessingStepUpdate().processingStep

        List<ClusterJob> completedClusterJobs = []
        completedCount.times {
            completedClusterJobs << DomainFactory.createClusterJob([
                    processingStep: step,
                    exitStatus    : ClusterJob.Status.COMPLETED
            ])
        }

        List<ClusterJob> failedClusterJobs = []
        failedCount.times {
            failedClusterJobs << DomainFactory.createClusterJob([
                    processingStep: step,
                    exitStatus    : ClusterJob.Status.FAILED
            ])
        }

        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createProcessParameter(step.process, seqTrack)
        assert step.processParameterObject

        AbstractJobImpl job = Mock(AbstractJobImpl) {
            _ * getProcessingStep() >> step
            _ * getLogFilePath(_) >> TestCase.uniqueNonExistentPath
            0 * _
        }

        JobMailService jobMailService = new JobMailService([
                mailHelperService      : Mock(MailHelperService) {
                    1 * sendEmail(_, _, _) >> { String emailSubject, String content, List<String> recipients ->
                        assert content.contains('\nWorkflow:\n')
                        assert content.contains('\nOTP Job:\n')
                        assert (failedCount > 0) == content.contains('\nCluster Job:\n')
                        completedClusterJobs.each {
                            assert !content.contains("clusterId: ${it.id}")
                        }
                        failedClusterJobs.each {
                            assert content.contains("clusterId: ${it.id}")
                        }
                    }
                    0 * _
                },
                processService         : Mock(ProcessService) {
                    processUrl(_) >> 'url'
                },
                jobStatusLoggingService: Mock(JobStatusLoggingService) {
                    failedOrNotFinishedClusterJobs(_, _) >> {
                        final ProcessingStep processingStep, final Collection<ClusterJobIdentifier> clusterJobs ->
                            failedClusterJobs
                    }
                },
                log                    : new NoOpLog()
        ])

        when:
        jobMailService.sendErrorNotification(job, new RuntimeException())

        then:
        noExceptionThrown()

        where:
        completedCount | failedCount
        0              | 0
        5              | 0
        0              | 5
        2              | 3
    }
}
