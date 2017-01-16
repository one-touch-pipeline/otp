package de.dkfz.tbi.otp.job

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import org.apache.commons.logging.impl.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

class JobMailServiceIntegrationSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    @Unroll
    void "sendErrorNotification, when with #completedCount completed cluster jobs and with #failedCount failed cluster job, send expected email"() {
        given:
        JobStatusLoggingService jobStatusLoggingService = new JobStatusLoggingService()

        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        Realm realm = DomainFactory.createRealmDataProcessing(temporaryFolder.newFolder(), [name: seqTrack.project.realmName])

        DomainFactory.createProcessingOptionForStatisticRecipient()
        ProcessingStep step = DomainFactory.createProcessingStepUpdate().processingStep


        List<ClusterJob> completedClusterJobs = []
        completedCount.times {
            ClusterJob clusterJob = DomainFactory.createClusterJob([
                    realm         : realm,
                    processingStep: step,
                    exitStatus    : ClusterJob.Status.COMPLETED
            ])
            completedClusterJobs << clusterJob
            File logFile = new File(jobStatusLoggingService.constructLogFileLocation(realm, step, clusterJob.clusterJobId))
            logFile.parentFile.mkdirs()
            logFile << jobStatusLoggingService.constructMessage(step, clusterJob.clusterJobId) << '\n'
        }

        List<ClusterJob> failedClusterJobs = []
        failedCount.times {
            failedClusterJobs << DomainFactory.createClusterJob([
                    realm         : realm,
                    processingStep: step,
                    exitStatus    : ClusterJob.Status.FAILED
            ])
        }

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
                            assert !content.contains("clusterId: ${it.clusterJobId}")
                        }
                        failedClusterJobs.each {
                            assert content.contains("clusterId: ${it.clusterJobId}")
                        }
                    }
                    0 * _
                },
                processService         : Mock(ProcessService) {
                    processUrl(_) >> 'url'
                },
                jobStatusLoggingService: jobStatusLoggingService,
                trackingService        : new TrackingService(),
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


    void "restartCount, when not restarted, return 0"() {
        given:
        ProcessingStep step = DomainFactory.createProcessingStep()

        expect:
        0 == new JobMailService().restartCount(step)
    }

    void "restartCount, when one time restarted, return 1"() {
        given:
        ProcessingStep step = DomainFactory.createRestartedProcessingStep()

        expect:
        1 == new JobMailService().restartCount(step)
    }

    void "restartCount, when two time restarted, return 2"() {
        given:
        ProcessingStep step = DomainFactory.createRestartedProcessingStep(original: DomainFactory.createRestartedProcessingStep())

        expect:
        2 == new JobMailService().restartCount(step)
    }
}
