package de.dkfz.tbi.otp.qcTrafficLight

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        AbstractMergedBamFile,
        DataFile,
        ExomeSeqTrack,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProcessingOption,
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
class QcTrafficLightServiceSpec extends Specification {

    QcTrafficLightService qcTrafficLightService
    TestConfigService testConfigService

    @Unroll
    void "test changeQcTrafficLightStatusWithComment valid input (is rna: #rna, qcStatus: #qcStatus), succeeds"() {
        given:
        RoddyBamFile roddyBamFile = rna ? DomainFactory.createRnaRoddyBamFile() : DomainFactory.createRoddyBamFile()
        DomainFactory.createDefaultRealmWithProcessingOption()
        testConfigService = new TestConfigService()
        roddyBamFile.comment = new Comment(comment: "oldComment", author: "author", modificationDate: new Date())
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            linkResultsExecutions * linkNewResults(_, _)
            linkRnaResultsExecutions * linkNewRnaResults(_, _)
        }
        qcTrafficLightService.commentService = Mock(CommentService) {
            1 * saveComment(roddyBamFile, "comment")
        }
        qcTrafficLightService.configService = testConfigService
        qcTrafficLightService.configService.processingOptionService = new ProcessingOptionService()

        when:
        qcTrafficLightService.changeQcTrafficLightStatusWithComment(roddyBamFile, qcStatus, "comment")

        then:
        roddyBamFile.qcTrafficLightStatus == qcStatus

        where:
        rna   | qcStatus                                            | linkResultsExecutions | linkRnaResultsExecutions
        false | AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED  | 0                     | 0
        false | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED | 1                     | 0
        false | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | 0                     | 0
        true  | AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED  | 0                     | 0
        true  | AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED | 0                     | 1
        true  | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | 0                     | 0
    }

    void "test changeQcTrafficLightStatusWithComment invalid input, fails"() {
        given:
        qcTrafficLightService = new QcTrafficLightService()

        when:
        qcTrafficLightService.changeQcTrafficLightStatusWithComment(useBamFile ? DomainFactory.createRoddyBamFile() : null, qcStatus, comment)

        then:
        thrown(AssertionError)

        where:
        useBamFile | qcStatus                                            | comment
        true       | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | null
        true       | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | ""
        true       | null                                                | "comment"
        false      | AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | "comment"
    }
}
