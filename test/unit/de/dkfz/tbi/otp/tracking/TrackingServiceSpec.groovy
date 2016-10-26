package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import grails.test.mixin.web.*
import grails.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

@TestMixin(ControllerUnitTestMixin)
@Mock([
    DataFile,
    FileType,
    IlseSubmission,
    Individual,
    OtrsTicket,
    MergingWorkPackage,
    ProcessingOption,
    Project,
    ProjectCategory,
    ReferenceGenome,
    ReferenceGenomeProjectSeqType,
    Run,
    RunSegment,
    Sample,
    SampleType,
    SeqCenter,
    SeqPlatform,
    SeqPlatformGroup,
    SeqTrack,
    SeqType,
    SoftwareTool,
])
class TrackingServiceSpec extends Specification {

    final static String TICKET_NUMBER = "2000010112345678"

    TrackingService trackingService = new TrackingService()

    @Unroll
    def 'test createOrResetOtrsTicket, when no OtrsTicket with ticket number exists, creates one' () {
        given:
        OtrsTicket otrsTicket
        TrackingService trackingService = new TrackingService()

        when:
        otrsTicket = trackingService.createOrResetOtrsTicket(TICKET_NUMBER, comment)

        then:
        testTicket(otrsTicket)
        otrsTicket.seqCenterComment == comment

        where:
        comment << [
                null,
                '',
                'Some Cooment',
                'Some\nMultiline\nComment'
        ]
    }

    def 'test createOrResetOtrsTicket, when OtrsTicket with ticket number exists, resets it' () {
        given:
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket([
                ticketNumber: TICKET_NUMBER,
                installationFinished: new Date(),
                fastqcFinished: new Date(),
                alignmentFinished: new Date(),
                snvFinished: new Date(),
                finalNotificationSent: true,
        ])
        TrackingService trackingService = new TrackingService()

        when:
        otrsTicket = trackingService.createOrResetOtrsTicket(TICKET_NUMBER, null)

        then:
        testTicket(otrsTicket)
    }

    @Unroll
    def 'test createOrResetOtrsTicket, when OtrsTicket with ticket number exists, combine the seq center comment'() {
        given:
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket([
                ticketNumber: TICKET_NUMBER,
                seqCenterComment: comment1
        ])
        TrackingService trackingService = new TrackingService()

        when:
        otrsTicket = trackingService.createOrResetOtrsTicket(TICKET_NUMBER, comment2)

        then:
        resultComment == otrsTicket.seqCenterComment

        where:
        comment1     | comment2     || resultComment
        null         | null         || null
        'Something'  | null         || 'Something'
        null         | 'Something'  || 'Something'
        'Something'  | 'Something'  || 'Something'
        'Something1' | 'Something2' || 'Something1\n\nSomething2'
    }


    def 'test createOrResetOtrsTicket, when ticket number is null, throws ValidationException' () {
        given:
        TrackingService trackingService = new TrackingService()

        when:
        trackingService.createOrResetOtrsTicket(null, null)

        then:
        ValidationException ex = thrown()
        ex.message.contains("on field 'ticketNumber': rejected value [null]")
    }

    def 'test createOrResetOtrsTicket, when ticket number is blank, throws ValidationException' () {
        given:
        TrackingService trackingService = new TrackingService()

        when:
        trackingService.createOrResetOtrsTicket("", null)

        then:
        ValidationException ex = thrown()
        ex.message.contains("on field 'ticketNumber': rejected value []")
    }

