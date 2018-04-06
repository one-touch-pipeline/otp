package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import grails.test.mixin.*
import spock.lang.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*

@Mock([
        SeqType,
        Pipeline,
        LibraryPreparationKit,
        Realm,
        Project,
        Individual,
        SampleType,
        Sample,
        SeqPlatformGroup,
        ReferenceGenome,
        MergingWorkPackage,
        ReferenceGenomeProjectSeqType,
        SeqPlatformModelLabel,
        SeqPlatform,
        SeqCenter,
        Run,
        FileType,
        SoftwareTool,
        SeqTrack,
        RunSegment,
        DataFile,
        MergingCriteria,
        RoddyBamFile,
        RoddyWorkflowConfig,
        ProcessingOption,
])
class QcTrafficLightServiceSpec extends Specification {

    QcTrafficLightService qcTrafficLightService
    TestConfigService testConfigService

    void "test changeQcTrafficLightStatusWithComment valid input, succeeds"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createDefaultRealmWithProcessingOption()
        testConfigService = new TestConfigService()
        roddyBamFile.comment = new Comment(comment: "oldComment", author: "author", modificationDate: new Date())
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            numberOfLinkNewResultsExecutions * linkNewResults(_, _)
        }
        qcTrafficLightService.commentService = Mock(CommentService) {
            1 * saveComment(roddyBamFile, "comment")
        }

        when:
        qcTrafficLightService.changeQcTrafficLightStatusWithComment(roddyBamFile, qcStatus, "comment")
        then:
        roddyBamFile.qcTrafficLightStatus == qcStatus


        where:
        qcStatus                                            | numberOfLinkNewResultsExecutions
        AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED  | 0
        AbstractMergedBamFile.QcTrafficLightStatus.ACCEPTED | 1
        AbstractMergedBamFile.QcTrafficLightStatus.REJECTED | 0
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
