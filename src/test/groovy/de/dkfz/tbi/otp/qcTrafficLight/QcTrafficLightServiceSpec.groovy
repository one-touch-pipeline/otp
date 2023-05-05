/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.qcTrafficLight

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService

class QcTrafficLightServiceSpec extends Specification implements RoddyRnaFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AbstractBamFile,
                DataFile,
                Comment,
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
                FastqImportInstance,
                Sample,
                SampleType,
                SeqType,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqCenter,
                SeqTrack,
                SoftwareTool,
        ]
    }

    QcTrafficLightService qcTrafficLightService
    TestConfigService testConfigService

    @Unroll
    void "test setQcTrafficLightStatusWithComment valid input (is rna: #rna, qcStatus: #qcStatus), succeeds"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        RoddyBamFile roddyBamFile = rna ? RoddyRnaFactory.super.createBamFile() : DomainFactory.createRoddyBamFile()
        DomainFactory.createDefaultRealmWithProcessingOption()
        testConfigService = new TestConfigService()
        roddyBamFile.comment = new Comment(comment: "oldComment", author: "author", modificationDate: new Date())
        roddyBamFile.qcTrafficLightStatus = prevQcStatus
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            linkCount * linkNewResults(_, _)
            linkRnaCount * linkNewRnaResults(_, _)
        }
        qcTrafficLightService.commentService = Mock(CommentService) {
            1 * saveComment(roddyBamFile, "comment")
        }
        qcTrafficLightService.otrsTicketService = Mock(OtrsTicketService) {
            otrsCount * findAllOtrsTickets(roddyBamFile.seqTracks) >> []
        }
        qcTrafficLightService.configService = testConfigService
        qcTrafficLightService.configService.processingOptionService = new ProcessingOptionService()

        when:
        qcTrafficLightService.setQcTrafficLightStatusWithComment(roddyBamFile, qcStatus, "comment")

        then:
        roddyBamFile.qcTrafficLightStatus == qcStatus

        where:
        rna   | prevQcStatus                                           | qcStatus                                            | linkCount | linkRnaCount | otrsCount
        false | AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 1 | 0 | 1
        false | AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET | AbstractBamFile.QcTrafficLightStatus.WARNING  | 1 | 0 | 1
        false | AbstractBamFile.QcTrafficLightStatus.WARNING     | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 0 | 0 | 0
        false | AbstractBamFile.QcTrafficLightStatus.BLOCKED     | AbstractBamFile.QcTrafficLightStatus.WARNING  | 1 | 0 | 1
        false | AbstractBamFile.QcTrafficLightStatus.BLOCKED     | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 1 | 0 | 1
        true  | AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 0 | 1 | 1
        true  | AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET | AbstractBamFile.QcTrafficLightStatus.WARNING  | 0 | 1 | 1
        true  | AbstractBamFile.QcTrafficLightStatus.WARNING     | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 0 | 0 | 0
        true  | AbstractBamFile.QcTrafficLightStatus.BLOCKED     | AbstractBamFile.QcTrafficLightStatus.WARNING  | 0 | 1 | 1
        true  | AbstractBamFile.QcTrafficLightStatus.BLOCKED     | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 0 | 1 | 1
    }

    void "test setQcTrafficLightStatusWithComment invalid input, fails"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        qcTrafficLightService = new QcTrafficLightService()

        when:
        qcTrafficLightService.setQcTrafficLightStatusWithComment(useBamFile ? DomainFactory.createRoddyBamFile() : null, qcStatus, comment)

        then:
        thrown(AssertionError)

        where:
        useBamFile | qcStatus                                            | comment
        true       | AbstractBamFile.QcTrafficLightStatus.REJECTED | null
        true       | AbstractBamFile.QcTrafficLightStatus.REJECTED | ""
        true       | null                                                | "comment"
        false      | AbstractBamFile.QcTrafficLightStatus.REJECTED | "comment"
    }

    void "test setQcTrafficLightStatusWithComment set analysis of otrs to not sent"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                qcTrafficLightStatus: AbstractBamFile.QcTrafficLightStatus.REJECTED,
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
        qcTrafficLightService.otrsTicketService = Spy(OtrsTicketService) {
            1 * findAllOtrsTickets(roddyBamFile.seqTracks) >> [otrsTicket1, otrsTicket2]
        }
        qcTrafficLightService.configService = testConfigService
        qcTrafficLightService.configService.processingOptionService = new ProcessingOptionService()

        when:
        qcTrafficLightService.setQcTrafficLightStatusWithComment(
                roddyBamFile,
                AbstractBamFile.QcTrafficLightStatus.ACCEPTED,
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
