package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import grails.test.mixin.web.*
import grails.validation.*
import spock.lang.*

@TestMixin(ControllerUnitTestMixin)
@Mock([
    DataFile,
    FileType,
    Individual,
    OtrsTicket,
    Project,
    Run,
    RunSegment,
    Sample,
    SampleType,
    SeqCenter,
    SeqPlatform,
    SeqTrack,
    SeqType,
    SoftwareTool,
])
class TrackingServiceSpec extends Specification {

    final static String TICKET_NUMBER = "2000010112345678"

    def 'test createOrResetOtrsTicket, when no OtrsTicket with ticket number exists, creates one' () {
        given:
        OtrsTicket otrsTicket
        TrackingService trackingService = new TrackingService()

        when:
        otrsTicket = trackingService.createOrResetOtrsTicket(TICKET_NUMBER)

        then:
        testTicket(otrsTicket)
    }

    def 'test createOrResetOtrsTicket, when OtrsTicket with ticket number exists, resets it' () {
        given:
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket([
                ticketNumber: '2000010112345678',
                installationFinished: new Date(),
                fastqcFinished: new Date(),
                alignmentFinished: new Date(),
                snvFinished: new Date(),
                finalNotificationSent: true,
        ])
        TrackingService trackingService = new TrackingService()

        when:
        otrsTicket = trackingService.createOrResetOtrsTicket(TICKET_NUMBER)

        then:
        testTicket(otrsTicket)
    }

    def 'test createOrResetOtrsTicket, when ticket number is null, throws ValidationException' () {
        given:
        TrackingService trackingService = new TrackingService()

        when:
        trackingService.createOrResetOtrsTicket(null)

        then:
        ValidationException ex = thrown()
        ex.message.contains("on field 'ticketNumber': rejected value [null]")
    }

    def 'test createOrResetOtrsTicket, when ticket number is blank, throws ValidationException' () {
        given:
        TrackingService trackingService = new TrackingService()

        when:
        trackingService.createOrResetOtrsTicket("")

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
