package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.TrackingService.SamplePairDiscovery
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*
import static org.junit.Assert.*

class TrackingServiceIntegrationSpec extends IntegrationSpec {

    TrackingService trackingService
    MailHelperService mailHelperService
    SnvCallingService snvCallingService

    void setup() {
        // Overwrite the autowired service with a new instance for each test, so mocks do not have to be cleaned up
        trackingService = new TrackingService(
                mailHelperService: mailHelperService,
                snvCallingService: snvCallingService,
        )
    }

    void cleanup() {
        TestCase.removeMetaClass(TrackingService, trackingService)
    }

    void 'processFinished calls setFinishedTimestampsAndNotify for the tickets of the passed SeqTracks'() {
        given:
        OtrsTicket ticketA = DomainFactory.createOtrsTicket()
        SeqTrack seqTrackA = DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS],
                [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketA), fileLinked: true])

        OtrsTicket ticketB = DomainFactory.createOtrsTicket()
        SeqTrack seqTrackB1 = DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketB), fileLinked: true])
        SeqTrack seqTrackB2 = DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS],
                [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketB), fileLinked: true])

        DomainFactory.createProcessingOptionForOtrsTicketPrefix("the prefix")

        when:
        trackingService.processFinished([seqTrackA, seqTrackB1] as Set, OtrsTicket.ProcessingStep.FASTQC)

        then:
        ticketA.installationFinished != null
        ticketB.installationFinished != null
        ticketB.fastqcFinished == null
    }

    void 'setFinishedTimestampsAndNotify, when final notification has already been sent, does nothing'() {
        given:
        // Installation: finished timestamp set,     all done,     won't do more
        // FastQC:       finished timestamp not set, all done,     won't do more
        // Alignment:    finished timestamp not set, nothing done, won't do more
        // SNV:          finished timestamp not set, nothing done, won't do more
        Date installationFinished = new Date()
        OtrsTicket ticket = DomainFactory.createOtrsTicket(
                installationFinished: installationFinished,
                finalNotificationSent: true,
        )
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                [runSegment: runSegment, fileLinked: true])

        // mailHelperService shall be null such that the test fails with a NullPointerException if the method tries to
        // send a notification
        trackingService.mailHelperService = null

        when:
        trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairDiscovery())

        then:
        ticket.installationFinished == installationFinished
        ticket.fastqcFinished == null
        ticket.alignmentFinished == null
        ticket.snvFinished == null
        ticket.finalNotificationSent
    }

    void 'setFinishedTimestampsAndNotify, when nothing just completed, does nothing'() {
        given:
        // Installation: finished timestamp set,     all done,     won't do more
        // FastQC:       finished timestamp not set, partly done,  might do more
        // Alignment:    finished timestamp not set, nothing done, won't do more
        // SNV:          finished timestamp not set, nothing done, won't do more
        Date installationFinished = new Date()
        OtrsTicket ticket = DomainFactory.createOtrsTicket(
                installationFinished: installationFinished,
        )
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                [runSegment: runSegment, fileLinked: true])
        DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS],
                [runSegment: runSegment, fileLinked: true])

        // mailHelperService shall be null such that the test fails with a NullPointerException if the method tries to
        // send a notification
        trackingService.mailHelperService = null

        when:
        trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairDiscovery())

        then:
        ticket.installationFinished == installationFinished
        ticket.fastqcFinished == null
        ticket.alignmentFinished == null
        ticket.snvFinished == null
        !ticket.finalNotificationSent
    }

    void 'setFinishedTimestampsAndNotify, when something just completed and might do more, sends normal notification'() {
        given:
        // Installation: finished timestamp not set, all done,     won't do more
        // FastQC:       finished timestamp not set, nothing done, might do more
        // Alignment:    finished timestamp not set, nothing done, won't do more
        // SNV:          finished timestamp not set, nothing done, won't do more
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS],
                [runSegment: runSegment, fileLinked: true])
        ProcessingStatus expectedStatus = new ProcessingStatus(
                installationProcessingStatus: ALL_DONE,
                fastqcProcessingStatus: NOTHING_DONE_MIGHT_DO,
                alignmentProcessingStatus: NOTHING_DONE_WONT_DO,
                snvProcessingStatus: NOTHING_DONE_WONT_DO,
        )

        String prefix = "the prefix"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

        String otrsRecipient = HelperUtils.uniqueString
        int callCount = 0
        trackingService.mailHelperService = [
                getOtrsRecipient: { otrsRecipient },
                sendEmail: { String emailSubject, String content, String recipient ->
                    callCount++
                    assertEquals(otrsRecipient, recipient)
                    assert content.contains(expectedStatus.toString())
                    assertEquals("${prefix}#${ticket.ticketNumber} Processing Status Update".toString(), emailSubject)
                }
        ] as MailHelperService

        when:
        trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairDiscovery())

        then:
        ticket.installationFinished != null
        ticket.fastqcFinished == null
        ticket.alignmentFinished == null
        ticket.snvFinished == null
        !ticket.finalNotificationSent
        callCount == 1
    }

    void "setFinishedTimestampsAndNotify, when something just completed and won't do more, sends final notification"() {
        given:
        // Installation: finished timestamp not set, all done,     won't do more
        // FastQC:       finished timestamp not set, all done,     won't do more
        // Alignment:    finished timestamp not set, partly done,  won't do more
        // SNV:          finished timestamp not set, nothing done, won't do more
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                [runSegment: runSegment, fileLinked: true])
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(
                [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                [runSegment: runSegment, fileLinked: true]
        )
        setBamFileInProjectFolder(DomainFactory.createRoddyBamFile(
                DomainFactory.createRoddyBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage(
                                MergingWorkPackage.getMergingProperties(seqTrack) +
                                        [pipeline: DomainFactory.createPanCanPipeline()]
                        )
                ]),
                DomainFactory.randomProcessedBamFileProperties + [seqTracks: [seqTrack] as Set],
        ))
        ProcessingStatus expectedStatus = new ProcessingStatus(
                installationProcessingStatus: ALL_DONE,
                fastqcProcessingStatus: ALL_DONE,
                alignmentProcessingStatus: PARTLY_DONE_WONT_DO_MORE,
                snvProcessingStatus: NOTHING_DONE_WONT_DO,
        )

        String prefix = "the prefix"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

        String otrsRecipient = HelperUtils.uniqueString
        int callCount = 0
        trackingService.mailHelperService = [
            getOtrsRecipient: { otrsRecipient },
            sendEmail: { String emailSubject, String content, String recipient ->
                callCount++
                assertEquals(otrsRecipient, recipient)
                assert content.contains(expectedStatus.toString())
                assertEquals("${prefix}#${ticket.ticketNumber} Final Processing Status Update".toString(), emailSubject)
            }
        ] as MailHelperService

        when:
        trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairDiscovery())

        then:
        ticket.installationFinished != null
        ticket.fastqcFinished == ticket.installationFinished
        ticket.alignmentFinished == ticket.installationFinished
        ticket.snvFinished == null
        ticket.finalNotificationSent
        callCount == 1
    }

    void "getAlignmentAndDownstreamProcessingStatus, No ST, returns NOTHING_DONE_WONT_DO"() {
        expect:
        NOTHING_DONE_WONT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus([] as Set, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 ST not alignable, returns NOTHING_DONE_WONT_DO"() {
        given:
        Set<SeqTrack> seqTracks = [DomainFactory.createSeqTrackWithOneDataFile([:], [fileWithdrawn: true])]

        expect:
        NOTHING_DONE_WONT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 MWP ALL_DONE, returns ALL_DONE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)

        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks

        expect:
        ALL_DONE == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 MWP NOTHING_DONE_MIGHT_DO, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)

        Set<SeqTrack> seqTracks = [DomainFactory.createSeqTrackWithDataFiles(bamFile.mergingWorkPackage)]

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 ST not alignable, 1 MWP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)

        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks + [DomainFactory.createSeqTrackWithDataFiles(bamFile.mergingWorkPackage, [:], [fileWithdrawn: true])]

        expect:
        PARTLY_DONE_WONT_DO_MORE == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, no MWP, returns NOTHING_DONE_WONT_DO"() {
        given:
        Set<SeqTrack> seqTracks = [DomainFactory.createSeqTrackWithOneDataFile()]

        expect:
        NOTHING_DONE_WONT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(bamFile.containedSeqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 2 MergingProperties, 1 MWP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)

        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks + [DomainFactory.createSeqTrackWithOneDataFile()]

        expect:
        PARTLY_DONE_WONT_DO_MORE == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 2 MergingProperties, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()

        Set<SeqTrack> seqTracks = bamFile.containedSeqTracks + [DomainFactory.createSeqTrackWithOneDataFile()]

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getAlignmentAndDownstreamProcessingStatus, 2 MergingProperties, 1 MWP NOTHING_DONE_MIGHT_DO, 1 MWP ALL_DONE, returns PARTLY_DONE_MIGHT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile1 = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
        AbstractMergedBamFile bamFile2 = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)

        Set<SeqTrack> seqTracks = bamFile1.containedSeqTracks + bamFile2.containedSeqTracks

        DomainFactory.createSeqTrackWithDataFiles(bamFile2.mergingWorkPackage)

        expect:
        PARTLY_DONE_MIGHT_DO_MORE == trackingService.getAlignmentAndDownstreamProcessingStatus(seqTracks, new SamplePairDiscovery()).alignmentProcessingStatus
    }

    void "getSnvProcessingStatus, no MWP, returns NOTHING_DONE_WONT_DO"() {
        expect:
        NOTHING_DONE_WONT_DO == trackingService.getSnvProcessingStatus([] as Set, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, no SP, returns NOTHING_DONE_WONT_DO"() {
        given:
        Set<MergingWorkPackage> mergingWorkPackages = [DomainFactory.createMergingWorkPackage()]

        expect:
        NOTHING_DONE_WONT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 1 SCI FINISHED, bamFileInProjectFolder set, returns ALL_DONE"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        expect:
        ALL_DONE == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 1 SCI not FINISHED, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 1 SCI FINISHED, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, no SCI, bamFileInProjectFolder set, no samplePairForSnvProcessing, returns NOTHING_DONE_WONT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        snvCallingInstance.delete(flush: true)

        expect:
        NOTHING_DONE_WONT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, no SCI, bamFileInProjectFolder set, samplePairForSnvProcessing exists, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles([:], [coverage: 2], [coverage: 2])

        DomainFactory.createSnvConfigForSnvCallingInstance(snvCallingInstance)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
            DomainFactory.createProcessingThresholdsForBamFile(snvCallingInstance."sampleType${it}BamFile", [coverage: 1, numberOfLanes: null])
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        snvCallingInstance.delete(flush: true)

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, no SCI, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage]

        snvCallingInstance.delete(flush: true)

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 2 MWP, 1 SP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage] + [DomainFactory.createMergingWorkPackage()]

        expect:
        PARTLY_DONE_WONT_DO_MORE == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 2 MWP, 1 MWP without SP, 1 MWP MIGHT_DO_MORE, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage] + DomainFactory.createMergingWorkPackage()

        expect:
        NOTHING_DONE_MIGHT_DO == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    void "getSnvProcessingStatus, 2 MWP, 1 SP ALL_DONE, 1 SP MIGHT_DO_MORE, returns PARTLY_DONE_MIGHT_DO_MORE"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        SnvCallingInstance snvCallingInstance2 = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: SnvProcessingStates.FINISHED)

        Set<MergingWorkPackage> mergingWorkPackages = [snvCallingInstance.sampleType1BamFile.mergingWorkPackage] + [snvCallingInstance2.sampleType1BamFile.mergingWorkPackage]

        expect:
        PARTLY_DONE_MIGHT_DO_MORE == trackingService.getSnvProcessingStatus(mergingWorkPackages, false, new SamplePairDiscovery()).snvProcessingStatus
    }

    private static AbstractMergedBamFile createBamFileInProjectFolder(Map bamFileProperties = [:]) {
        AbstractMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(bamFileProperties)

        return setBamFileInProjectFolder(bamFile)
    }

    private static AbstractMergedBamFile setBamFileInProjectFolder (AbstractMergedBamFile bamFile) {
        bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
        bamFile.mergingWorkPackage.save(flush: true)

        return bamFile
    }

    void "assignOtrsTicketToRunSegment, no RunSegment for runSegementId, throws AssertionError "() {
        when:
        trackingService.assignOtrsTicketToRunSegment("", 1)

        then:
        AssertionError error = thrown()
        error.message.contains("No RunSegment found")
    }

    void "assignOtrsTicketToRunSegment, new ticketNumber equals old ticketNumber, returns true"() {
        given:
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345678')
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)

        expect:
        trackingService.assignOtrsTicketToRunSegment(otrsTicket.ticketNumber, runSegment.id)
    }

    void "assignOtrsTicketToRunSegment, new ticketNumber does not pass custom validation, throws UserException"() {
        given:
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345678')
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)

        when:
        trackingService.assignOtrsTicketToRunSegment('abc', runSegment.id)

        then:
        UserException error = thrown()
        error.message.contains("does not pass validation or error while saving.")
    }

    void "assignOtrsTicketToRunSegment, old OtrsTicket consists of several other RunSegements, throws UserException"() {
        given:
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345678')
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)
        DomainFactory.createRunSegment(otrsTicket: otrsTicket)

        when:
        trackingService.assignOtrsTicketToRunSegment('2000010112345679', runSegment.id)

        then:
        UserException error = thrown()
        error.message.contains("Assigning a runSegment that belongs to an OTRS-Ticket which consists of several other runSegments is not allowed.")
    }

    void "assignOtrsTicketToRunSegment, new OtrsTicket final notification already sent, throws UserException"() {
        given:
        OtrsTicket oldOtrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345678')
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: oldOtrsTicket)
        OtrsTicket newOtrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345679', finalNotificationSent: true)

        when:
        trackingService.assignOtrsTicketToRunSegment(newOtrsTicket.ticketNumber, runSegment.id)

        then:
        UserException error = thrown()
        error.message.contains("It is not allowed to assign to an finally notified OTRS-Ticket.")
    }

    void "assignOtrsTicketToRunSegment, adjust ProcessingStatus of new OtrsTicket"() {
        given:
        Date minDate = new Date().minus(1)
        Date maxDate = new Date().plus(1)

        OtrsTicket oldOtrsTicket = DomainFactory.createOtrsTicket(
                ticketNumber: '2000010112345678',
                installationStarted: minDate,
                installationFinished: minDate,
                fastqcStarted: maxDate,
                fastqcFinished: maxDate,
                alignmentStarted: null,
                alignmentFinished: maxDate,
                snvStarted: null,
                snvFinished: null
        )
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: oldOtrsTicket)
        OtrsTicket newOtrsTicket = DomainFactory.createOtrsTicket(
                ticketNumber: '2000010112345679',
                installationStarted: maxDate,
                installationFinished: maxDate,
                fastqcStarted: minDate,
                fastqcFinished: minDate,
                alignmentStarted: minDate,
                alignmentFinished: null,
                snvStarted: null,
                snvFinished: null
        )

        trackingService.metaClass.getProcessingStatus = { Set<SeqTrack> set ->
            return new ProcessingStatus(
                    installationProcessingStatus: ALL_DONE,
                    fastqcProcessingStatus: ALL_DONE,
                    alignmentProcessingStatus: PARTLY_DONE_MIGHT_DO_MORE,
                    snvProcessingStatus: NOTHING_DONE_MIGHT_DO
            )
        }

        when:
        trackingService.assignOtrsTicketToRunSegment(newOtrsTicket.ticketNumber, runSegment.id)

        then:
        minDate == newOtrsTicket.installationStarted
        maxDate == newOtrsTicket.installationFinished
        minDate == newOtrsTicket.fastqcStarted
        maxDate == newOtrsTicket.fastqcFinished
        minDate == newOtrsTicket.alignmentStarted
        null == newOtrsTicket.alignmentFinished
        null == newOtrsTicket.snvStarted
        null == newOtrsTicket.snvFinished
    }
}
