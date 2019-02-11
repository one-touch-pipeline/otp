package de.dkfz.tbi.otp.qcTrafficLight

import grails.test.mixin.Mock
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*

@Mock([
        AbstractMergedBamFile,
        Comment,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        ProcessingOption,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
])
class QcTrafficLightCheckServiceSpec extends Specification implements IsRoddy {

    @Unroll
    void "handleQcCheck, if status is #status, then #text"() {
        given:
        int notifyCount = callNotify ? 1 : 0

        RoddyBamFile bamFile = createBamFile([
                qcTrafficLightStatus: status,
        ])

        QcTrafficLightCheckService service = new QcTrafficLightCheckService([
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService) {
                    notifyCount * informResultsAreBlocked(_)
                },
        ])

        boolean called = false
        Closure closure = {
            called = true
        }

        when:
        service.handleQcCheck(bamFile, closure)

        then:
        called == callCallback

        where:
        status                                                   || callCallback | callNotify
        null                                                     || true         | false
        AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED     || true         | false
        AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED       || false        | true
        AbstractMergedBamFile.QcTrafficLightStatus.AUTO_ACCEPTED || true         | true
        AbstractMergedBamFile.QcTrafficLightStatus.UNCHECKED     || true         | false

        text = "${callCallback ? '' : 'do not '}call the callback and ${callNotify ? '' : 'do not '} call the notify"
    }

    @Unroll
    void "handleQcCheck, if status is #status, then do not call the callback nor call the notification, but throw an exception"() {
        given:
        RoddyBamFile bamFile = createBamFile([
                qcTrafficLightStatus: status,
        ])

        QcTrafficLightCheckService service = new QcTrafficLightCheckService([
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService) {
                    0 * informResultsAreBlocked(_)
                },
        ])

        Closure closure = {
            throw new AssertionError('should not be called')
        }

        when:
        service.handleQcCheck(bamFile, closure)

        then:
        RuntimeException e = thrown()
        e.message.contains(status.toString())

        where:
        status << AbstractMergedBamFile.QcTrafficLightStatus.values().findAll {
            it.jobLinkCase == AbstractMergedBamFile.QcTrafficLightStatus.JobLinkCase.SHOULD_NOT_OCCUR ||
                    it.jobNotifyCase == AbstractMergedBamFile.QcTrafficLightStatus.JobNotifyCase.SHOULD_NOT_OCCUR
        }
    }
}
