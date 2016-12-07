package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import de.dkfz.tbi.otp.tracking.TrackingService.SamplePairDiscovery
import de.dkfz.tbi.otp.user.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*
import spock.lang.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class TrackingServiceIntegrationSpec extends IntegrationSpec {

    TrackingService trackingService
    MailHelperService mailHelperService
    IndelCallingService indelCallingService
    SnvCallingService snvCallingService
    CreateNotificationTextService createNotificationTextService

    void setup() {
        // Overwrite the autowired service with a new instance for each test, so mocks do not have to be cleaned up
        trackingService = new TrackingService(
                mailHelperService: mailHelperService,
                indelCallingService: indelCallingService,
                snvCallingService: snvCallingService,
                createNotificationTextService: createNotificationTextService,
        )
    }

    void cleanup() {
        TestCase.removeMetaClass(TrackingService, trackingService)
    }

    void 'processFinished calls setFinishedTimestampsAndNotify for the tickets of the passed SeqTracks'() {
        given:
        OtrsTicket ticketA = DomainFactory.createOtrsTicket()
        SeqTrack seqTrackA = DomainFactory.createSeqTrackWithOneDataFile(
                [
                        dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                        fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS,
                ],
                [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketA), fileLinked: true])

        OtrsTicket ticketB = DomainFactory.createOtrsTicket()
        SeqTrack seqTrackB1 = DomainFactory.createSeqTrackWithOneDataFile(
                [
                        dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                        fastqcState: SeqTrack.DataProcessingState.FINISHED,
                ],
                [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketB), fileLinked: true])
        SeqTrack seqTrackB2 = DomainFactory.createSeqTrackWithOneDataFile(
                [
                        dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                        fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS,
                ],
                [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketB), fileLinked: true])

        DomainFactory.createProcessingOptionForOtrsTicketPrefix("the prefix")

        trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
            notification(_, _, _) >> 'Something'
        }
        trackingService.mailHelperService = Mock(MailHelperService) {
            getOtrsRecipient(_) >> 'a.b@c.d'
        }

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
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(
                [
                        dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                        fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS,
                ],
                [runSegment: runSegment, fileLinked: true])
        ProcessingStatus expectedStatus = [
                getInstallationProcessingStatus: { -> ALL_DONE },
                getFastqcProcessingStatus: { -> NOTHING_DONE_MIGHT_DO },
                getAlignmentProcessingStatus: { -> NOTHING_DONE_WONT_DO },
                getSnvProcessingStatus: { -> NOTHING_DONE_WONT_DO },
        ] as ProcessingStatus

        String prefix = "the prefix"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

        String otrsRecipient = HelperUtils.uniqueString
        String notificationText = HelperUtils.uniqueString

        String expectedEmailSubjectOperator = "${prefix}#${ticket.ticketNumber} Processing Status Update"
        String expectedEmailSubjectCustomer = "[${prefix}#${ticket.ticketNumber}] TO BE SENT: ${seqTrack.project.name} sequencing data installed"

        trackingService.mailHelperService = Mock(MailHelperService) {
            getOtrsRecipient() >> otrsRecipient
            1 * sendEmail(expectedEmailSubjectOperator, _, otrsRecipient) >> { String emailSubject, String content, String recipient ->
                assert content.contains(expectedStatus.toString())
            }
            1 * sendEmail(expectedEmailSubjectCustomer, notificationText, [otrsRecipient])
            0 * _
        }

        trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
            1 * notification(ticket, _, OtrsTicket.ProcessingStep.INSTALLATION) >> notificationText
            0 * notification(_, _, _)
        }

        when:
        trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairDiscovery())

        then:
        ticket.installationFinished != null
        ticket.fastqcFinished == null
        ticket.alignmentFinished == null
        ticket.snvFinished == null
        !ticket.finalNotificationSent
    }

    void "setFinishedTimestampsAndNotify, when something just completed and won't do more, sends final notification"() {
        given:
        // Installation: finished timestamp not set, all done,     won't do more
        // FastQC:       finished timestamp not set, all done,     won't do more
        // Alignment:    finished timestamp not set, partly done,  won't do more
        // SNV:          finished timestamp not set, nothing done, won't do more
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithOneDataFile(
                [
                        dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                        fastqcState: SeqTrack.DataProcessingState.FINISHED,
                ],
                [runSegment: runSegment, fileLinked: true])
        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithOneDataFile([
                sample: DomainFactory.createSample(individual: DomainFactory.createIndividual(project: seqTrack1.project)),
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                fastqcState: SeqTrack.DataProcessingState.FINISHED,
                run: DomainFactory.createRun(
                        seqPlatform: DomainFactory.createSeqPlatform(
                                seqPlatformGroup: DomainFactory.createSeqPlatformGroup(),
                        ),
                ),
        ],
                [runSegment: runSegment, fileLinked: true]
        )
        setBamFileInProjectFolder(DomainFactory.createRoddyBamFile(
                DomainFactory.createRoddyBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage(
                                MergingWorkPackage.getMergingProperties(seqTrack2) +
                                        [pipeline: DomainFactory.createPanCanPipeline()]
                        )
                ]),
                DomainFactory.randomProcessedBamFileProperties + [seqTracks: [seqTrack2] as Set],
        ))
        ProcessingStatus expectedStatus = [
                getInstallationProcessingStatus: { -> ALL_DONE },
                getFastqcProcessingStatus: { -> ALL_DONE },
                getAlignmentProcessingStatus: { -> PARTLY_DONE_WONT_DO_MORE },
                getSnvProcessingStatus: { -> NOTHING_DONE_WONT_DO },
        ] as ProcessingStatus

        String prefix = "the prefix"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

        String otrsRecipient = HelperUtils.uniqueString
        String notificationText1 = HelperUtils.uniqueString
        String notificationText2 = HelperUtils.uniqueString

        String expectedEmailSubjectOperator = "${prefix}#${ticket.ticketNumber} Final Processing Status Update"
        String expectedEmailSubjectCustomer = "[${prefix}#${ticket.ticketNumber}] TO BE SENT: ${seqTrack1.project.name} sequencing data "
        String expectedEmailSubjectCustomer1 = expectedEmailSubjectCustomer + OtrsTicket.ProcessingStep.INSTALLATION.notificationSubject
        String expectedEmailSubjectCustomer2 = expectedEmailSubjectCustomer + OtrsTicket.ProcessingStep.ALIGNMENT.notificationSubject

        trackingService.mailHelperService = Mock(MailHelperService) {
            getOtrsRecipient() >> otrsRecipient
            1 * sendEmail(expectedEmailSubjectOperator, _, otrsRecipient) >> { String emailSubject, String content, String recipient ->
                assert content.contains(expectedStatus.toString())
            }
            1 * sendEmail(expectedEmailSubjectCustomer1, notificationText1, [otrsRecipient])
            1 * sendEmail(expectedEmailSubjectCustomer2, notificationText2, [otrsRecipient])
            0 * _
        }

        trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
            1 * notification(ticket, _, OtrsTicket.ProcessingStep.INSTALLATION) >> notificationText1
            1 * notification(ticket, _, OtrsTicket.ProcessingStep.ALIGNMENT) >> notificationText2
            0 * notification(_, _, _)
        }

        when:
        trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairDiscovery())

        then:
        ticket.installationFinished != null
        ticket.fastqcFinished == ticket.installationFinished
        ticket.alignmentFinished == ticket.installationFinished
        ticket.snvFinished == null
        ticket.finalNotificationSent
    }


    private static final String OTRS_RECIPIENT = HelperUtils.uniqueString

    @Unroll
    void 'sendCustomerNotification sends expected notification'(int dataCase, boolean automaticNotification, OtrsTicket.ProcessingStep notificationStep, List<String> recipients, String subject) {
        given:
        Sample sample1 = DomainFactory.createSample(
                individual: DomainFactory.createIndividual(
                        project: DomainFactory.createProject(
                                name: 'Project1',
                                mailingListName: 'tr_list@test.test',
                        )
                )
        )
        Sample sample2 = DomainFactory.createSample(
                individual: DomainFactory.createIndividual(
                        project: DomainFactory.createProject(
                                name: 'Project2',
                                mailingListName: null,
                        )
                )
        )

        Collection<SeqTrack> seqTracks
        switch (dataCase) {
            case 1:
                seqTracks = [
                        DomainFactory.createSeqTrack(sample: sample1),
                ]
                break
            case 2:
                seqTracks = [
                        DomainFactory.createSeqTrack(sample: sample1),
                ]
                break
            case 3:
                seqTracks = [
                        DomainFactory.createSeqTrack(sample: sample1),
                        DomainFactory.createSeqTrack(
                                sample: sample1,
                                ilseSubmission: DomainFactory.createIlseSubmission(ilseNumber: 1234),
                        ),
                ]
                break
            case 4:
                seqTracks = [
                        DomainFactory.createSeqTrack(sample: sample1),
                ]
                break
            case 5:
                seqTracks = [
                        DomainFactory.createSeqTrack(sample: sample2),
                ]
                break
            case 6:
                IlseSubmission ilse = DomainFactory.createIlseSubmission(ilseNumber: 9876)
                seqTracks = [
                        DomainFactory.createSeqTrack(
                                sample: sample2,
                                ilseSubmission: ilse,
                        ),
                        DomainFactory.createSeqTrack(
                                sample: sample2,
                                ilseSubmission: ilse,
                        ),
                        DomainFactory.createSeqTrack(
                                sample: sample2,
                                ilseSubmission: DomainFactory.createIlseSubmission(ilseNumber: 1234),
                        ),
                        DomainFactory.createSeqTrack(sample: sample2),
                ]
        }

        OtrsTicket ticket = DomainFactory.createOtrsTicket(automaticNotification: automaticNotification)
        ProcessingStatus status = new ProcessingStatus(seqTracks.collect { new SeqTrackProcessingStatus(it) })
        String prefix = HelperUtils.uniqueString
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
        subject = "[${prefix}#${ticket.ticketNumber}] ${subject}"
        int callCount = recipients.isEmpty() ? 0 : 1
        String content = HelperUtils.uniqueString

        trackingService.mailHelperService = Mock(MailHelperService) {
            getOtrsRecipient() >> OTRS_RECIPIENT
            callCount * sendEmail(subject, content, recipients)
            0 * sendEmail(_, _, _)
        }
        trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
            callCount * notification(ticket, _, notificationStep) >> { OtrsTicket ticket1, ProcessingStatus status1, OtrsTicket.ProcessingStep processingStep ->
                TestCase.assertContainSame(status.seqTrackProcessingStatuses, status1.seqTrackProcessingStatuses)
                return content
            }
        }

        expect:
        trackingService.sendCustomerNotification(ticket, status, notificationStep)

        where:
        dataCase  | automaticNotification | notificationStep                       | recipients                            | subject
        1         | true                  | OtrsTicket.ProcessingStep.INSTALLATION | ['tr_list@test.test', OTRS_RECIPIENT] | 'Project1 sequencing data installed'
        2         | false                 | OtrsTicket.ProcessingStep.INSTALLATION | [OTRS_RECIPIENT]                      | 'TO BE SENT: Project1 sequencing data installed'
        3         | true                  | OtrsTicket.ProcessingStep.INSTALLATION | ['tr_list@test.test', OTRS_RECIPIENT] | '[S#1234] Project1 sequencing data installed'
        4         | true                  | OtrsTicket.ProcessingStep.FASTQC       | []                                    | null
        5         | true                  | OtrsTicket.ProcessingStep.ALIGNMENT    | [OTRS_RECIPIENT]                      | 'TO BE SENT: Project2 sequencing data aligned'
        6         | true                  | OtrsTicket.ProcessingStep.SNV          | [OTRS_RECIPIENT]                      | 'TO BE SENT: [S#1234,9876] Project2 sequencing data SNV-called'
    }

    void 'sendCustomerNotification, with multiple projects, sends multiple notifications'() {
        given:
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        ProcessingStatus status = new ProcessingStatus([1, 2].collect { int index ->
            new SeqTrackProcessingStatus(DomainFactory.createSeqTrack(
                    sample: DomainFactory.createSample(
                            individual: DomainFactory.createIndividual(
                                    project: DomainFactory.createProject(
                                            name: "Project_X${index}",
                                            mailingListName: "tr_project${index}@test.test",
                                    )
                            )
                    )
            ))
        })
        String prefix = HelperUtils.uniqueString
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

        trackingService.mailHelperService = Mock(MailHelperService) {
            getOtrsRecipient() >> OTRS_RECIPIENT
            1 * sendEmail("[${prefix}#${ticket.ticketNumber}] Project_X1 sequencing data installed",
                    'Project_X1', ['tr_project1@test.test', OTRS_RECIPIENT])
            1 * sendEmail("[${prefix}#${ticket.ticketNumber}] Project_X2 sequencing data installed",
                    'Project_X2', ['tr_project2@test.test', OTRS_RECIPIENT])
            0 * sendEmail(_, _, _)
        }
        trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
            notification(ticket, _, OtrsTicket.ProcessingStep.INSTALLATION) >> { OtrsTicket ticket1, ProcessingStatus status1, OtrsTicket.ProcessingStep processingStep ->
                return exactlyOneElement(status1.seqTrackProcessingStatuses*.seqTrack*.project.unique()).name
            }
        }

        expect:
        trackingService.sendCustomerNotification(ticket, status, OtrsTicket.ProcessingStep.INSTALLATION)
    }


    private static SeqTrackProcessingStatus createSeqTrackProcessingStatus(SeqTrack seqTrack) {
        return new SeqTrackProcessingStatus(seqTrack, ALL_DONE, ALL_DONE, [])
    }

    private static ProcessingStatus createProcessingStatus(SeqTrackProcessingStatus... seqTrackStatuses) {
        return new ProcessingStatus(Arrays.asList(seqTrackStatuses))
    }

    void "fillInMergingWorkPackageProcessingStatuses, no ST, does not crash"() {
        expect:
        trackingService.fillInMergingWorkPackageProcessingStatuses([], new SamplePairDiscovery())
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 ST not alignable, returns NOTHING_DONE_WONT_DO"() {
        given:
        SeqTrackProcessingStatus seqTrackStatus = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithOneDataFile([:], [fileWithdrawn: true]))
        ProcessingStatus processingStatus = createProcessingStatus(seqTrackStatus)

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus], new SamplePairDiscovery())

        then:
        seqTrackStatus.mergingWorkPackageProcessingStatuses.isEmpty()
        seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
        seqTrackStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
        processingStatus.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
        processingStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 MWP ALL_DONE, returns ALL_DONE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
        SeqTrackProcessingStatus seqTrackStatus = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus], new SamplePairDiscovery())

        then:
        MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
        mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
        mwpStatus.completeProcessableBamFileInProjectFolder == bamFile
        mwpStatus.alignmentProcessingStatus == ALL_DONE
        seqTrackStatus.alignmentProcessingStatus == ALL_DONE
        createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == ALL_DONE
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 MWP NOTHING_DONE_MIGHT_DO, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
        SeqTrackProcessingStatus seqTrackStatus = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithDataFiles(bamFile.mergingWorkPackage))

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus], new SamplePairDiscovery())

        then:
        MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
        mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
        mwpStatus.completeProcessableBamFileInProjectFolder == null
        mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 ST not alignable, rest merged, returns NOTHING_DONE_WONT_DO_MORE and ALL_DONE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
        SeqTrackProcessingStatus seqTrack1Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithDataFiles(bamFile.mergingWorkPackage, [:], [fileWithdrawn: true]))
        SeqTrackProcessingStatus seqTrack2Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status], new SamplePairDiscovery())

        then:
        seqTrack1Status.mergingWorkPackageProcessingStatuses.isEmpty()
        seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO

        MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack2Status.mergingWorkPackageProcessingStatuses)
        mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
        mwpStatus.completeProcessableBamFileInProjectFolder == bamFile
        mwpStatus.alignmentProcessingStatus == ALL_DONE
        seqTrack2Status.alignmentProcessingStatus == ALL_DONE

        createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_WONT_DO_MORE
    }

    void "fillInMergingWorkPackageProcessingStatuses, no MWP, returns NOTHING_DONE_WONT_DO"() {
        given:
        SeqTrackProcessingStatus seqTrackStatus = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithOneDataFile())
        ProcessingStatus processingStatus = createProcessingStatus(seqTrackStatus)

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus], new SamplePairDiscovery())

        then:
        seqTrackStatus.mergingWorkPackageProcessingStatuses.isEmpty()
        seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
        seqTrackStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
        processingStatus.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
        processingStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()
        SeqTrackProcessingStatus seqTrackStatus = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus], new SamplePairDiscovery())

        then:
        MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
        mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
        mwpStatus.completeProcessableBamFileInProjectFolder == null
        mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
        SeqTrackProcessingStatus seqTrack1Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
        SeqTrackProcessingStatus seqTrack2Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithOneDataFile())

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status], new SamplePairDiscovery())

        then:
        MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
        mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
        mwpStatus.completeProcessableBamFileInProjectFolder == bamFile
        mwpStatus.alignmentProcessingStatus == ALL_DONE
        seqTrack1Status.alignmentProcessingStatus == ALL_DONE

        seqTrack2Status.mergingWorkPackageProcessingStatuses.isEmpty()
        seqTrack2Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO

        createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_WONT_DO_MORE
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()
        SeqTrackProcessingStatus seqTrack1Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
        SeqTrackProcessingStatus seqTrack2Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithOneDataFile())

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status], new SamplePairDiscovery())

        then:
        MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
        mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
        mwpStatus.completeProcessableBamFileInProjectFolder == null
        mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO

        seqTrack2Status.mergingWorkPackageProcessingStatuses.isEmpty()
        seqTrack2Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO

        createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP NOTHING_DONE_MIGHT_DO, 1 MWP ALL_DONE, returns PARTLY_DONE_MIGHT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile1 = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
        AbstractMergedBamFile bamFile2 = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
        SeqTrackProcessingStatus seqTrack1Status = createSeqTrackProcessingStatus(bamFile1.containedSeqTracks.first())
        SeqTrackProcessingStatus seqTrack2Status = createSeqTrackProcessingStatus(bamFile2.containedSeqTracks.first())
        SeqTrackProcessingStatus seqTrack3Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithDataFiles(bamFile1.mergingWorkPackage))

        when:
        trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status, seqTrack3Status], new SamplePairDiscovery())

        then:
        MergingWorkPackageProcessingStatus mwp1Status = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
        mwp1Status.mergingWorkPackage == bamFile1.mergingWorkPackage
        mwp1Status.completeProcessableBamFileInProjectFolder == null
        mwp1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO

        MergingWorkPackageProcessingStatus mwp2Status = exactlyOneElement(seqTrack2Status.mergingWorkPackageProcessingStatuses)
        mwp2Status.mergingWorkPackage == bamFile2.mergingWorkPackage
        mwp2Status.completeProcessableBamFileInProjectFolder == bamFile2
        mwp2Status.alignmentProcessingStatus == ALL_DONE
        seqTrack2Status.alignmentProcessingStatus == ALL_DONE

        exactlyOneElement(seqTrack3Status.mergingWorkPackageProcessingStatuses).is(mwp1Status)

        createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_MIGHT_DO_MORE
    }

    private static MergingWorkPackageProcessingStatus createMergingWorkPackageProcessingStatus(
            MergingWorkPackage mergingWorkPackage, WorkflowProcessingStatus processingStatus) {
        return new MergingWorkPackageProcessingStatus(mergingWorkPackage, processingStatus, null, [])
    }

    private static MergingWorkPackageProcessingStatus createMergingWorkPackageProcessingStatus(AbstractMergedBamFile bamFile) {
        return new MergingWorkPackageProcessingStatus(bamFile.mergingWorkPackage, ALL_DONE, bamFile, [])
    }

    private static SeqTrackProcessingStatus createSeqTrackProcessingStatus(MergingWorkPackageProcessingStatus... mwpStatuses) {
        // seqTrack actually must not be null, but for this test it does not matter
        return new SeqTrackProcessingStatus(null, ALL_DONE, ALL_DONE, Arrays.asList(mwpStatuses))
    }

    void "fillInSamplePairStatuses, no MWP, does not crash"() {
        expect:
        trackingService.fillInSamplePairStatuses([], new SamplePairDiscovery())
    }

    void "fillInSamplePairStatuses, no SP, returns NOTHING_DONE_WONT_DO"() {
        given:
        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        mwpStatus.samplePairProcessingStatuses.isEmpty()
        mwpStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
        mwpStatus.indelProcessingStatus == NOTHING_DONE_WONT_DO
        createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInSamplePairStatuses, SCI withdrawn, returns NOTHING_DONE_WONT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile)

        when:
        snvCallingInstance.withdrawn = true
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        assert snvCallingInstance.withdrawn
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.completeSnvCallingInstance == null
        mwpStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
        createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInSamplePairStatuses, 1 SCI FINISHED, bamFileInProjectFolder set, returns ALL_DONE"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == snvCallingInstance.samplePair
        samplePairStatus.completeSnvCallingInstance == snvCallingInstance
        samplePairStatus.snvProcessingStatus == ALL_DONE
        mwpStatus.snvProcessingStatus == ALL_DONE
        createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == ALL_DONE
    }

    void "fillInSamplePairStatuses, 1 SCI not FINISHED, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == snvCallingInstance.samplePair
        samplePairStatus.completeSnvCallingInstance == null
        samplePairStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwpStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, 1 SCI FINISHED, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile.mergingWorkPackage, NOTHING_DONE_MIGHT_DO)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == snvCallingInstance.samplePair
        samplePairStatus.completeSnvCallingInstance == null
        samplePairStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwpStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, no SCI, bamFileInProjectFolder set, no samplePairForProcessing, returns NOTHING_DONE_WONT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile)

        snvCallingInstance.delete(flush: true)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == snvCallingInstance.samplePair
        samplePairStatus.completeSnvCallingInstance == null
        samplePairStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
        mwpStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
        createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInSamplePairStatuses, no SCI, bamFileInProjectFolder set, samplePairForProcessing exists, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles([:], [coverage: 2], [coverage: 2])

        DomainFactory.createSnvConfigForSnvCallingInstance(snvCallingInstance)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
            DomainFactory.createProcessingThresholdsForBamFile(snvCallingInstance."sampleType${it}BamFile", [coverage: 1, numberOfLanes: null])
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile)

        snvCallingInstance.delete(flush: true)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == snvCallingInstance.samplePair
        samplePairStatus.completeSnvCallingInstance == null
        samplePairStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwpStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, no SCI, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile.mergingWorkPackage, NOTHING_DONE_MIGHT_DO)

        snvCallingInstance.delete(flush: true)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == snvCallingInstance.samplePair
        samplePairStatus.completeSnvCallingInstance == null
        samplePairStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwpStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, with SNV 2 MWP, 1 SP ALL_DONE, 1 MWP without SP, returns ALL_DONE and NOTHING_DONE_WONT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwp1Status = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile)
        MergingWorkPackageProcessingStatus mwp2Status = createMergingWorkPackageProcessingStatus(
                DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)

        when:
        trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwp1Status.samplePairProcessingStatuses)
        samplePairStatus.samplePair == snvCallingInstance.samplePair
        samplePairStatus.completeSnvCallingInstance == snvCallingInstance
        samplePairStatus.snvProcessingStatus == ALL_DONE
        mwp1Status.snvProcessingStatus == ALL_DONE

        mwp2Status.samplePairProcessingStatuses.isEmpty()
        mwp2Status.snvProcessingStatus == NOTHING_DONE_WONT_DO

        createSeqTrackProcessingStatus(mwp1Status, mwp2Status).snvProcessingStatus == PARTLY_DONE_WONT_DO_MORE
    }

    void "fillInSamplePairStatuses, with SNV 2 MWP, 1 MWP without SP, 1 MWP MIGHT_DO_MORE, returns NOTHING_DONE_WONT_DO and NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        MergingWorkPackageProcessingStatus mwp1Status = createMergingWorkPackageProcessingStatus(
                DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)
        MergingWorkPackageProcessingStatus mwp2Status = createMergingWorkPackageProcessingStatus(
                snvCallingInstance.sampleType1BamFile)

        when:
        trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairDiscovery())

        then:
        mwp1Status.samplePairProcessingStatuses.isEmpty()
        mwp1Status.snvProcessingStatus == NOTHING_DONE_WONT_DO

        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwp2Status.samplePairProcessingStatuses)
        samplePairStatus.samplePair == snvCallingInstance.samplePair
        samplePairStatus.completeSnvCallingInstance == null
        samplePairStatus.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwp2Status.snvProcessingStatus == NOTHING_DONE_MIGHT_DO

        createSeqTrackProcessingStatus(mwp1Status, mwp2Status).snvProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, with SNV 2 MWP, 1 SP ALL_DONE, 1 SP MIGHT_DO_MORE, returns ALL_DONE and NOTHING_DONE_MIGHT_DO"() {
        given:
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(snvCallingInstance."sampleType${it}BamFile")
        }

        SnvCallingInstance snvCallingInstance2 = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        MergingWorkPackageProcessingStatus mwp1Status =
                createMergingWorkPackageProcessingStatus(snvCallingInstance.sampleType1BamFile)
        MergingWorkPackageProcessingStatus mwp2Status =
                createMergingWorkPackageProcessingStatus(snvCallingInstance2.sampleType1BamFile)

        when:
        trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePair1Status = exactlyOneElement(mwp1Status.samplePairProcessingStatuses)
        samplePair1Status.samplePair == snvCallingInstance.samplePair
        samplePair1Status.completeSnvCallingInstance == snvCallingInstance
        samplePair1Status.snvProcessingStatus == ALL_DONE
        mwp1Status.snvProcessingStatus == ALL_DONE

        SamplePairProcessingStatus samplePair2Status = exactlyOneElement(mwp2Status.samplePairProcessingStatuses)
        samplePair2Status.samplePair == snvCallingInstance2.samplePair
        samplePair2Status.completeSnvCallingInstance == null
        samplePair2Status.snvProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwp2Status.snvProcessingStatus == NOTHING_DONE_MIGHT_DO

        createSeqTrackProcessingStatus(mwp1Status, mwp2Status).snvProcessingStatus == PARTLY_DONE_MIGHT_DO_MORE
    }

    void "fillInSamplePairStatuses, ICI withdrawn, returns NOTHING_DONE_WONT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(indelCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile)

        when:
        indelCallingInstance.withdrawn = true
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        assert indelCallingInstance.withdrawn
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.completeIndelCallingInstance == null
        mwpStatus.indelProcessingStatus == NOTHING_DONE_WONT_DO
        createSeqTrackProcessingStatus(mwpStatus).indelProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInSamplePairStatuses, 1 ICI FINISHED, bamFileInProjectFolder set, returns ALL_DONE"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(indelCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == indelCallingInstance.samplePair
        samplePairStatus.completeIndelCallingInstance == indelCallingInstance
        samplePairStatus.indelProcessingStatus == ALL_DONE
        mwpStatus.indelProcessingStatus == ALL_DONE
        createSeqTrackProcessingStatus(mwpStatus).indelProcessingStatus == ALL_DONE
    }

    void "fillInSamplePairStatuses, 1 ICI not FINISHED, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        [1, 2].each {
            setBamFileInProjectFolder(indelCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == indelCallingInstance.samplePair
        samplePairStatus.completeIndelCallingInstance == null
        samplePairStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwpStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        createSeqTrackProcessingStatus(mwpStatus).indelProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, 1 ICI FINISHED, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile.mergingWorkPackage, NOTHING_DONE_MIGHT_DO)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == indelCallingInstance.samplePair
        samplePairStatus.completeIndelCallingInstance == null
        samplePairStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwpStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        createSeqTrackProcessingStatus(mwpStatus).indelProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, no ICI, bamFileInProjectFolder set, no samplePairForProcessing, returns NOTHING_DONE_WONT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        [1, 2].each {
            setBamFileInProjectFolder(indelCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile)

        indelCallingInstance.delete(flush: true)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == indelCallingInstance.samplePair
        samplePairStatus.completeIndelCallingInstance == null
        samplePairStatus.indelProcessingStatus == NOTHING_DONE_WONT_DO
        mwpStatus.indelProcessingStatus == NOTHING_DONE_WONT_DO
        createSeqTrackProcessingStatus(mwpStatus).indelProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInSamplePairStatuses, no ICI, bamFileInProjectFolder set, samplePairForProcessing exists, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles([:], [coverage: 2], [coverage: 2])

        DomainFactory.createRoddyWorkflowConfig(
                seqType: indelCallingInstance.seqType,
                project: indelCallingInstance.project,
                pipeline: DomainFactory.createIndelPipelineLazy()
        )

        [1, 2].each {
            setBamFileInProjectFolder(indelCallingInstance."sampleType${it}BamFile")
            DomainFactory.createProcessingThresholdsForBamFile(indelCallingInstance."sampleType${it}BamFile", [coverage: 1, numberOfLanes: null])
        }

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile)

        indelCallingInstance.delete(flush: true)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == indelCallingInstance.samplePair
        samplePairStatus.completeIndelCallingInstance == null
        samplePairStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwpStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        createSeqTrackProcessingStatus(mwpStatus).indelProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, no ICI, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        MergingWorkPackageProcessingStatus mwpStatus = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile.mergingWorkPackage, NOTHING_DONE_MIGHT_DO)

        indelCallingInstance.delete(flush: true)

        when:
        trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
        samplePairStatus.samplePair == indelCallingInstance.samplePair
        samplePairStatus.completeIndelCallingInstance == null
        samplePairStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwpStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        createSeqTrackProcessingStatus(mwpStatus).indelProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, with Indel 2 MWP, 1 SP ALL_DONE, 1 MWP without SP, returns ALL_DONE and NOTHING_DONE_WONT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(indelCallingInstance."sampleType${it}BamFile")
        }

        MergingWorkPackageProcessingStatus mwp1Status = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile)
        MergingWorkPackageProcessingStatus mwp2Status = createMergingWorkPackageProcessingStatus(
                DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)

        when:
        trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwp1Status.samplePairProcessingStatuses)
        samplePairStatus.samplePair == indelCallingInstance.samplePair
        samplePairStatus.completeIndelCallingInstance == indelCallingInstance
        samplePairStatus.indelProcessingStatus == ALL_DONE
        mwp1Status.indelProcessingStatus == ALL_DONE

        mwp2Status.samplePairProcessingStatuses.isEmpty()
        mwp2Status.indelProcessingStatus == NOTHING_DONE_WONT_DO

        createSeqTrackProcessingStatus(mwp1Status, mwp2Status).indelProcessingStatus == PARTLY_DONE_WONT_DO_MORE
    }

    void "fillInSamplePairStatuses, with Indel 2 MWP, 1 MWP without SP, 1 MWP MIGHT_DO_MORE, returns NOTHING_DONE_WONT_DO and NOTHING_DONE_MIGHT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        MergingWorkPackageProcessingStatus mwp1Status = createMergingWorkPackageProcessingStatus(
                DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)
        MergingWorkPackageProcessingStatus mwp2Status = createMergingWorkPackageProcessingStatus(
                indelCallingInstance.sampleType1BamFile)

        when:
        trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairDiscovery())

        then:
        mwp1Status.samplePairProcessingStatuses.isEmpty()
        mwp1Status.indelProcessingStatus == NOTHING_DONE_WONT_DO

        SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwp2Status.samplePairProcessingStatuses)
        samplePairStatus.samplePair == indelCallingInstance.samplePair
        samplePairStatus.completeIndelCallingInstance == null
        samplePairStatus.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwp2Status.indelProcessingStatus == NOTHING_DONE_MIGHT_DO

        createSeqTrackProcessingStatus(mwp1Status, mwp2Status).indelProcessingStatus == NOTHING_DONE_MIGHT_DO
    }

    void "fillInSamplePairStatuses, with Indel 2 MWP, 1 SP ALL_DONE, 1 SP MIGHT_DO_MORE, returns ALL_DONE and NOTHING_DONE_MIGHT_DO"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        [1, 2].each {
            setBamFileInProjectFolder(indelCallingInstance."sampleType${it}BamFile")
        }

        IndelCallingInstance indelCallingInstance2 = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        MergingWorkPackageProcessingStatus mwp1Status =
                createMergingWorkPackageProcessingStatus(indelCallingInstance.sampleType1BamFile)
        MergingWorkPackageProcessingStatus mwp2Status =
                createMergingWorkPackageProcessingStatus(indelCallingInstance2.sampleType1BamFile)

        when:
        trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairDiscovery())

        then:
        SamplePairProcessingStatus samplePair1Status = exactlyOneElement(mwp1Status.samplePairProcessingStatuses)
        samplePair1Status.samplePair == indelCallingInstance.samplePair
        samplePair1Status.completeIndelCallingInstance == indelCallingInstance
        samplePair1Status.indelProcessingStatus == ALL_DONE
        mwp1Status.indelProcessingStatus == ALL_DONE

        SamplePairProcessingStatus samplePair2Status = exactlyOneElement(mwp2Status.samplePairProcessingStatuses)
        samplePair2Status.samplePair == indelCallingInstance2.samplePair
        samplePair2Status.completeIndelCallingInstance == null
        samplePair2Status.indelProcessingStatus == NOTHING_DONE_MIGHT_DO
        mwp2Status.indelProcessingStatus == NOTHING_DONE_MIGHT_DO

        createSeqTrackProcessingStatus(mwp1Status, mwp2Status).indelProcessingStatus == PARTLY_DONE_MIGHT_DO_MORE
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
            return [
                    getInstallationProcessingStatus: { -> ALL_DONE },
                    getFastqcProcessingStatus: { -> ALL_DONE },
                    getAlignmentProcessingStatus: { -> PARTLY_DONE_MIGHT_DO_MORE },
                    getSnvProcessingStatus: { -> NOTHING_DONE_MIGHT_DO },
            ] as ProcessingStatus
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
