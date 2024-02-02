/*
 * Copyright 2011-2024 The OTP authors
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
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService

class QcTrafficLightServiceSpec extends Specification implements RoddyRnaFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                Comment,
                FastqFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                Project,
                ProcessingOption,
                Ticket,
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
        testConfigService = new TestConfigService()
        roddyBamFile.comment = new Comment(comment: "oldComment", author: "author", modificationDate: new Date())
        roddyBamFile.qcTrafficLightStatus = prevQcStatus
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            linkCount * linkNewResults(_)
            linkRnaCount * linkNewRnaResults(_)
        }
        qcTrafficLightService.commentService = Mock(CommentService) {
            1 * saveComment(roddyBamFile, "comment")
        }
        qcTrafficLightService.ticketService = Mock(TicketService) {
            ticketCount * findAllTickets(roddyBamFile.seqTracks) >> []
        }
        qcTrafficLightService.configService = testConfigService
        qcTrafficLightService.configService.processingOptionService = new ProcessingOptionService()

        when:
        qcTrafficLightService.setQcTrafficLightStatusWithComment(roddyBamFile, qcStatus, "comment")

        then:
        roddyBamFile.qcTrafficLightStatus == qcStatus

        where:
        rna   | prevQcStatus                                           | qcStatus                                            | linkCount | linkRnaCount | ticketCount
        false | AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 1 | 0 | 1
        false | AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET | AbstractBamFile.QcTrafficLightStatus.WARNING  | 1 | 0 | 1
        false | AbstractBamFile.QcTrafficLightStatus.WARNING     | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 0 | 0 | 0
        true  | AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 0 | 1 | 1
        true  | AbstractBamFile.QcTrafficLightStatus.NOT_RUN_YET | AbstractBamFile.QcTrafficLightStatus.WARNING  | 0 | 1 | 1
        true  | AbstractBamFile.QcTrafficLightStatus.WARNING     | AbstractBamFile.QcTrafficLightStatus.ACCEPTED | 0 | 0 | 0
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
        true       | null                                                | "comment"
    }

    void "test setQcTrafficLightStatusWithComment set analysis of ticket system to not sent"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                qcTrafficLightStatus: AbstractBamFile.QcTrafficLightStatus.ACCEPTED,
        ])
        Ticket ticket1 = DomainFactory.createTicketWithEndDatesAndNotificationSent()
        Ticket ticket2 = DomainFactory.createTicketWithEndDatesAndNotificationSent()

        testConfigService = new TestConfigService()
        qcTrafficLightService = new QcTrafficLightService()
        qcTrafficLightService.linkFilesToFinalDestinationService = Mock(LinkFilesToFinalDestinationService) {
            1 * linkNewResults(_)
        }
        qcTrafficLightService.commentService = Mock(CommentService) {
            1 * saveComment(roddyBamFile, "comment")
        }
        qcTrafficLightService.ticketService = Spy(TicketService) {
            1 * findAllTickets(roddyBamFile.seqTracks) >> [ticket1, ticket2]
        }
        qcTrafficLightService.configService = testConfigService
        qcTrafficLightService.configService.processingOptionService = new ProcessingOptionService()

        when:
        qcTrafficLightService.setQcTrafficLightStatusWithComment(
                roddyBamFile,
                AbstractBamFile.QcTrafficLightStatus.ACCEPTED,
                "comment")

        then:
        false == ticket1.finalNotificationSent
        null == ticket1.snvFinished
        null == ticket1.indelFinished
        null == ticket1.sophiaFinished
        null == ticket1.aceseqFinished
        null == ticket1.runYapsaFinished

        false == ticket2.finalNotificationSent
        null == ticket1.snvFinished
        null == ticket2.indelFinished
        null == ticket2.sophiaFinished
        null == ticket2.aceseqFinished
        null == ticket2.runYapsaFinished
    }
}
