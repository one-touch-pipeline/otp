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
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

class NotificationCreatorSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                ProcessingOption,
                OtrsTicket,
                MetaDataFile,
                SeqTrack,
                UserProjectRole,
        ]
    }

    private NotificationCreator notificationCreator = new NotificationCreator()

    void setup() {
        notificationCreator.processingOptionService = new ProcessingOptionService()
        notificationCreator.userProjectRoleService = new UserProjectRoleService()
        notificationCreator.otrsTicketService = new OtrsTicketService()
        notificationCreator.createNotificationTextService = Stub(CreateNotificationTextService) {
            getMessageSourceService() >> Mock(MessageSourceService)
        }
        DomainFactory.createProcessingOptionForOtrsTicketPrefix("TICKET_PREFIX")
        DomainFactory.createProcessingOptionForTicketSystemEmail()

        GroovyMock([global : true], GrailsArtefactCheckHelper)
    }

    @Unroll
    void 'setStarted, when otrsTickets are given, then call saveStartTimeIfNeeded for with given #step'() {
        given:
        OtrsTicket otrsTicket1 = createOtrsTicket()
        OtrsTicket otrsTicket2 = createOtrsTicket()
        createOtrsTicket()

        NotificationCreator notificationCreator = new NotificationCreator([
                otrsTicketService: Mock(OtrsTicketService) {
                    1 * saveStartTimeIfNeeded(otrsTicket1, step)
                    1 * saveStartTimeIfNeeded(otrsTicket2, step)
                }
        ])

        when:
        notificationCreator.setStarted([otrsTicket1, otrsTicket2], step)

        then:
        true

        where:
        step << OtrsTicket.ProcessingStep.values()
    }

    @Unroll
    void 'setStartedForSeqTracks, when seqTracks are given, then call for each corresponding OtrsTicket the saveStartTimeIfNeeded with given #step'() {
        given:
        OtrsTicket otrsTicket1 = createOtrsTicket()
        OtrsTicket otrsTicket2 = createOtrsTicket()
        createOtrsTicket()

        Set<SeqTrack> seqTracks = [createSeqTrack()] as Set

        NotificationCreator notificationCreator = new NotificationCreator([
                otrsTicketService: Mock(OtrsTicketService) {
                    1 * findAllOtrsTickets(_) >> ([otrsTicket1, otrsTicket2] as Set)
                    1 * saveStartTimeIfNeeded(otrsTicket1, step)
                    1 * saveStartTimeIfNeeded(otrsTicket2, step)
                },
        ])

        when:
        notificationCreator.setStartedForSeqTracks(seqTracks, step)

        then:
        true

        where:
        step << OtrsTicket.ProcessingStep.values()
    }

    void 'sendProcessingStatusOperatorNotification, when finalNotification is false, sends normal notification with correct subject and content'() {
        given:
        OtrsTicket ticket = createOtrsTicket()
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
                assert "${ticket.prefixedTicketNumber} Processing Status Update".toString() == emailSubject
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
        OtrsTicket ticket = createOtrsTicket()
        notificationCreator.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem("${ticket.prefixedTicketNumber} Final Processing Status Update", _)
        }

        expect:
        notificationCreator.sendProcessingStatusOperatorNotification(ticket, [createSeqTrack()] as Set, new ProcessingStatus(), true)
    }

    void 'sendProcessingStatusOperatorNotification, when finalNotification is true and project.customFinalNotification is true and has an Ilse Number, sends final notification with correct subject'() {
        given:
        OtrsTicket ticket = createOtrsTicket()
        SeqTrack seqTrack = createSeqTrackforCustomFinalNotification(createProject(), createIlseSubmission(), ticket)
        String expectedHeader = "${ticket.prefixedTicketNumber} Final Processing Status Update [S#${seqTrack.ilseId}] ${seqTrack.individual.pid} " +
                "(${seqTrack.seqType.displayName})"
        notificationCreator.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(expectedHeader, _)
        }

        expect:
        notificationCreator.sendProcessingStatusOperatorNotification(ticket, [seqTrack] as Set, new ProcessingStatus(), true)
    }

    void 'sendProcessingStatusOperatorNotification, when finalNotification is true and project.customFinalNotification is true for multiple seqTracks, sends final notification with correct subject'() {
        given:
        OtrsTicket ticket = createOtrsTicket()
        Project project = createProject()
        List<SeqTrack> seqTracks = [
                createSeqTrackforCustomFinalNotification(project, createIlseSubmission(), ticket),
                createSeqTrackforCustomFinalNotification(project, createIlseSubmission(), ticket),
                createSeqTrackforCustomFinalNotification(project, createIlseSubmission(), ticket),
        ]

        String ilseString = seqTracks*.ilseId.sort().join(',')
        String pidString = seqTracks*.individual*.pid.sort().join(', ')
        String seqTypeStringString = seqTracks*.seqType*.displayName.sort().join(', ')
        String expectedHeader = "${ticket.prefixedTicketNumber} Final Processing Status Update [S#${ilseString}] ${pidString} (${seqTypeStringString})"

        notificationCreator.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(expectedHeader, _)
        }

        expect:
        notificationCreator.sendProcessingStatusOperatorNotification(ticket, seqTracks as Set, new ProcessingStatus(), true)
    }

    void 'sendProcessingStatusOperatorNotification, when finalNotification is true, sending message contains a link to the import detail page'() {
        given:
        //one ticket with three imports
        OtrsTicket ticket = createOtrsTicket()
        List<FastqImportInstance> fastqImportInstances = [
            createFastqImportInstance([otrsTicket : ticket]),
            createFastqImportInstance([otrsTicket : ticket]),
            createFastqImportInstance([otrsTicket : ticket]),
        ]

        final Map linkProperties = [
            (LinkGenerator.ATTRIBUTE_CONTROLLER) : "metadataImport",
            (LinkGenerator.ATTRIBUTE_ACTION)     : "details",
        ]

        //url path to the details page of metadata import
        final String pathMetadataImportDetail = linkProperties.values().join('/')

        notificationCreator.createNotificationTextService = new CreateNotificationTextService(
            messageSourceService : Mock(MessageSourceService),
            linkGenerator: Mock(LinkGenerator) {
                3 * link(_) >> { Map params ->
                    assert params.size() == 4
                    assert params.get(LinkGenerator.ATTRIBUTE_CONTROLLER) == linkProperties.get(LinkGenerator.ATTRIBUTE_CONTROLLER)
                    assert params.get(LinkGenerator.ATTRIBUTE_ACTION)     == linkProperties.get(LinkGenerator.ATTRIBUTE_ACTION)
                    assert params.get(LinkGenerator.ATTRIBUTE_ABSOLUTE)
                    return "http://dummy.com/${pathMetadataImportDetail}/${params.get(LinkGenerator.ATTRIBUTE_ID)}"
                }
            },
        )

        notificationCreator.mailHelperService = Mock(MailHelperService)
        GroovyMock([global : true], GrailsArtefactCheckHelper)

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

    void 'sendWorkflowCreateSuccessMail, when finalNotification is true, sending message contains a link to the import detail page'() {
        given:
        MetaDataFile metaDataFile = DomainFactory.createMetaDataFile([
                fastqImportInstance: createFastqImportInstance([
                        otrsTicket: createOtrsTicket()
                ])
        ])
        notificationCreator.mailHelperService = Mock(MailHelperService)

        when:
        notificationCreator.sendWorkflowCreateSuccessMail(metaDataFile)

        then:
        1 * notificationCreator.mailHelperService.sendEmailToTicketSystem(_, _) >> { arguments ->
            assert arguments[0].contains("Workflow created successfully for ${metaDataFile.fileName}")
            assert arguments[1].contains("The workflow creation succeeded:")
            assert arguments[1].contains("Import id: ${metaDataFile.fastqImportInstance.id}")
        }
    }

    void 'sendWorkflowCreateErrorMail, when finalNotification is true, sending message contains a link to the import detail page'() {
        given:
        MetaDataFile metaDataFile = DomainFactory.createMetaDataFile([
                fastqImportInstance: createFastqImportInstance([
                        otrsTicket: createOtrsTicket(),
                        dataFiles: [
                                createDataFile(),
                                createDataFile()
                        ],
                ])
        ])
        notificationCreator.mailHelperService = Mock(MailHelperService)

        when:
        notificationCreator.sendWorkflowCreateErrorMail(metaDataFile, new HibernateException("Something happened"))

        then:
        1 * notificationCreator.mailHelperService.sendEmailToTicketSystem(_, _) >> { arguments ->
            assert arguments[0].contains("Failed to create workflows for ${metaDataFile.fileName}")
            assert arguments[1].contains("The workflow creation failed:")
            assert arguments[1].contains("Import id: ${metaDataFile.fastqImportInstance.id}")
            assert arguments[1].contains("ctx.fastqImportInstanceService.updateState(FastqImportInstance.get(${metaDataFile.fastqImportInstance.id}), \
FastqImportInstance.WorkFlowTriggerState.WAITING)")
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

    private SeqTrack createSeqTrackforCustomFinalNotification(Project project, IlseSubmission ilseSubmission, OtrsTicket ticket) {
        SeqTrack seqTrack = createSeqTrackWithOneDataFile(
                [
                        ilseSubmission: ilseSubmission,
                        sample        : createSample(
                                individual: createIndividual(
                                        project: project
                                )
                        ),
                ],
                [fastqImportInstance: createFastqImportInstance(otrsTicket: ticket), fileLinked: true])
        project.customFinalNotification = true
        project.save(flush: true)
        return seqTrack
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