    def 'test findAllOtrsTickets' () {
        given:
        TrackingService trackingService = new TrackingService()
        OtrsTicket otrsTicket01 = DomainFactory.createOtrsTicket()
        OtrsTicket otrsTicket02 = DomainFactory.createOtrsTicket()
        OtrsTicket otrsTicket03 = DomainFactory.createOtrsTicket()
        SeqTrack seqTrack01 = DomainFactory.createSeqTrack()
        SeqTrack seqTrack02 = DomainFactory.createSeqTrack()
        SeqTrack seqTrack03 = DomainFactory.createSeqTrack()
        SeqTrack seqTrack04 = DomainFactory.createSeqTrack()
        SeqTrack seqTrack05 = DomainFactory.createSeqTrack()
        SeqTrack seqTrack06 = DomainFactory.createSeqTrack()
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket01), seqTrack: seqTrack01)
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket02), seqTrack: seqTrack02)
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket03), seqTrack: seqTrack03)
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket01), seqTrack: seqTrack05)
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(), seqTrack: seqTrack06)

        when:
        Set<OtrsTicket> otrsTickets01 = trackingService.findAllOtrsTickets([seqTrack01, seqTrack02, seqTrack05])
        then:
        TestCase.assertContainSame(otrsTickets01, [otrsTicket01, otrsTicket02])

        when:
        Set<OtrsTicket> otrsTickets02 = trackingService.findAllOtrsTickets([seqTrack03])
        then:
        TestCase.assertContainSame(otrsTickets02, [otrsTicket03])

        when:
        Set<OtrsTicket> otrsTickets03 = trackingService.findAllOtrsTickets([seqTrack04])
        then:
        TestCase.assertContainSame(otrsTickets03, [])

        when:
        Set<OtrsTicket> otrsTickets04 = trackingService.findAllOtrsTickets([seqTrack06])
        then:
        TestCase.assertContainSame(otrsTickets04, [])
    }

    def 'test setStarted' () {
        given:
        TrackingService trackingService = new TrackingService()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()

        when:
        trackingService.setStarted([otrsTicket], step)

        then:
        otrsTicket."${step}Started" != null

        where:
        step                                    | _
        OtrsTicket.ProcessingStep.INSTALLATION  | _
        OtrsTicket.ProcessingStep.FASTQC        | _
        OtrsTicket.ProcessingStep.ALIGNMENT     | _
        OtrsTicket.ProcessingStep.SNV           | _
    }

    def 'test setStarted, twice' () {
        given:
        TrackingService trackingService = new TrackingService()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()

        when:
        trackingService.setStarted([otrsTicket], OtrsTicket.ProcessingStep.INSTALLATION)
        Date date = otrsTicket.installationStarted
        trackingService.setStarted([otrsTicket], OtrsTicket.ProcessingStep.INSTALLATION)

        then:
        date.is(otrsTicket.installationStarted)
    }

    void 'sendOperatorNotification, when finalNotification is false, sends normal notification with correct subject and content'() {
        given:
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        String prefix = "the prefix"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
        ProcessingStatus status = [
                getInstallationProcessingStatus: { -> ALL_DONE },
                getFastqcProcessingStatus: { -> PARTLY_DONE_MIGHT_DO_MORE },
                getAlignmentProcessingStatus: { -> NOTHING_DONE_MIGHT_DO },
                getSnvProcessingStatus: { -> NOTHING_DONE_WONT_DO },
        ] as ProcessingStatus
        Run runA = DomainFactory.createRun(name: 'runA')
        Run runB = DomainFactory.createRun(name: 'runB')
        Sample sample = DomainFactory.createSample()
        SeqType seqType = DomainFactory.createSeqType()
        String sampleText = "${sample.project.name}, ${sample.individual.pid}, ${sample.sampleType.name}, ${seqType.name} ${seqType.libraryLayout}"
        IlseSubmission ilseSubmission1 = DomainFactory.createIlseSubmission(ilseNumber: 1234)
        IlseSubmission ilseSubmission2 = DomainFactory.createIlseSubmission(ilseNumber: 5678)
        Closure createInstalledSeqTrack = { Map properties -> DomainFactory.createSeqTrack([dataInstallationState: SeqTrack.DataProcessingState.FINISHED] + properties) }
        Set<SeqTrack> seqTracks = [
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: ilseSubmission2, run: runA, laneId: '1'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: ilseSubmission1, run: runB, laneId: '2'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: ilseSubmission1, run: runA, laneId: '4'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: ilseSubmission1, run: runA, laneId: '3'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: null, run: runB, laneId: '8'),
                createInstalledSeqTrack(sample: sample, seqType: seqType, ilseSubmission: null, run: runA, laneId: '8'),
        ] as Set
        String expectedContent = """
Installation: ALL_DONE
FastQC:       PARTLY_DONE_MIGHT_DO_MORE
Alignment:    NOTHING_DONE_MIGHT_DO
SNV:          NOTHING_DONE_WONT_DO

6 SeqTrack(s) in ticket ${ticket.ticketNumber}:
runA, lane 8, ${sampleText}
runB, lane 8, ${sampleText}
ILSe 1234, runA, lane 3, ${sampleText}
ILSe 1234, runA, lane 4, ${sampleText}
ILSe 1234, runB, lane 2, ${sampleText}
ILSe 5678, runA, lane 1, ${sampleText}
"""

        String otrsRecipient = HelperUtils.uniqueString
        int callCount = 0
        trackingService.mailHelperService = new MailHelperService() {
            @Override
            String getOtrsRecipient() {
                return otrsRecipient
            }

            @Override
            void sendEmail(String emailSubject, String content, String recipient) {
                callCount++
                assertEquals("${prefix}#${ticket.ticketNumber} Processing Status Update".toString(), emailSubject)
                assertEquals(otrsRecipient, recipient)
                assertEquals(expectedContent, content)
            }
        }

        when:
        trackingService.sendOperatorNotification(ticket, seqTracks, status, false)

        then:
        callCount  == 1
    }

    void 'sendOperatorNotification, when finalNotification is true, sends final notification with correct subject'() {
        given:
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        String prefix = "the prefix"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
        String otrsRecipient = HelperUtils.uniqueString
        trackingService.mailHelperService = Mock(MailHelperService) {
            getOtrsRecipient() >> otrsRecipient
            1 * sendEmail("${prefix}#${ticket.ticketNumber} Final Processing Status Update", _, otrsRecipient)
        }

        expect:
        trackingService.sendOperatorNotification(ticket, Collections.emptySet(), new ProcessingStatus(), true)
    }

    @Unroll
    void 'sendCustomerNotification, when ProcessingStep.sendNotification is true, sends customer notification, else send nothing'() {
        given:
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        String otrsRecipient = HelperUtils.uniqueString
        int callCount = sendNotification ? 1 : 0
        DomainFactory.createProcessingOptionForOtrsTicketPrefix()

        trackingService.mailHelperService = Mock(MailHelperService) {
            callCount * getOtrsRecipient() >> otrsRecipient
            callCount * sendEmail(_, _, otrsRecipient)
        }
        trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
            callCount * notification(_, _, _)
        }

        expect:
        trackingService.sendCustomerNotification(ticket, new ProcessingStatus(), processingStep)

        where:
        processingStep                         | sendNotification
        OtrsTicket.ProcessingStep.INSTALLATION | true
        OtrsTicket.ProcessingStep.FASTQC       | false
        OtrsTicket.ProcessingStep.ALIGNMENT    | true
        OtrsTicket.ProcessingStep.SNV          | true
    }


    void "getProcessingStatus returns expected status"() {
        given:
        SeqTrack seqTrack1 = DomainFactory.createSeqTrack([dataInstallationState: st1State])
        SeqTrack seqTrack2 = DomainFactory.createSeqTrack([dataInstallationState: st2State])
        SeqTrack seqTrack3 = DomainFactory.createSeqTrack([fastqcState: st1State])
        SeqTrack seqTrack4 = DomainFactory.createSeqTrack([fastqcState: st2State])

        when:
        ProcessingStatus processingStatus1 = trackingService.getProcessingStatus([seqTrack1, seqTrack2])
        then:
        TestCase.containSame(processingStatus1.seqTrackProcessingStatuses*.seqTrack, [seqTrack1, seqTrack2])
        processingStatus1.installationProcessingStatus == processingStatus

        when:
        ProcessingStatus processingStatus2 = trackingService.getProcessingStatus([seqTrack3, seqTrack4])
        then:
        TestCase.containSame(processingStatus2.seqTrackProcessingStatuses*.seqTrack, [seqTrack3, seqTrack4])
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

    private boolean testTicket(OtrsTicket otrsTicket) {
        assert otrsTicket.ticketNumber == TICKET_NUMBER
        assert otrsTicket.installationFinished == null
        assert otrsTicket.fastqcFinished == null
        assert otrsTicket.alignmentFinished == null
        assert otrsTicket.snvFinished == null
        assert !otrsTicket.finalNotificationSent
        return true
    }
}
