package de.dkfz.tbi.otp.job

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyCheck
import de.dkfz.tbi.otp.job.processing.ProcessService
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.MailHelperService
import org.junit.After
import org.junit.Test

class JobMailServiceTests {

    static final String RECIPIENT = 'a.b@c.d'

    JobMailService jobMailService

    ProcessService processService

    @After
    void tearDown() {
        TestCase.removeMetaClass(MailHelperService, jobMailService.mailHelperService)
    }

    @Test
    void testSendErrorNotificationIfFastTrack_withStringMessage_fastTrack_shouldSendEmail() {
        String message = 'test'
        doTestSendErrorNotificationIfFastTrack(message, message)
    }

    @Test
    void testSendErrorNotificationIfFastTrack_withException_fastTrack_shouldSendEmail() {
        Exception exception = new Exception('Test')
        doTestSendErrorNotificationIfFastTrack(exception.toString(), exception)
     }

    void doTestSendErrorNotificationIfFastTrack(String expectedText, def inputParameter) {
        doWithMockRecipient {
            SeqTrack seqTrack = DomainFactory.createSeqTrack()
            seqTrack.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
            assert seqTrack.project.save(flush: true, failOnError: true)

            ProcessingStep processingStep = DomainFactory.createProcessingStep()
            DomainFactory.createProcessParameter(processingStep.process, seqTrack)

            boolean executed = false
            jobMailService.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
                assert subject.startsWith('FASTTRACK ERROR: ')
                assert RECIPIENT == recipient
                assert content.contains(processingStep.processParameterObject.toString())
                assert content.contains(expectedText)
                assert content.contains(processService.processUrl(processingStep.process))
                if (executed) {
                    assert false: 'called twiced'
                } else {
                    executed = true
                }
            }

            jobMailService.sendErrorNotificationIfFastTrack(processingStep, inputParameter)
            assert executed
        }
    }

    @Test
    void testSendErrorNotificationIfFastTrack_noFastTrack_shouldNotSendEmail() {
        Exception exception = new Exception('Test')
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        ProcessingStep processingStep = DomainFactory.createProcessingStep()
        DomainFactory.createProcessParameter(processingStep.process, seqTrack)

        jobMailService.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
            assert false: 'should not be reached'
        }

        jobMailService.sendErrorNotificationIfFastTrack(processingStep, exception)
    }

    @Test
    void testSendErrorNotificationIfFastTrack_noRecipient_shouldFail() {
        doWithMockRecipient('') {
            Exception exception = new Exception('Test')
            SeqTrack seqTrack = DomainFactory.createSeqTrack()
            seqTrack.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
            assert seqTrack.project.save(flush: true, failOnError: true)

            ProcessingStep processingStep = DomainFactory.createProcessingStep()
            DomainFactory.createProcessParameter(processingStep.process, seqTrack)

            jobMailService.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
                assert false: 'should not be reached'
            }

            TestCase.shouldFailWithMessageContaining(AssertionError, 'recipient') {
                jobMailService.sendErrorNotificationIfFastTrack(processingStep, exception)
            }
        }
    }



    void doWithMockRecipient(String recipient = RECIPIENT, Closure closure) {
        jobMailService.grailsApplication.config.otp.mail.notification.fasttrack.to = recipient
        String oldRecipient = jobMailService.grailsApplication.config.otp.mail.notification.fasttrack.to
        try {
            closure()
        } finally {
            jobMailService.grailsApplication.config.otp.mail.notification.to = oldRecipient
        }
    }

}
