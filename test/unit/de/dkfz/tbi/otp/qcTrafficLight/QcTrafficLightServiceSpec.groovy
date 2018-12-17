package de.dkfz.tbi.otp.qcTrafficLight

import grails.test.mixin.Mock
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.TrackingService

@Mock([
        AbstractMergedBamFile,
        DataFile,
        Comment,
        ExomeSeqTrack,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProcessingOption,
        OtrsTicket,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RnaRoddyBamFile,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqType,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqCenter,
        SeqTrack,
        SoftwareTool,
])
class QcTrafficLightServiceSpec extends Specification implements RoddyRnaFactory {

    QcTrafficLightService qcTrafficLightService
    TestConfigService testConfigService

    @Unroll
    void "test setQcTrafficLightStatusWithComment valid input (is rna: #rna, qcStatus: #qcStatus), succeeds"() {
        given:
        RoddyBamFile roddyBamFile = rna ? RoddyRnaFactory.super.createBamFile() : DomainFactory.createRoddyBamFile()
        DomainFactory.createDefaultRealmWithProcessingOption()
        testConfigService = new TestConfigService()
        roddyBamFile.comment = new Comment(comment: "oldComment", author: "author", modificationDate: new Date())
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            linkCount * linkNewResults(_, _)
            linkRnaCount * linkNewRnaResults(_, _)
        }
        qcTrafficLightService.commentService = Mock(CommentService) {
            1 * saveComment(roddyBamFile, "comment")
        }
        qcTrafficLightService.trackingService = Mock(TrackingService) {
            otrsCount * findAllOtrsTickets(roddyBamFile.seqTracks) >> []
        }
        qcTrafficLightService.configService = testConfigService
        qcTrafficLightService.configService.processingOptionService = new ProcessingOptionService()

        when:
        qcTrafficLightService.setQcTrafficLightStatusWithComment(roddyBamFile, qcStatus, "comment")

        then:
        roddyBamFile.qcTrafficLightStatus == qcStatus

        where:
        rna   | qcStatus                                            | linkCount | linkRnaCount | otrsCount
        false | AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED  | 0         | 0            | 0
        false | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED | 1         | 0            | 1
        false | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | 0         | 0            | 0
        true  | AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED  | 0         | 0            | 0
        true  | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED | 0         | 1            | 1
        true  | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | 0         | 0            | 0
    }

    void "test setQcTrafficLightStatusWithComment invalid input, fails"() {
        given:
        qcTrafficLightService = new QcTrafficLightService()

        when:
        qcTrafficLightService.setQcTrafficLightStatusWithComment(useBamFile ? DomainFactory.createRoddyBamFile() : null, qcStatus, comment)

        then:
        thrown(AssertionError)

        where:
        useBamFile | qcStatus                                            | comment
        true       | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | null
        true       | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | ""
        true       | null                                                | "comment"
        false      | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | "comment"
    }

    void "test setQcTrafficLightStatusWithComment set analysis of otrs to not sent"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                qcTrafficLightStatus: AbstractMergedBamFile.QcTrafficLightStatus.REJECTED,
        ])
        OtrsTicket otrsTicket1 = DomainFactory.createOtrsTicketWithEndDatesAndNotificationSent()
        OtrsTicket otrsTicket2 = DomainFactory.createOtrsTicketWithEndDatesAndNotificationSent()
        DomainFactory.createDefaultRealmWithProcessingOption()

        testConfigService = new TestConfigService()
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            1 * linkNewResults(_, _)
        }
        qcTrafficLightService.commentService = Mock(CommentService) {
            1 * saveComment(roddyBamFile, "comment")
        }
        qcTrafficLightService.trackingService = Spy(TrackingService) {
            1 * findAllOtrsTickets(roddyBamFile.seqTracks) >> [otrsTicket1, otrsTicket2]
        }
        qcTrafficLightService.configService = testConfigService
        qcTrafficLightService.configService.processingOptionService = new ProcessingOptionService()

        when:
        qcTrafficLightService.setQcTrafficLightStatusWithComment(
                roddyBamFile,
                AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED,
                "comment")

        then:
        false == otrsTicket1.finalNotificationSent
        null == otrsTicket1.snvFinished
        null == otrsTicket1.indelFinished
        null == otrsTicket1.sophiaFinished
        null == otrsTicket1.aceseqFinished
        null == otrsTicket1.runYapsaFinished

        false == otrsTicket2.finalNotificationSent
        null == otrsTicket1.snvFinished
        null == otrsTicket2.indelFinished
        null == otrsTicket2.sophiaFinished
        null == otrsTicket2.aceseqFinished
        null == otrsTicket2.runYapsaFinished
    }
}
