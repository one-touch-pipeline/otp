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
package de.dkfz.tbi.otp.tracking

import grails.testing.gorm.DataTest
import grails.web.mapping.LinkGenerator
import org.hibernate.HibernateException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.utils.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

class NotificationCreatorSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RawSequenceFile,
                ProcessingOption,
                Ticket,
                MetaDataFile,
                SeqTrack,
                UserProjectRole,
                FastqFile,
        ]
    }

    private NotificationCreator notificationCreator = new NotificationCreator()

    final String ticketPrefix = "prefix"

    void setup() {
        notificationCreator.processingOptionService = new ProcessingOptionService()
        notificationCreator.userProjectRoleService = new UserProjectRoleService()
        notificationCreator.ticketService = new TicketService(
                processingOptionService: new ProcessingOptionService(),
        )
        notificationCreator.createNotificationTextService = Stub(CreateNotificationTextService) {
            getMessageSourceService() >> Mock(MessageSourceService)
        }
        DomainFactory.createProcessingOptionForTicketPrefix(ticketPrefix)
        DomainFactory.createProcessingOptionForTicketSystemEmail()

        GroovyMock([global: true], GrailsArtefactCheckHelper)
    }

    @Unroll
    void 'setStarted, when tickets are given, then call saveStartTimeIfNeeded for with given #step'() {
        given:
        Ticket ticket1 = createTicket()
        Ticket ticket2 = createTicket()
        createTicket()

        NotificationCreator notificationCreator = new NotificationCreator([
                ticketService: Mock(TicketService) {
                    1 * saveStartTimeIfNeeded(ticket1, step)
                    1 * saveStartTimeIfNeeded(ticket2, step)
                }
        ])

        when:
        notificationCreator.setStarted([ticket1, ticket2], step)

        then:
        true

        where:
        step << Ticket.ProcessingStep.values()
    }

    @Unroll
    void 'setStartedForSeqTracks, when seqTracks are given, then call for each corresponding ticket the saveStartTimeIfNeeded with given #step'() {
        given:
        Ticket ticket1 = createTicket()
        Ticket ticket2 = createTicket()
        createTicket()

        Set<SeqTrack> seqTracks = [createSeqTrack()] as Set

        NotificationCreator notificationCreator = new NotificationCreator([
                ticketService: Mock(TicketService) {
                    1 * findAllTickets(_) >> ([ticket1, ticket2] as Set)
                    1 * saveStartTimeIfNeeded(ticket1, step)
                    1 * saveStartTimeIfNeeded(ticket2, step)
                },
        ])

        when:
        notificationCreator.setStartedForSeqTracks(seqTracks, step)

        then:
        true

        where:
        step << Ticket.ProcessingStep.values()
    }

    void 'sendProcessingStatusOperatorNotification, when finalNotification is false, sends normal notification with correct subject and content'() {
        given:
        Ticket ticket = createTicket()
        ProcessingStatus status = [
                getInstallationProcessingStatus: { -> ALL_DONE },
                getFastqcProcessingStatus      : { -> PARTLY_DONE_MIGHT_DO_MORE },
                getAlignmentProcessingStatus   : { -> NOTHING_DONE_MIGHT_DO },
                getSnvProcessingStatus         : { -> NOTHING_DONE_WONT_DO },
                getIndelProcessingStatus       : { -> NOTHING_DONE_MIGHT_DO },
                getSophiaProcessingStatus      : { -> NOTHING_DONE_MIGHT_DO },
                getAceseqProcessingStatus      : { -> NOTHING_DONE_MIGHT_DO },
                getRunYapsaProcessingStatus    : { -> NOTHING_DONE_WONT_DO },
        ] as ProcessingStatus
        Run runA = createRun(name: 'runA')
        Run runB = createRun(name: 'runB')
        Sample sample = createSample()
        SeqType seqType = createSeqType()
        String sampleText = "${sample.project.name}, ${sample.individual.pid}, ${sample.sampleType.name}, ${seqType.name} ${seqType.libraryLayout}"
        IlseSubmission ilseSubmission1 = createIlseSubmission(ilseNumber: 1234)
        IlseSubmission ilseSubmission2 = createIlseSubmission(ilseNumber: 5678)
        Closure createInstalledSeqTrack = { Map properties ->
            createSeqTrack([dataInstallationState: SeqTrack.DataProcessingState.FINISHED] + properties)
        }
        Set<SeqTrack> seqTracks = [
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: ilseSubmission2, run: runA, laneId: '1'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: ilseSubmission1, run: runB, laneId: '2'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: ilseSubmission1, run: runA, laneId: '4'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: ilseSubmission1, run: runA, laneId: '3'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: null, run: runB, laneId: '8'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: null, run: runA, laneId: '8'),
        ] as Set
        String expectedContent = """\
INSTALLATION: ALL_DONE
FASTQC:       PARTLY_DONE_MIGHT_DO_MORE
ALIGNMENT:    NOTHING_DONE_MIGHT_DO
SNV:          NOTHING_DONE_WONT_DO
INDEL:        NOTHING_DONE_MIGHT_DO
SOPHIA:       NOTHING_DONE_MIGHT_DO
ACESEQ:       NOTHING_DONE_MIGHT_DO
RUN_YAPSA:    NOTHING_DONE_WONT_DO

6 SeqTrack(s) in ticket ${ticket.ticketNumber}:
runA, lane 8, ${sampleText}
runB, lane 8, ${sampleText}
ILSe 1234, runA, lane 3, ${sampleText}
ILSe 1234, runA, lane 4, ${sampleText}
ILSe 1234, runB, lane 2, ${sampleText}
ILSe 5678, runA, lane 1, ${sampleText}
"""

        notificationCreator.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(_, _) >> { String emailSubject, String content ->
                assert "${ticketPrefix}#${ticket.ticketNumber} Processing Status Update".toString() == emailSubject
                assert content.startsWith(expectedContent)
            }
        }

        when:
        notificationCreator.sendProcessingStatusOperatorNotification(ticket, seqTracks, status, false)

        then:
        true
    }

    void 'sendProcessingStatusOperatorNotification, when finalNotification is true, sends final notification with correct subject'() {
        given:
        Ticket ticket = createTicket()
        notificationCreator.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem("${ticketPrefix}#${ticket.ticketNumber} Final Processing Status Update", _)
        }

        expect:
        notificationCreator.sendProcessingStatusOperatorNotification(ticket, [createSeqTrack()] as Set, new ProcessingStatus(), true)
    }

    void 'sendProcessingStatusOperatorNotification, when finalNotification is true, sending message contains a link to the import detail page'() {
        given:
        // one ticket with three imports
        Ticket ticket = createTicket()
        List<FastqImportInstance> fastqImportInstances = [
                createFastqImportInstance([ticket: ticket]),
                createFastqImportInstance([ticket: ticket]),
                createFastqImportInstance([ticket: ticket]),
        ]

        final Map linkProperties = [
                (LinkGenerator.ATTRIBUTE_CONTROLLER): "metadataImport",
                (LinkGenerator.ATTRIBUTE_ACTION)    : "details",
        ]

        // url path to the details page of metadata import
        final String pathMetadataImportDetail = linkProperties.values().join('/')

        notificationCreator.createNotificationTextService = new CreateNotificationTextService(
                messageSourceService: Mock(MessageSourceService),
                linkGenerator: Mock(LinkGenerator) {
                    3 * link(_) >> { Map params ->
                        assert params.size() == 4
                        assert params.get(LinkGenerator.ATTRIBUTE_CONTROLLER) == linkProperties.get(LinkGenerator.ATTRIBUTE_CONTROLLER)
                        assert params.get(LinkGenerator.ATTRIBUTE_ACTION) == linkProperties.get(LinkGenerator.ATTRIBUTE_ACTION)
                        assert params.get(LinkGenerator.ATTRIBUTE_ABSOLUTE)
                        return "http://dummy.com/${pathMetadataImportDetail}/${params.get(LinkGenerator.ATTRIBUTE_ID)}"
                    }
                },
        )

        notificationCreator.mailHelperService = Mock(MailHelperService)
        GroovyMock([global: true], GrailsArtefactCheckHelper)

        when:
        notificationCreator.sendProcessingStatusOperatorNotification(ticket, [createSeqTrack()] as Set, new ProcessingStatus(), true)

        then:
        1 * GrailsArtefactCheckHelper.check(_, _, _)
        1 * notificationCreator.createNotificationTextService.messageSourceService.createMessage(_) >> { String templateName ->
            assert templateName.contains("notification.import.detail.link")
            return "Details about metadata import can be found"
        }
        1 * notificationCreator.mailHelperService.sendEmailToTicketSystem(_, _) >> { String subject, String content ->
            assert content.contains(ticket.ticketNumber)
            fastqImportInstances.each {
                assert content.contains(pathMetadataImportDetail + "/${it.id}")
            }
        }
    }

    void 'sendWorkflowCreateSuccessMail, when called, the send success mail'() {
        given:
        Ticket ticket = createTicket()
        String message = "Some text ${nextId}"
        MetaDataFile metaDataFile = DomainFactory.createMetaDataFile([
                fastqImportInstance: createFastqImportInstance([
                        ticket: ticket,
                ]),
        ])
        notificationCreator.mailHelperService = Mock(MailHelperService)

        when:
        notificationCreator.sendWorkflowCreateSuccessMail(metaDataFile, message)

        then:
        1 * notificationCreator.mailHelperService.sendEmailToTicketSystem(_, _) >> { String emailSubject, String content ->
            assert emailSubject.startsWith("[${ticketPrefix}#${ticket.ticketNumber}]")
            assert emailSubject.contains("Workflow created successfully for ${metaDataFile.fileNameSource}")
            assert content.contains("The workflow creation succeeded:")
            assert content.contains("Import id: ${metaDataFile.fastqImportInstance.id}")
            assert content.contains(message)
        }
    }

    void 'sendWorkflowCreateErrorMail, when called, then send error mail'() {
        given:
        Ticket ticket = createTicket()
        List<RawSequenceFile> rawSequenceFiles = [
                createFastqFile(),
                createFastqFile(),
        ]
        MetaDataFile metaDataFile = DomainFactory.createMetaDataFile([
                fastqImportInstance: createFastqImportInstance([
                        ticket: ticket,
                        sequenceFiles: rawSequenceFiles,
                ]),
        ])
        notificationCreator.mailHelperService = Mock(MailHelperService)

        when:
        notificationCreator.sendWorkflowCreateErrorMail(metaDataFile, new HibernateException("Something happened"))

        then:
        1 * notificationCreator.mailHelperService.sendEmailToTicketSystem(_, _) >> { String emailSubject, String content ->
            assert emailSubject.startsWith("[${ticketPrefix}#${ticket.ticketNumber}]")
            assert emailSubject.contains("Failed to create workflows for ${metaDataFile.fileNameSource}")
            assert content.contains("The workflow creation failed:")
            assert content.contains("Import id: ${metaDataFile.fastqImportInstance.id}")
            assert content.contains("ctx.fastqImportInstanceService.updateState(FastqImportInstance.get(${metaDataFile.fastqImportInstance.id}), " +
                    "WorkflowCreateState.WAITING)")
            assert content.contains("https://gitlab.com/one-touch-pipeline/otp/-/blob/master/scripts/operations/dataCleanup/DeleteSeqtracks.groovy")
            rawSequenceFiles*.seqTrack.unique().each {
                assert content.contains(it.id.toString())
            }
        }
    }

    void "getProcessingStatus returns expected status"() {
        given:
        SeqTrack seqTrack1 = createSeqTrack([dataInstallationState: st1State])
        SeqTrack seqTrack2 = createSeqTrack([dataInstallationState: st2State])
        SeqTrack seqTrack3 = createSeqTrack([fastqcState: st1State])
        SeqTrack seqTrack4 = createSeqTrack([fastqcState: st2State])

        when:
        ProcessingStatus processingStatus1 = notificationCreator.getProcessingStatus([seqTrack1, seqTrack2])
        then:
        TestCase.assertContainSame(processingStatus1.seqTrackProcessingStatuses*.seqTrack, [seqTrack1, seqTrack2])
        processingStatus1.installationProcessingStatus == processingStatus

        when:
        ProcessingStatus processingStatus2 = notificationCreator.getProcessingStatus([seqTrack3, seqTrack4])
        then:
        TestCase.assertContainSame(processingStatus2.seqTrackProcessingStatuses*.seqTrack, [seqTrack3, seqTrack4])
        processingStatus2.fastqcProcessingStatus == processingStatus

        where:
        st1State                                 | st2State                                 || processingStatus
        SeqTrack.DataProcessingState.FINISHED    | SeqTrack.DataProcessingState.FINISHED    || ALL_DONE
        SeqTrack.DataProcessingState.FINISHED    | SeqTrack.DataProcessingState.IN_PROGRESS || PARTLY_DONE_MIGHT_DO_MORE
        SeqTrack.DataProcessingState.FINISHED    | SeqTrack.DataProcessingState.NOT_STARTED || PARTLY_DONE_MIGHT_DO_MORE
        SeqTrack.DataProcessingState.FINISHED    | SeqTrack.DataProcessingState.UNKNOWN     || PARTLY_DONE_MIGHT_DO_MORE
        SeqTrack.DataProcessingState.IN_PROGRESS | SeqTrack.DataProcessingState.IN_PROGRESS || NOTHING_DONE_MIGHT_DO
        SeqTrack.DataProcessingState.IN_PROGRESS | SeqTrack.DataProcessingState.NOT_STARTED || NOTHING_DONE_MIGHT_DO
        SeqTrack.DataProcessingState.IN_PROGRESS | SeqTrack.DataProcessingState.UNKNOWN     || NOTHING_DONE_MIGHT_DO
        SeqTrack.DataProcessingState.NOT_STARTED | SeqTrack.DataProcessingState.NOT_STARTED || NOTHING_DONE_MIGHT_DO
        SeqTrack.DataProcessingState.NOT_STARTED | SeqTrack.DataProcessingState.UNKNOWN     || NOTHING_DONE_MIGHT_DO
        SeqTrack.DataProcessingState.UNKNOWN     | SeqTrack.DataProcessingState.UNKNOWN     || NOTHING_DONE_MIGHT_DO
    }

    @Unroll
    void "CombineStatuses, when input is #input1 and #input2, then result is #result"() {
        expect:
        result == NotificationCreator.combineStatuses([input1, input2], Closure.IDENTITY)

        where:
        input1                    | input2                    || result
        NOTHING_DONE_WONT_DO      | NOTHING_DONE_WONT_DO      || NOTHING_DONE_WONT_DO
        NOTHING_DONE_MIGHT_DO     | NOTHING_DONE_WONT_DO      || NOTHING_DONE_MIGHT_DO
        PARTLY_DONE_WONT_DO_MORE  | NOTHING_DONE_WONT_DO      || PARTLY_DONE_WONT_DO_MORE
        PARTLY_DONE_MIGHT_DO_MORE | NOTHING_DONE_WONT_DO      || PARTLY_DONE_MIGHT_DO_MORE
        ALL_DONE                  | NOTHING_DONE_WONT_DO      || PARTLY_DONE_WONT_DO_MORE
        NOTHING_DONE_WONT_DO      | NOTHING_DONE_MIGHT_DO     || NOTHING_DONE_MIGHT_DO
        NOTHING_DONE_MIGHT_DO     | NOTHING_DONE_MIGHT_DO     || NOTHING_DONE_MIGHT_DO
        PARTLY_DONE_WONT_DO_MORE  | NOTHING_DONE_MIGHT_DO     || PARTLY_DONE_MIGHT_DO_MORE
        PARTLY_DONE_MIGHT_DO_MORE | NOTHING_DONE_MIGHT_DO     || PARTLY_DONE_MIGHT_DO_MORE
        ALL_DONE                  | NOTHING_DONE_MIGHT_DO     || PARTLY_DONE_MIGHT_DO_MORE
        NOTHING_DONE_WONT_DO      | PARTLY_DONE_WONT_DO_MORE  || PARTLY_DONE_WONT_DO_MORE
        NOTHING_DONE_MIGHT_DO     | PARTLY_DONE_WONT_DO_MORE  || PARTLY_DONE_MIGHT_DO_MORE
        PARTLY_DONE_WONT_DO_MORE  | PARTLY_DONE_WONT_DO_MORE  || PARTLY_DONE_WONT_DO_MORE
        PARTLY_DONE_MIGHT_DO_MORE | PARTLY_DONE_WONT_DO_MORE  || PARTLY_DONE_MIGHT_DO_MORE
        ALL_DONE                  | PARTLY_DONE_WONT_DO_MORE  || PARTLY_DONE_WONT_DO_MORE
        NOTHING_DONE_WONT_DO      | PARTLY_DONE_MIGHT_DO_MORE || PARTLY_DONE_MIGHT_DO_MORE
        NOTHING_DONE_MIGHT_DO     | PARTLY_DONE_MIGHT_DO_MORE || PARTLY_DONE_MIGHT_DO_MORE
        PARTLY_DONE_WONT_DO_MORE  | PARTLY_DONE_MIGHT_DO_MORE || PARTLY_DONE_MIGHT_DO_MORE
        PARTLY_DONE_MIGHT_DO_MORE | PARTLY_DONE_MIGHT_DO_MORE || PARTLY_DONE_MIGHT_DO_MORE
        ALL_DONE                  | PARTLY_DONE_MIGHT_DO_MORE || PARTLY_DONE_MIGHT_DO_MORE
        NOTHING_DONE_WONT_DO      | ALL_DONE                  || PARTLY_DONE_WONT_DO_MORE
        NOTHING_DONE_MIGHT_DO     | ALL_DONE                  || PARTLY_DONE_MIGHT_DO_MORE
        PARTLY_DONE_WONT_DO_MORE  | ALL_DONE                  || PARTLY_DONE_WONT_DO_MORE
        PARTLY_DONE_MIGHT_DO_MORE | ALL_DONE                  || PARTLY_DONE_MIGHT_DO_MORE
        ALL_DONE                  | ALL_DONE                  || ALL_DONE
    }

    ProcessingOption setupBlacklistImportSourceNotificationProcessingOption(String blacklist) {
        return DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.BLACKLIST_IMPORT_SOURCE_NOTIFICATION,
                type: null,
                project: null,
                value: blacklist,
        )
    }

    @Unroll
    void "getPrefixBlacklistFilteredStrings properly filters out Strings that are listed in the blacklist"() {
        when:
        setupBlacklistImportSourceNotificationProcessingOption(blacklist)
        List<String> result = notificationCreator.getPrefixBlacklistFilteredStrings(strings)

        then:
        result == expected

        where:
        strings                                       | blacklist       || expected
        ["/data/t1", "/data/t2", "/data/t3"]          | ""              || ["/data/t1", "/data/t2", "/data/t3"]
        ["/data/t1", "/data/t2", "/filtered/t3"]      | "/filtered"     || ["/data/t1", "/data/t2"]
        ["/data/t1", "/filtered/no", "/filtered/yes"] | "/filtered/yes" || ["/data/t1", "/filtered/no"]
        ["/data/t1", "/filtered/no", "/filtered/yes"] | "/filt"         || ["/data/t1"]
    }
}
