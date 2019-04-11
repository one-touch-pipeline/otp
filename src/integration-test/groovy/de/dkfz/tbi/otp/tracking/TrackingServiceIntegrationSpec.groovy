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

import grails.testing.mixin.integration.Integration
import groovy.sql.Sql
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import de.dkfz.tbi.otp.tracking.TrackingService.SamplePairCreation
import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.*

import javax.sql.DataSource

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Integration
class TrackingServiceIntegrationSpec extends Specification {

    TrackingService trackingService
    MailHelperService mailHelperService
    IndelCallingService indelCallingService
    SnvCallingService snvCallingService
    SophiaService sophiaService
    AceseqService aceseqService
    RunYapsaService runYapsaService
    CreateNotificationTextService createNotificationTextService
    ProcessingOptionService processingOptionService
    UserProjectRoleService UserProjectRoleService

    List<ProcessingOption> referenceGenomeProcessingOptions

    DataSource dataSource
    File schemaDump
    Sql sql

    final static String EMAIL = HelperUtils.getRandomEmail()
    final static String PREFIX = "the prefix"

    static List listPairAnalysis = [
            [
                    analysisType           : OtrsTicket.ProcessingStep.INDEL,
                    createRoddyBamFile     : "createIndelCallingInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeIndelCallingInstance",
                    processingStatus       : "indelProcessingStatus",
            ], [
                    analysisType           : OtrsTicket.ProcessingStep.SNV,
                    createRoddyBamFile     : "createRoddySnvInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeSnvCallingInstance",
                    processingStatus       : "snvProcessingStatus",
            ], [
                    analysisType           : OtrsTicket.ProcessingStep.SOPHIA,
                    createRoddyBamFile     : "createSophiaInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeSophiaInstance",
                    processingStatus       : "sophiaProcessingStatus",
            ], [
                    analysisType           : OtrsTicket.ProcessingStep.ACESEQ,
                    createRoddyBamFile     : "createAceseqInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeAceseqInstance",
                    processingStatus       : "aceseqProcessingStatus",
            ], [
                    analysisType           : OtrsTicket.ProcessingStep.RUN_YAPSA,
                    createRoddyBamFile     : "createRunYapsaInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeRunYapsaInstance",
                    processingStatus       : "runYapsaProcessingStatus",
            ],
    ]*.asImmutable().asImmutable()

    void setup() {
        sql = new Sql(dataSource)
        schemaDump = new File(TestCase.createEmptyTestDirectory(), "test-database-dump.sql")
        sql.execute("SCRIPT NODATA DROP TO ?", [schemaDump.absolutePath])
    }

    void setupData() {
        // Overwrite the autowired service with a new instance for each test, so mocks do not have to be cleaned up
        trackingService = new TrackingService(
                mailHelperService: mailHelperService,
                indelCallingService: indelCallingService,
                snvCallingService: snvCallingService,
                sophiaService: sophiaService,
                aceseqService: aceseqService,
                runYapsaService: runYapsaService,
                createNotificationTextService: createNotificationTextService,
                processingOptionService: processingOptionService,
                userProjectRoleService: userProjectRoleService,
        )
        SessionUtils.withNewSession {
            DomainFactory.createAllAnalysableSeqTypes()

            referenceGenomeProcessingOptions = DomainFactory.createReferenceGenomeAndAnalysisProcessingOptions()
        }
    }

    void cleanup() {
        TestCase.removeMetaClass(TrackingService, trackingService)
        if (sql) {
            sql.execute("DROP ALL OBJECTS")
            sql.execute("RUNSCRIPT FROM ?", [schemaDump.absolutePath])
        }
    }

    void "check that all analyses are provided in the list 'listPairAnalysis'"() {
        given:
        List<OtrsTicket.ProcessingStep> analysisProcessingSteps
        List<OtrsTicket.ProcessingStep> testedProcessingSteps
        setupData()
        SessionUtils.withNewSession {

            analysisProcessingSteps = OtrsTicket.ProcessingStep.values() - [
                    OtrsTicket.ProcessingStep.INSTALLATION,
                    OtrsTicket.ProcessingStep.FASTQC,
                    OtrsTicket.ProcessingStep.ALIGNMENT,
            ]

            testedProcessingSteps = listPairAnalysis*.analysisType
        }


        expect:
        TestCase.assertContainSame(analysisProcessingSteps, testedProcessingSteps)
    }

    def 'test findAllOtrsTickets'() {
        given: "a handfull of tickets and linked and unlinked seqtracks"
        TrackingService trackingService = new TrackingService()
        OtrsTicket otrsTicket01, otrsTicket02, otrsTicket03
        SeqTrack seqTrack1A, seqTrack1B, seqTrack02, seqTrack03, seqTrackOrphanNoDatafile, seqTrackOrphanWithDatafile
        Set<OtrsTicket> actualBatch, actualSingle, actualOrphanNoDatafile, actualOrphanWithDatafile

        setupData()
        SessionUtils.withNewSession {
            // one ticket, with two seqtracks
            otrsTicket01 = DomainFactory.createOtrsTicket()
            seqTrack1A = DomainFactory.createSeqTrack()
            DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket01), seqTrack: seqTrack1A)
            seqTrack1B = DomainFactory.createSeqTrack()
            DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket01), seqTrack: seqTrack1B)
            // one ticket, one seqtrack
            otrsTicket02 = DomainFactory.createOtrsTicket()
            seqTrack02 = DomainFactory.createSeqTrack()
            DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket02), seqTrack: seqTrack02)
            // another ticket, again one seqtrack
            otrsTicket03 = DomainFactory.createOtrsTicket()
            seqTrack03 = DomainFactory.createSeqTrack()
            DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket03), seqTrack: seqTrack03)
            // an orphaned seqtrack, no ticket, no datafile
            seqTrackOrphanNoDatafile = DomainFactory.createSeqTrack()
            // an orphaned seqtrack, no ticket, but with a datafile
            seqTrackOrphanWithDatafile = DomainFactory.createSeqTrack()
            DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(), seqTrack: seqTrackOrphanWithDatafile)
        }

        when: "looking for seqtrack batches, find all (unique) tickets"
        SessionUtils.withNewSession {
            actualBatch = trackingService.findAllOtrsTickets([seqTrack1A, seqTrack02, seqTrack1B])
        }
        then:
        TestCase.assertContainSame(actualBatch, [otrsTicket01, otrsTicket02])

        when: "looking for a single seqtrack, find its ticket"
        SessionUtils.withNewSession {
            actualSingle = trackingService.findAllOtrsTickets([seqTrack03])
        }
        then:
        TestCase.assertContainSame(actualSingle, [otrsTicket03])

        when: "looking for orphans without datafiles, find nothing"
        SessionUtils.withNewSession {
            actualOrphanNoDatafile = trackingService.findAllOtrsTickets([seqTrackOrphanNoDatafile])
        }
        then:
        TestCase.assertContainSame(actualOrphanNoDatafile, [])

        when: "looking for orphans with datafiles, find nothing"
        SessionUtils.withNewSession {
            actualOrphanWithDatafile = trackingService.findAllOtrsTickets([seqTrackOrphanWithDatafile])
        }
        then:
        TestCase.assertContainSame(actualOrphanWithDatafile, [])
    }

    void 'processFinished calls setFinishedTimestampsAndNotify for the tickets of the passed SeqTracks'() {
        given: "tickets with (at least one) fastQC still in progress"
        OtrsTicket ticketA, ticketB
        SeqTrack seqTrackA, seqTrackB
        setupData()
        SessionUtils.withNewSession {
            ticketA = DomainFactory.createOtrsTicket()
            seqTrackA = DomainFactory.createSeqTrackWithOneDataFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.IN_PROGRESS,
                    ],
                    [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketA), fileLinked: true])

            ticketB = DomainFactory.createOtrsTicket()
            seqTrackB = DomainFactory.createSeqTrackWithOneDataFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                    ],
                    [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketB), fileLinked: true])
            DomainFactory.createSeqTrackWithOneDataFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.IN_PROGRESS,
                    ],
                    [runSegment: DomainFactory.createRunSegment(otrsTicket: ticketB), fileLinked: true])

            DomainFactory.createProcessingOptionForOtrsTicketPrefix("the prefix")

            trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
                notification(_, _, _, _) >> 'Something'
            }
            DomainFactory.createProcessingOptionForNotificationRecipient()
        }

        when: "running our tracking update"
        SessionUtils.withNewSession {
            trackingService.processFinished([seqTrackA, seqTrackB] as Set)
            ticketA = OtrsTicket.get(ticketA.id)
            ticketB = OtrsTicket.get(ticketA.id)
        }

        then: "installation should be marked as done, but fastQC as still running"
        ticketA.installationFinished != null
        ticketB.installationFinished != null
        ticketB.fastqcFinished == null
    }

    void 'setFinishedTimestampsAndNotify, when final notification has already been sent, does nothing'() {
        given:
        OtrsTicket ticket
        Date installationFinished = new Date()
        setupData()
        SessionUtils.withNewSession {
            // Installation: finished timestamp set,     all done,     won't do more
            // FastQC:       finished timestamp not set, all done,     won't do more
            // Alignment:    finished timestamp not set, nothing done, won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = DomainFactory.createOtrsTicket(
                    installationFinished: installationFinished,
                    finalNotificationSent: true,
            )
            RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
            DomainFactory.createSeqTrackWithOneDataFile(
                    [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                    [runSegment: runSegment, fileLinked: true])

            trackingService.mailHelperService = Mock(MailHelperService) {
                0 * _
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairCreation())
            //ticket = OtrsTicket.get(ticket.id)
        }

        then:
        ticket.installationFinished == installationFinished
        ticket.fastqcFinished == null
        ticket.alignmentFinished == null
        ticket.snvFinished == null
        ticket.finalNotificationSent
    }

    void 'setFinishedTimestampsAndNotify, when nothing just completed, does nothing'() {
        given:
        OtrsTicket ticket
        Date installationFinished = new Date()
        setupData()
        SessionUtils.withNewSession {
            // Installation: finished timestamp set,     all done,     won't do more
            // FastQC:       finished timestamp not set, partly done,  might do more
            // Alignment:    finished timestamp not set, nothing done, won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = DomainFactory.createOtrsTicket(
                    installationFinished: installationFinished,
            )
            RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
            DomainFactory.createSeqTrackWithOneDataFile(
                    [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                    [runSegment: runSegment, fileLinked: true])
            DomainFactory.createSeqTrackWithOneDataFile(
                    [fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS],
                    [runSegment: runSegment, fileLinked: true])

            trackingService.mailHelperService = Mock(MailHelperService) {
                0 * _
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairCreation())
        }

        then:
        ticket.installationFinished == installationFinished
        ticket.fastqcFinished == null
        ticket.alignmentFinished == null
        ticket.snvFinished == null
        !ticket.finalNotificationSent
    }

    void 'setFinishedTimestampsAndNotify, when something just completed and might do more, sends normal notification'() {
        given:
        OtrsTicket ticket
        setupData()
        SessionUtils.withNewSession {
            // Installation: finished timestamp not set, all done,     won't do more
            // FastQC:       finished timestamp not set, nothing done, might do more
            // Alignment:    finished timestamp not set, nothing done, won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = DomainFactory.createOtrsTicket()
            RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
            SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.IN_PROGRESS,
                    ],
                    [runSegment: runSegment, fileLinked: true])
            ProcessingStatus expectedStatus = [
                    getInstallationProcessingStatus: { -> ALL_DONE },
                    getFastqcProcessingStatus      : { -> NOTHING_DONE_MIGHT_DO },
                    getAlignmentProcessingStatus   : { -> NOTHING_DONE_WONT_DO },
                    getSnvProcessingStatus         : { -> NOTHING_DONE_WONT_DO },
            ] as ProcessingStatus

            String prefix = "the prefix"
            DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

            String otrsRecipient = HelperUtils.randomEmail
            String notificationText = HelperUtils.uniqueString
            DomainFactory.createProcessingOptionForNotificationRecipient(otrsRecipient)

            String expectedEmailSubjectOperator = "${prefix}#${ticket.ticketNumber} Processing Status Update"
            String expectedEmailSubjectCustomer = "[${prefix}#${ticket.ticketNumber}] TO BE SENT: ${seqTrack.project.name} sequencing data installed"

            trackingService.mailHelperService = Mock(MailHelperService) {
                1 * sendEmail(expectedEmailSubjectOperator, _, [otrsRecipient]) >> { String emailSubject, String content, List<String> recipient ->
                    assert content.contains(expectedStatus.toString())
                }
                1 * sendEmail(expectedEmailSubjectCustomer, notificationText, [otrsRecipient])
                0 * _
            }

            trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
                1 * notification(ticket, _, OtrsTicket.ProcessingStep.INSTALLATION, seqTrack.project) >> notificationText
                0 * notification(_, _, _, _)
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairCreation())
            ticket = OtrsTicket.get(ticket.id)
        }

        then:
        ticket.installationFinished != null
        ticket.fastqcFinished == null
        ticket.alignmentFinished == null
        ticket.snvFinished == null
        !ticket.finalNotificationSent
    }

    void "setFinishedTimestampsAndNotify, when something just completed and won't do more, sends final notification"() {
        given:
        OtrsTicket ticket
        setupData()
        SessionUtils.withNewSession {
            // Installation: finished timestamp not set, all done,     won't do more
            // FastQC:       finished timestamp not set, all done,     won't do more
            // Alignment:    finished timestamp not set, partly done,  won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = DomainFactory.createOtrsTicket()
            RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
            SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithOneDataFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                    ],
                    [runSegment: runSegment, fileLinked: true])
            SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithOneDataFile([
                    sample               : DomainFactory.createSample(individual: DomainFactory.createIndividual(project: seqTrack1.project)),
                    dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                    fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                    run                  : DomainFactory.createRun(
                            seqPlatform: DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                                    seqPlatformGroups: [DomainFactory.createSeqPlatformGroup()],
                            ),
                    ),
            ],
                    [runSegment: runSegment, fileLinked: true]
            )
            DomainFactory.createMergingCriteriaLazy(project: seqTrack1.project, seqType: seqTrack1.seqType)
            DomainFactory.createMergingCriteriaLazy(project: seqTrack2.project, seqType: seqTrack2.seqType)
            AbstractMergedBamFile abstractMergedBamFile = setBamFileInProjectFolder(DomainFactory.createRoddyBamFile(
                    DomainFactory.createRoddyBamFile([
                            workPackage: DomainFactory.createMergingWorkPackage(
                                    MergingWorkPackage.getMergingProperties(seqTrack2) +
                                            [pipeline: DomainFactory.createPanCanPipeline()]
                            )
                    ]),
                    DomainFactory.randomProcessedBamFileProperties + [seqTracks: [seqTrack2] as Set],
            ))
            ((MergingWorkPackage) (abstractMergedBamFile.workPackage)).seqTracks.add(seqTrack2)
            abstractMergedBamFile.workPackage.save(flush: true)
            ProcessingStatus expectedStatus = [
                    getInstallationProcessingStatus: { -> ALL_DONE },
                    getFastqcProcessingStatus      : { -> ALL_DONE },
                    getAlignmentProcessingStatus   : { -> PARTLY_DONE_WONT_DO_MORE },
                    getSnvProcessingStatus         : { -> NOTHING_DONE_WONT_DO },
            ] as ProcessingStatus

            String prefix = "the prefix"
            DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

            String otrsRecipient = HelperUtils.randomEmail
            String notificationText1 = HelperUtils.uniqueString
            String notificationText2 = HelperUtils.uniqueString
            DomainFactory.createProcessingOptionForNotificationRecipient(otrsRecipient)

            String expectedEmailSubjectOperator = "${prefix}#${ticket.ticketNumber} Final Processing Status Update"
            String expectedEmailSubjectCustomer = "[${prefix}#${ticket.ticketNumber}] TO BE SENT: ${seqTrack1.project.name} sequencing data "
            String expectedEmailSubjectCustomer1 = expectedEmailSubjectCustomer + OtrsTicket.ProcessingStep.INSTALLATION.notificationSubject
            String expectedEmailSubjectCustomer2 = expectedEmailSubjectCustomer + OtrsTicket.ProcessingStep.ALIGNMENT.notificationSubject

            trackingService.mailHelperService = Mock(MailHelperService) {
                1 * sendEmail(expectedEmailSubjectOperator, _, [otrsRecipient]) >> { String emailSubject, String content, List<String> recipient ->
                    assert content.contains(expectedStatus.toString())
                }
                1 * sendEmail(expectedEmailSubjectCustomer1, notificationText1, [otrsRecipient])
                1 * sendEmail(expectedEmailSubjectCustomer2, notificationText2, [otrsRecipient])
                0 * _
            }

            trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
                1 * notification(ticket, _, OtrsTicket.ProcessingStep.INSTALLATION, seqTrack1.project) >> notificationText1
                1 * notification(ticket, _, OtrsTicket.ProcessingStep.ALIGNMENT, seqTrack1.project) >> notificationText2
                0 * notification(_, _, _, _)
            }
        }
        when:
        SessionUtils.withNewSession {
            trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairCreation())
            ticket = OtrsTicket.get(ticket.id)
        }

        then:
        ticket.installationFinished != null
        ticket.fastqcFinished == ticket.installationFinished
        ticket.alignmentFinished == ticket.installationFinished
        ticket.snvFinished == null
        ticket.finalNotificationSent
    }

    void "setFinishedTimestampsAndNotify, when alignment is finished but installation is not, don't send notification"() {
        given:
        OtrsTicket ticket
        setupData()
        SessionUtils.withNewSession {
            // Installation: finished timestamp not set, all done,     won't do more
            // FastQC:       finished timestamp not set, all done,     won't do more
            // Alignment:    finished timestamp not set, partly done,  won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = DomainFactory.createOtrsTicket()
            RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
            SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithOneDataFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                    ],
                    [runSegment: runSegment, fileLinked: true])
            SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithOneDataFile([
                    sample               : DomainFactory.createSample(individual: DomainFactory.createIndividual(project: seqTrack1.project)),
                    dataInstallationState: SeqTrack.DataProcessingState.IN_PROGRESS,
                    fastqcState          : SeqTrack.DataProcessingState.NOT_STARTED,
                    run                  : DomainFactory.createRun(
                            seqPlatform: DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                                    seqPlatformGroups: [DomainFactory.createSeqPlatformGroup()],
                            ),
                    ),
            ],
                    [runSegment: runSegment, fileLinked: true]
            )
            DomainFactory.createMergingCriteriaLazy(project: seqTrack1.project, seqType: seqTrack1.seqType)
            DomainFactory.createMergingCriteriaLazy(project: seqTrack2.project, seqType: seqTrack2.seqType)

            setBamFileInProjectFolder(DomainFactory.createRoddyBamFile(
                    DomainFactory.createRoddyBamFile([
                            workPackage: DomainFactory.createMergingWorkPackage(
                                    MergingWorkPackage.getMergingProperties(seqTrack2) +
                                            [pipeline: DomainFactory.createPanCanPipeline()]
                            )
                    ]),
                    DomainFactory.randomProcessedBamFileProperties + [seqTracks: [seqTrack2] as Set],
            ))

            String prefix = "the prefix"
            DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

            String otrsRecipient = HelperUtils.randomEmail
            DomainFactory.createProcessingOptionForNotificationRecipient(otrsRecipient)

            trackingService.mailHelperService = Mock(MailHelperService) {
                0 * _
            }

            trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
                0 * notification(_, _, _, _)
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.setFinishedTimestampsAndNotify(ticket, new SamplePairCreation())
        }

        then:
        ticket.installationFinished == null
        ticket.alignmentFinished == null
        ticket.fastqcFinished == null
        ticket.snvFinished == null
        !ticket.finalNotificationSent
    }

    private static final String OTRS_RECIPIENT = HelperUtils.randomEmail

    @Unroll
    void 'sendCustomerNotification sends expected notification'(int dataCase, boolean automaticNotification, OtrsTicket.ProcessingStep notificationStep, List<String> recipients, String subject) {
        given:
        OtrsTicket ticket
        ProcessingStatus status
        setupData()
        SessionUtils.withNewSession {
            UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                    project: DomainFactory.createProject(name: 'Project1'),
                    user: DomainFactory.createUser(email: EMAIL),
            )
            Sample sample1 = DomainFactory.createSample(
                    individual: DomainFactory.createIndividual(
                            project: userProjectRole.project,
                    )
            )
            Sample sample2 = DomainFactory.createSample(
                    individual: DomainFactory.createIndividual(
                            project: DomainFactory.createProject(
                                    name: 'Project2',
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

            ticket = DomainFactory.createOtrsTicket(automaticNotification: automaticNotification)
            status = new ProcessingStatus(seqTracks.collect { new SeqTrackProcessingStatus(it) })
            String prefix = HelperUtils.uniqueString
            DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
            subject = "[${prefix}#${ticket.ticketNumber}] ${subject}"
            int callCount = recipients.isEmpty() ? 0 : 1
            String content = HelperUtils.randomEmail
            DomainFactory.createProcessingOptionForNotificationRecipient(OTRS_RECIPIENT)

            trackingService.mailHelperService = Mock(MailHelperService) {
                callCount * sendEmail(subject, content, recipients)
                0 * sendEmail(_, _, _)
            }
            trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
                Project project = exactlyOneElement(seqTracks*.project.unique())
                callCount * notification(ticket, _, notificationStep, project) >> {
                    OtrsTicket ticket1, ProcessingStatus status1, OtrsTicket.ProcessingStep processingStep, Project project1 ->
                        TestCase.assertContainSame(status.seqTrackProcessingStatuses, status1.seqTrackProcessingStatuses)
                        return content
                }
            }
        }

        expect:
        SessionUtils.withNewSession {
            trackingService.sendCustomerNotification(ticket, status, notificationStep)
            return true
        }

        where:
        dataCase | automaticNotification | notificationStep                       | recipients              | subject
        1        | true                  | OtrsTicket.ProcessingStep.INSTALLATION | [EMAIL, OTRS_RECIPIENT] | 'Project1 sequencing data installed'
        2        | false                 | OtrsTicket.ProcessingStep.INSTALLATION | [OTRS_RECIPIENT]        | 'TO BE SENT: Project1 sequencing data installed'
        3        | true                  | OtrsTicket.ProcessingStep.INSTALLATION | [EMAIL, OTRS_RECIPIENT] | '[S#1234] Project1 sequencing data installed'
        4        | true                  | OtrsTicket.ProcessingStep.FASTQC       | []                      | null
        5        | true                  | OtrsTicket.ProcessingStep.ALIGNMENT    | [OTRS_RECIPIENT]        | 'TO BE SENT: Project2 sequencing data aligned'
        6        | true                  | OtrsTicket.ProcessingStep.SNV          | [OTRS_RECIPIENT]        | 'TO BE SENT: [S#1234,9876] Project2 sequencing data SNV-called'
    }

    void 'sendCustomerNotification, with multiple projects, sends multiple notifications'() {
        given:
        OtrsTicket ticket
        ProcessingStatus status
        setupData()
        SessionUtils.withNewSession {
            ticket = DomainFactory.createOtrsTicket()
            status = new ProcessingStatus([1, 2].collect { int index ->
                UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                        user: DomainFactory.createUser(email: "project${index}@test.com"),
                        project: DomainFactory.createProject(name: "Project_X${index}"),
                )
                new SeqTrackProcessingStatus(DomainFactory.createSeqTrack(
                        sample: DomainFactory.createSample(
                                individual: DomainFactory.createIndividual(
                                        project: userProjectRole.project,
                                )
                        )
                ))
            })
            String prefix = HelperUtils.uniqueString
            DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
            DomainFactory.createProcessingOptionForNotificationRecipient(OTRS_RECIPIENT)

            trackingService.mailHelperService = Mock(MailHelperService) {
                1 * sendEmail("[${prefix}#${ticket.ticketNumber}] Project_X1 sequencing data installed",
                        'Project_X1', ['project1@test.com', OTRS_RECIPIENT])
                1 * sendEmail("[${prefix}#${ticket.ticketNumber}] Project_X2 sequencing data installed",
                        'Project_X2', ['project2@test.com', OTRS_RECIPIENT])
                0 * sendEmail(_, _, _)
            }
            trackingService.createNotificationTextService = Mock(CreateNotificationTextService) {
                notification(ticket, _, OtrsTicket.ProcessingStep.INSTALLATION, _) >> { OtrsTicket ticket1, ProcessingStatus status1, OtrsTicket.ProcessingStep processingStep, Project project ->
                    return project.name
                }
            }
        }

        expect:
        SessionUtils.withNewSession {
            trackingService.sendCustomerNotification(ticket, status, OtrsTicket.ProcessingStep.INSTALLATION)
            return true
        }
    }

    void 'sendCustomerNotification, with multiple projects, when non are finished, doesnt send emails'() {
        given:
        OtrsTicket ticket
        ProcessingStatus status
        setupData()
        SessionUtils.withNewSession {
            ticket = DomainFactory.createOtrsTicket()
            status = new ProcessingStatus([1, 2].collect { int index ->
                UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                        user: DomainFactory.createUser(email: "project${index}@test.com"),
                        project: DomainFactory.createProject(name: "Project_X${index}"),
                )
                new SeqTrackProcessingStatus(DomainFactory.createSeqTrack(
                        sample: DomainFactory.createSample(
                                individual: DomainFactory.createIndividual(
                                        project: userProjectRole.project,
                                )
                        ),
                ))
            })

            String prefix = HelperUtils.uniqueString
            DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
            DomainFactory.createProcessingOptionForNotificationRecipient(OTRS_RECIPIENT)

            trackingService.mailHelperService = Mock(MailHelperService) {
                0 * sendEmail(_, _, _)
            }
        }

        expect:
        SessionUtils.withNewSession {
            trackingService.sendCustomerNotification(ticket, status, OtrsTicket.ProcessingStep.INSTALLATION)
            return true
        }
    }



    private static SeqTrackProcessingStatus createSeqTrackProcessingStatus(SeqTrack seqTrack) {
        return new SeqTrackProcessingStatus(seqTrack, ALL_DONE, ALL_DONE, [])
    }

    private static ProcessingStatus createProcessingStatus(SeqTrackProcessingStatus... seqTrackStatuses) {
        return new ProcessingStatus(Arrays.asList(seqTrackStatuses))
    }

    void "fillInMergingWorkPackageProcessingStatuses, no ST, does not crash"() {
        given:
        setupData()

        expect:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([]).empty
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 ST not alignable, returns NOTHING_DONE_WONT_DO"() {
        given:
        SeqTrackProcessingStatus seqTrackStatus
        ProcessingStatus processingStatus
        setupData()
        SessionUtils.withNewSession {
            seqTrackStatus = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithOneDataFile([:], [fileWithdrawn: true]))
            processingStatus = createProcessingStatus(seqTrackStatus)
        }


        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
        }

        then:
        seqTrackStatus.mergingWorkPackageProcessingStatuses.isEmpty()
        seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
        seqTrackStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
        processingStatus.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
        processingStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 MWP ALL_DONE, returns ALL_DONE"() {
        given:
        AbstractMergedBamFile bamFile
        SeqTrackProcessingStatus seqTrackStatus
        setupData()
        SessionUtils.withNewSession {
            bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
            seqTrackStatus = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
        }

        then:
        SessionUtils.withNewSession {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
            return  mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage &&
                    mwpStatus.completeProcessableBamFileInProjectFolder == bamFile &&
                    mwpStatus.alignmentProcessingStatus == ALL_DONE &&
                    seqTrackStatus.alignmentProcessingStatus == ALL_DONE &&
                    createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == ALL_DONE
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 MWP NOTHING_DONE_MIGHT_DO, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        AbstractMergedBamFile bamFile
        SeqTrackProcessingStatus seqTrackStatus
        setupData()
        SessionUtils.withNewSession {
            bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
            seqTrackStatus = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithDataFiles(bamFile.mergingWorkPackage))
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
        }

        then:
        SessionUtils.withNewSession {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
            return  mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage &&
                    mwpStatus.completeProcessableBamFileInProjectFolder == null &&
                    mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO &&
                    seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO &&
                    createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 ST not alignable, rest merged, returns NOTHING_DONE_WONT_DO_MORE and ALL_DONE"() {
        given:
        SeqTrackProcessingStatus seqTrack1Status
        SeqTrackProcessingStatus seqTrack2Status
        AbstractMergedBamFile bamFile
        setupData()
        SessionUtils.withNewSession {
            bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
            Set<SeqTrack> seqTracks = new HashSet<SeqTrack>(((MergingWorkPackage) (bamFile.workPackage)).seqTracks)
            seqTrack1Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithDataFiles(bamFile.mergingWorkPackage, [:], [fileWithdrawn: true]))
            ((MergingWorkPackage) (bamFile.workPackage)).seqTracks = seqTracks
            bamFile.workPackage.save(flush: true)
            seqTrack2Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status])
        }

        then:
        SessionUtils.withNewSession {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack2Status.mergingWorkPackageProcessingStatuses)
            return  seqTrack1Status.mergingWorkPackageProcessingStatuses.isEmpty() &&
                    seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO &&
                    mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage &&
                    mwpStatus.completeProcessableBamFileInProjectFolder == bamFile &&
                    mwpStatus.alignmentProcessingStatus == ALL_DONE &&
                    seqTrack2Status.alignmentProcessingStatus == ALL_DONE &&
                    createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_WONT_DO_MORE
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, no MWP, returns NOTHING_DONE_WONT_DO"() {
        given:
        SeqTrackProcessingStatus seqTrackStatus
        ProcessingStatus processingStatus
        setupData()
        SessionUtils.withNewSession {
            seqTrackStatus = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithOneDataFile())
            processingStatus = createProcessingStatus(seqTrackStatus)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
        }

        then:
        seqTrackStatus.mergingWorkPackageProcessingStatuses.isEmpty()
        seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
        seqTrackStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
        processingStatus.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
        processingStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        ProcessedMergedBamFile bamFile
        SeqTrackProcessingStatus seqTrackStatus
        setupData()
        SessionUtils.withNewSession {
            bamFile = DomainFactory.createProcessedMergedBamFile()
            seqTrackStatus = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
        }

        then:
        SessionUtils.withNewSession {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
            return  mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage &&
                    mwpStatus.completeProcessableBamFileInProjectFolder == null &&
                    mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO &&
                    seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO &&
                    createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        AbstractMergedBamFile bamFile
        SeqTrackProcessingStatus seqTrack1Status, seqTrack2Status
        setupData()

        SessionUtils.withNewSession {
            bamFile = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
            seqTrack1Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
            seqTrack2Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithOneDataFile())
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status])
        }

        then:
        SessionUtils.withNewSession {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
            return  mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage &&
                    mwpStatus.completeProcessableBamFileInProjectFolder == bamFile &&
                    mwpStatus.alignmentProcessingStatus == ALL_DONE &&
                    seqTrack1Status.alignmentProcessingStatus == ALL_DONE &&
                    seqTrack2Status.mergingWorkPackageProcessingStatuses.isEmpty() &&
                    seqTrack2Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO &&
                    createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_WONT_DO_MORE
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        AbstractMergedBamFile bamFile
        SeqTrackProcessingStatus seqTrack1Status, seqTrack2Status
        setupData()
        SessionUtils.withNewSession {
            bamFile = DomainFactory.createProcessedMergedBamFile()
            seqTrack1Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
            seqTrack2Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithOneDataFile())
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status])
        }

        then:
        SessionUtils.withNewSession {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
            return  mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage &&
                    mwpStatus.completeProcessableBamFileInProjectFolder == null &&
                    mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO &&
                    seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO &&
                    seqTrack2Status.mergingWorkPackageProcessingStatuses.isEmpty() &&
                    seqTrack2Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO &&
                    createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP NOTHING_DONE_MIGHT_DO, 1 MWP ALL_DONE, returns PARTLY_DONE_MIGHT_DO_MORE"() {
        given:
        setupData()

        AbstractMergedBamFile bamFile1, bamFile2
        SeqTrackProcessingStatus seqTrack1Status, seqTrack2Status, seqTrack3Status

        SessionUtils.withNewSession {
            bamFile1 = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
            bamFile2 = createBamFileInProjectFolder(DomainFactory.randomProcessedBamFileProperties)
            seqTrack1Status = createSeqTrackProcessingStatus(bamFile1.containedSeqTracks.first())
            seqTrack2Status = createSeqTrackProcessingStatus(bamFile2.containedSeqTracks.first())
            seqTrack3Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithDataFiles(bamFile1.mergingWorkPackage))
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status, seqTrack3Status])
        }

        then:
        SessionUtils.withNewSession {
            MergingWorkPackageProcessingStatus mwp1Status = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
            MergingWorkPackageProcessingStatus mwp2Status = exactlyOneElement(seqTrack2Status.mergingWorkPackageProcessingStatuses)

            return  mwp1Status.mergingWorkPackage == bamFile1.mergingWorkPackage &&
                    mwp1Status.completeProcessableBamFileInProjectFolder == null &&
                    mwp1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO &&
                    seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO &&
                    mwp2Status.mergingWorkPackage == bamFile2.mergingWorkPackage &&
                    mwp2Status.completeProcessableBamFileInProjectFolder == bamFile2 &&
                    mwp2Status.alignmentProcessingStatus == ALL_DONE &&
                    seqTrack2Status.alignmentProcessingStatus == ALL_DONE &&
                    exactlyOneElement(seqTrack3Status.mergingWorkPackageProcessingStatuses).is(mwp1Status) &&
                    createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_MIGHT_DO_MORE
        }
    }

    private static MergingWorkPackageProcessingStatus createMergingWorkPackageProcessingStatus(
            MergingWorkPackage mergingWorkPackage, WorkflowProcessingStatus processingStatus) {
        return new MergingWorkPackageProcessingStatus(mergingWorkPackage, processingStatus, null, [])
    }

    private MergingWorkPackageProcessingStatus createMergingWorkPackageProcessingStatus(AbstractMergedBamFile bamFile) {
        return new MergingWorkPackageProcessingStatus(bamFile.mergingWorkPackage, ALL_DONE, bamFile, [])
    }

    private SeqTrackProcessingStatus createSeqTrackProcessingStatus(MergingWorkPackageProcessingStatus... mwpStatuses) {
        // seqTrack actually must not be null, but for this test it does not matter
        return new SeqTrackProcessingStatus(null, ALL_DONE, ALL_DONE, Arrays.asList(mwpStatuses))
    }

    void "fillInSamplePairStatuses, no MWP, does not crash"() {
        given:
        setupData()

        expect:
        trackingService.fillInSamplePairStatuses([], new SamplePairCreation())
    }

    void "fillInSamplePairStatuses, no SP, returns NOTHING_DONE_WONT_DO"() {
        given:
        setupData()

        MergingWorkPackageProcessingStatus mwpStatus
        SessionUtils.withNewSession {
            mwpStatus = createMergingWorkPackageProcessingStatus(DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            mwpStatus.samplePairProcessingStatuses.isEmpty()
            mwpStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
            mwpStatus.indelProcessingStatus == NOTHING_DONE_WONT_DO
            mwpStatus.sophiaProcessingStatus == NOTHING_DONE_WONT_DO
            mwpStatus.aceseqProcessingStatus == NOTHING_DONE_WONT_DO
            mwpStatus.runYapsaProcessingStatus == NOTHING_DONE_WONT_DO
            createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_WONT_DO
        }
    }

    @Unroll("fillInSamplePairStatuses, #pairAnalysis.analysisType withdrawn, returns NOTHING_DONE_WONT_DO")
    void "fillInSamplePairStatuses, analysisInstance withdrawn, returns NOTHING_DONE_WONT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                setBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withNewSession {
            analysisInstance = BamFilePairAnalysis.get(analysisInstance.id)
            analysisInstance.withdrawn = true
            analysisInstance.save(flush: true)
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert analysisInstance.withdrawn
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            assert mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO
            assert createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO
            return true
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    @Unroll("fillInSamplePairStatuses, no #pairAnalysis.analysisType, bamFileInProjectFolder set, no samplePairForProcessing, returns NOTHING_DONE_WONT_DO")
    void "fillInSamplePairStatuses, no analysisInstance, bamFileInProjectFolder set, no samplePairForProcessing, returns NOTHING_DONE_WONT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"()
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                setBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
            analysisInstance.delete(flush: true)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            return  samplePairStatus.samplePair == analysisInstance.samplePair &&
                    samplePairStatus."${pairAnalysis.completeCallingInstance}" == null &&
                    samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO &&
                    mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO &&
                    createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    @Unroll("fillInSamplePairStatuses, no #pairAnalysis.analysisType, bamFileInProjectFolder set, samplePairForProcessing exists, returns NOTHING_DONE_MIGHT_DO")
    void "fillInSamplePairStatuses, no analysisInstance, bamFileInProjectFolder set, samplePairForProcessing exists, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"([:], [coverage: 2], [coverage: 2])
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                setBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
                DomainFactory.createProcessingThresholdsForBamFile(analysisInstance."sampleType${it}BamFile", [coverage: 1, numberOfLanes: null])
            }

            referenceGenomeProcessingOptions.each {
                it.value = analysisInstance.samplePair.mergingWorkPackage1.referenceGenome.name
                it.save(flush: true)
            }

            switch (pairAnalysis.analysisType) {
                case OtrsTicket.ProcessingStep.ACESEQ:
                    analysisInstance.samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
                    analysisInstance.samplePair.save(flush: true)
                    DomainFactory.createSophiaInstance(analysisInstance.samplePair)
                    break
                case OtrsTicket.ProcessingStep.RUN_YAPSA:
                    analysisInstance.samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
                    analysisInstance.samplePair.save(flush: true)
                    DomainFactory.createRoddySnvInstanceWithRoddyBamFiles([samplePair: analysisInstance.samplePair])
                    break
            }

            analysisInstance.delete(flush: true)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert  samplePairStatus.samplePair == analysisInstance.samplePair
            assert  samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            assert  samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert  mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert  createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            return true
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    void "fillInSamplePairStatuses, no AI, bamFileInProjectFolder set, samplePairForProcessing exists, but Sophia is not finished yet, returns NOTHING_DONE_WONT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory.createAceseqInstanceWithRoddyBamFiles([:], [coverage: 2], [coverage: 2])
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)

            [1, 2].each {
                setBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
                DomainFactory.createProcessingThresholdsForBamFile(analysisInstance."sampleType${it}BamFile", [coverage: 1, numberOfLanes: null])
            }

            referenceGenomeProcessingOptions.each {
                it.value = analysisInstance.samplePair.mergingWorkPackage1.referenceGenome.name
                it.save(flush: true)
            }

            analysisInstance.delete(flush: true)
        }


        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            return  samplePairStatus.samplePair == analysisInstance.samplePair &&
                    samplePairStatus.completeAceseqInstance == null &&
                    samplePairStatus.aceseqProcessingStatus == NOTHING_DONE_WONT_DO &&
                    mwpStatus.aceseqProcessingStatus == NOTHING_DONE_WONT_DO &&
                    createSeqTrackProcessingStatus(mwpStatus).aceseqProcessingStatus == NOTHING_DONE_WONT_DO
        }
    }

    @Unroll("fillInSamplePairStatuses, no #pairAnalysis.analysisType, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO")
    void "fillInSamplePairStatuses, no analysisInstance, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"()
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile.mergingWorkPackage, NOTHING_DONE_MIGHT_DO)
            analysisInstance.delete(flush: true)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            return  samplePairStatus.samplePair == analysisInstance.samplePair &&
                    samplePairStatus."${pairAnalysis.completeCallingInstance}" == null &&
                    samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO &&
                    mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO &&
                    createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    @Unroll("fillInSamplePairStatuses, 1 #pairAnalysis.analysisType FINISHED, bamFileInProjectFolder set, returns ALL_DONE")
    void "fillInSamplePairStatuses, 1 analysisInstance FINISHED, bamFileInProjectFolder set, returns ALL_DONE"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                setBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            return  samplePairStatus.samplePair == analysisInstance.samplePair &&
                    samplePairStatus."${pairAnalysis.completeCallingInstance}" == analysisInstance &&
                    samplePairStatus."${pairAnalysis.processingStatus}" == ALL_DONE &&
                    mwpStatus."${pairAnalysis.processingStatus}" == ALL_DONE &&
                    createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == ALL_DONE
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    @Unroll("fillInSamplePairStatuses, 1 #pairAnalysis.analysisType FINISHED, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO")
    void "fillInSamplePairStatuses, 1 analysisInstance FINISHED, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile.mergingWorkPackage, NOTHING_DONE_MIGHT_DO)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            return  samplePairStatus.samplePair == analysisInstance.samplePair
                    samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
                    samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
                    mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
                    createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
        }

        where:
        pairAnalysis << listPairAnalysis
    }


    @Unroll("fillInSamplePairStatuses, 1 #pairAnalysis.analysisType not FINISHED, returns NOTHING_DONE_MIGHT_DO")
    void "fillInSamplePairStatuses, 1 analysisInstance not FINISHED, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"()
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                setBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwpStatus], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            samplePairStatus.samplePair == analysisInstance.samplePair
            samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
        }

        where:
        pairAnalysis << listPairAnalysis
    }


    @Unroll("fillInSamplePairStatuses, with #pairAnalysis.analysisType 2 MWP, 1 SP ALL_DONE, 1 MWP without SP, returns ALL_DONE and NOTHING_DONE_WONT_DO")
    void "fillInSamplePairStatuses, with analysisInstance 2 MWP, 1 SP ALL_DONE, 1 MWP without SP, returns ALL_DONE and NOTHING_DONE_WONT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwp1Status
        MergingWorkPackageProcessingStatus mwp2Status

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwp1Status = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            mwp2Status = createMergingWorkPackageProcessingStatus(DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)
            [1, 2].each {
                setBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwp1Status.samplePairProcessingStatuses)
            return  samplePairStatus.samplePair == analysisInstance.samplePair &&
                    samplePairStatus."${pairAnalysis.completeCallingInstance}" == analysisInstance &&
                    samplePairStatus."${pairAnalysis.processingStatus}" == ALL_DONE &&
                    mwp1Status."${pairAnalysis.processingStatus}" == ALL_DONE &&
                    mwp2Status.samplePairProcessingStatuses.isEmpty() &&
                    mwp2Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO &&
                    createSeqTrackProcessingStatus(mwp1Status, mwp2Status)."${pairAnalysis.processingStatus}" == PARTLY_DONE_WONT_DO_MORE
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    @Unroll("fillInSamplePairStatuses, with #pairAnalysis.analysisType 2 MWP, 1 MWP without SP, 1 MWP MIGHT_DO_MORE, returns NOTHING_DONE_WONT_DO and NOTHING_DONE_MIGHT_DO")
    void "fillInSamplePairStatuses, with analysisInstance 2 MWP, 1 MWP without SP, 1 MWP MIGHT_DO_MORE, returns NOTHING_DONE_WONT_DO and NOTHING_DONE_MIGHT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwp1Status
        MergingWorkPackageProcessingStatus mwp2Status

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwp1Status = createMergingWorkPackageProcessingStatus(DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)
            mwp2Status = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwp2Status.samplePairProcessingStatuses)

            return  mwp1Status.samplePairProcessingStatuses.isEmpty() &&
                    mwp1Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO &&
                    samplePairStatus.samplePair == analysisInstance.samplePair &&
                    samplePairStatus."${pairAnalysis.completeCallingInstance}" == null &&
                    samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO &&
                    mwp2Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO &&
                    createSeqTrackProcessingStatus(mwp1Status, mwp2Status)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    @Unroll("fillInSamplePairStatuses, with #pairAnalysis.analysisType 2 MWP, 1 SP ALL_DONE, 1 SP MIGHT_DO_MORE, returns ALL_DONE and NOTHING_DONE_MIGHT_DO")
    void "fillInSamplePairStatuses, with analysisInstance 2 MWP, 1 SP ALL_DONE, 1 SP MIGHT_DO_MORE, returns ALL_DONE and NOTHING_DONE_MIGHT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        BamFilePairAnalysis analysisInstance2
        MergingWorkPackageProcessingStatus mwp1Status
        MergingWorkPackageProcessingStatus mwp2Status

        SessionUtils.withNewSession {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            analysisInstance2 = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwp1Status = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            mwp2Status = createMergingWorkPackageProcessingStatus(analysisInstance2.sampleType1BamFile)
            [1, 2].each {
                setBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.fillInSamplePairStatuses([mwp1Status, mwp2Status], new SamplePairCreation())
        }

        then:
        SessionUtils.withNewSession {
            SamplePairProcessingStatus samplePair1Status = exactlyOneElement(mwp1Status.samplePairProcessingStatuses)
            SamplePairProcessingStatus samplePair2Status = exactlyOneElement(mwp2Status.samplePairProcessingStatuses)

            return  samplePair1Status.samplePair == analysisInstance.samplePair &&
                    samplePair1Status."${pairAnalysis.completeCallingInstance}" == analysisInstance &&
                    samplePair1Status."${pairAnalysis.processingStatus}" == ALL_DONE &&
                    mwp1Status."${pairAnalysis.processingStatus}" == ALL_DONE &&
                    samplePair2Status.samplePair == analysisInstance2.samplePair &&
                    samplePair2Status."${pairAnalysis.completeCallingInstance}" == null &&
                    samplePair2Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO &&
                    mwp2Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO &&
                    createSeqTrackProcessingStatus(mwp1Status, mwp2Status)."${pairAnalysis.processingStatus}" == PARTLY_DONE_MIGHT_DO_MORE
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    private static AbstractMergedBamFile createBamFileInProjectFolder(Map bamFileProperties = [:]) {
        AbstractMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(bamFileProperties)

        return setBamFileInProjectFolder(bamFile)
    }

    private static AbstractMergedBamFile setBamFileInProjectFolder(AbstractMergedBamFile bamFile) {
        bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
        bamFile.mergingWorkPackage.save(flush: true)

        return bamFile
    }

    void "assignOtrsTicketToRunSegment, no RunSegment for runSegementId, throws AssertionError "() {
        given:
        setupData()

        when:
        SessionUtils.withNewSession {
            trackingService.assignOtrsTicketToRunSegment("", 1)
        }

        then:
        AssertionError error = thrown()
        error.message.contains("No RunSegment found")
    }

    void "assignOtrsTicketToRunSegment, new ticketNumber equals old ticketNumber, returns true"() {
        given:
        setupData()

        OtrsTicket otrsTicket
        RunSegment runSegment

        SessionUtils.withNewSession {
            otrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345678')
            runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)
        }

        expect:
        SessionUtils.withNewSession {
            trackingService.assignOtrsTicketToRunSegment(otrsTicket.ticketNumber, runSegment.id)
            return true
        }
    }

    void "assignOtrsTicketToRunSegment, new ticketNumber does not pass custom validation, throws UserException"() {
        given:
        setupData()

        OtrsTicket otrsTicket
        RunSegment runSegment

        SessionUtils.withNewSession {
            otrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345678')
            runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.assignOtrsTicketToRunSegment('abc', runSegment.id)
        }

        then:
        UserException error = thrown()
        error.message.contains("does not pass validation or error while saving.")
    }

    void "assignOtrsTicketToRunSegment, old OtrsTicket consists of several other RunSegements, throws UserException"() {
        given:
        setupData()

        OtrsTicket otrsTicket
        RunSegment runSegment

        SessionUtils.withNewSession {
            otrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345678')
            runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)
            DomainFactory.createRunSegment(otrsTicket: otrsTicket)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.assignOtrsTicketToRunSegment('2000010112345679', runSegment.id)
        }

        then:
        UserException error = thrown()
        error.message.contains("Assigning a runSegment that belongs to an OTRS-Ticket which consists of several other runSegments is not allowed.")
    }

    void "assignOtrsTicketToRunSegment, new OtrsTicket final notification already sent, throws UserException"() {
        given:
        setupData()

        OtrsTicket oldOtrsTicket
        RunSegment runSegment
        OtrsTicket newOtrsTicket

        SessionUtils.withNewSession {
            oldOtrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345678')
            runSegment = DomainFactory.createRunSegment(otrsTicket: oldOtrsTicket)
            newOtrsTicket = DomainFactory.createOtrsTicket(ticketNumber: '2000010112345679', finalNotificationSent: true)
        }

        when:
        SessionUtils.withNewSession {
            trackingService.assignOtrsTicketToRunSegment(newOtrsTicket.ticketNumber, runSegment.id)
        }

        then:
        UserException error = thrown()
        error.message.contains("It is not allowed to assign to an finally notified OTRS-Ticket.")
    }

    void "assignOtrsTicketToRunSegment, adjust ProcessingStatus of new OtrsTicket"() {
        given:
        setupData()

        OtrsTicket newOtrsTicket
        RunSegment runSegment

        Date minDate = new Date().minus(1)
        Date maxDate = new Date().plus(1)

        SessionUtils.withNewSession {
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
            runSegment = DomainFactory.createRunSegment(otrsTicket: oldOtrsTicket)
            newOtrsTicket = DomainFactory.createOtrsTicket(
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
                        getFastqcProcessingStatus      : { -> ALL_DONE },
                        getAlignmentProcessingStatus   : { -> PARTLY_DONE_MIGHT_DO_MORE },
                        getSnvProcessingStatus         : { -> NOTHING_DONE_MIGHT_DO },
                ] as ProcessingStatus
            }
        }

        when:
        SessionUtils.withNewSession {
            trackingService.assignOtrsTicketToRunSegment(newOtrsTicket.ticketNumber, runSegment.id)
        }

        then:
        SessionUtils.withNewSession {
            newOtrsTicket = OtrsTicket.get(newOtrsTicket.id)
            assert minDate.getTime() == newOtrsTicket.installationStarted.getTime()
            assert maxDate.getTime() == newOtrsTicket.installationFinished.getTime()
            assert minDate.getTime() == newOtrsTicket.fastqcStarted.getTime()
            assert maxDate.getTime() == newOtrsTicket.fastqcFinished.getTime()
            assert minDate.getTime() == newOtrsTicket.alignmentStarted.getTime()
            assert null == newOtrsTicket.alignmentFinished
            assert null == newOtrsTicket.snvStarted
            assert null == newOtrsTicket.snvFinished
            return true
        }
    }


    void "fillInMergingWorkPackageProcessingStatuses, when a SeqTrack is added manually to an existing MergingWorkPackage, then find the MergingWorkPackage for this SeqTrack"() {
        given: "two seqtracks, with different merging properties, but manually combined in the same MergingWorkPackage"
        setupData()

        SeqTrackProcessingStatus statusB
        MergingWorkPackage mergePackageA

        SessionUtils.withNewSession {
            // shared
            Sample sharedSample = DomainFactory.createSample()
            SeqType sharedSeqType = DomainFactory.createSeqType()

            // seqtrack A
            SeqPlatformGroup groupA = DomainFactory.createSeqPlatformGroup()
            SeqTrack seqTrackA = createSeqTrack(sharedSample, sharedSeqType, groupA)
            new SeqTrackProcessingStatus(seqTrackA,
                    ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                    ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                    []
            )
            // seqtrack B
            SeqPlatformGroup groupB = DomainFactory.createSeqPlatformGroup()
            SeqTrack seqTrackB = createSeqTrack(sharedSample, sharedSeqType, groupB)
            statusB = new SeqTrackProcessingStatus(seqTrackB,
                    ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE,
                    ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE,
                    []
            )
            // "existing" merge group
            DomainFactory.createMergingCriteriaLazy(project: sharedSample.project, seqType: sharedSeqType)
            mergePackageA = DomainFactory.createMergingWorkPackage([
                    libraryPreparationKit: null, // irrelevant for this test, don't even need to mock it
                    sample               : sharedSample,
                    seqType              : sharedSeqType,
                    seqPlatformGroup     : groupA,
                    seqTracks            : [seqTrackA],
            ], true)

            // "manually" add seqtrack with mismatching seqPlatformGroup
            mergePackageA.seqTracks.add(seqTrackB)
            mergePackageA.save(flush: true)
        }

        when: "looking for B"
        List<MergingWorkPackageProcessingStatus> statuses
        SessionUtils.withNewSession {
            statuses = trackingService.fillInMergingWorkPackageProcessingStatuses([statusB])
        }

        then: "we should find A's merging package"
        SessionUtils.withNewSession {
            statuses*.mergingWorkPackage == [mergePackageA]
        }
    }


    void "fillInMergingWorkPackageProcessingStatuses, when a SeqTrack is removed manually from an existing MergingWorkPackage, then do not find the MergingWorkPackage for this SeqTrack"() {
        given: "two seqtracks, with same merging properties, but one manually removed from the MergingWorkPackage"
        setupData()

        SeqTrackProcessingStatus statusB

        SessionUtils.withNewSession {
            // shared
            Sample sharedSample = DomainFactory.createSample()
            SeqType sharedSeqType = DomainFactory.createSeqType()

            // seqtrack A
            SeqPlatformGroup groupA = DomainFactory.createSeqPlatformGroup()
            SeqTrack seqTrackA = createSeqTrack(sharedSample, sharedSeqType, groupA)
            new SeqTrackProcessingStatus(seqTrackA,
                    ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                    ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                    []
            )
            // seqtrack B
            SeqTrack seqTrackB = createSeqTrack(sharedSample, sharedSeqType, groupA)
            statusB = new SeqTrackProcessingStatus(seqTrackB,
                    ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE,
                    ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE,
                    []
            )
            // "existing" merge group
            DomainFactory.createMergingCriteriaLazy(project: sharedSample.project, seqType: sharedSeqType)
            MergingWorkPackage mergePackageA = DomainFactory.createMergingWorkPackage([
                    libraryPreparationKit: null, // irrelevant for this test, don't even need to mock it
                    sample               : sharedSample,
                    seqType              : sharedSeqType,
                    seqPlatformGroup     : groupA,
                    seqTracks            : [seqTrackA, seqTrackB],
            ], true)

            // "manually" remove seqtrack with matching seqPlatformGroup
            mergePackageA.seqTracks.remove(seqTrackB)
            mergePackageA.save(flush: true)
        }

        when: "looking for B"
        List<MergingWorkPackageProcessingStatus> statuses
        SessionUtils.withNewSession {
            statuses = trackingService.fillInMergingWorkPackageProcessingStatuses([statusB])
        }

        then: "we should not find a merging package"
        SessionUtils.withNewSession {
            return statuses.empty
        }
    }


    private SeqTrack createSeqTrack(Sample sample, SeqType seqType, SeqPlatformGroup groupA) {
        SeqTrack seqTrackA = DomainFactory.createSeqTrackWithOneDataFile([
                sample : sample,
                seqType: seqType,
                run    : DomainFactory.createRun(
                        seqPlatform: DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                                seqPlatformGroups: [groupA],
                        ),
                ),
        ])
        return seqTrackA
    }

    void 'sendOperatorNotification, when finalNotification is true and project.customFinalNotification is true and no Ilse, sends final notification with correct subject and to project list'() {
        given:
        setupData()

        OtrsTicket ticket
        UserProjectRole userProjectRole
        SeqTrack seqTrack

        SessionUtils.withNewSession {
            ticket = DomainFactory.createOtrsTicket()
            userProjectRole = DomainFactory.createUserProjectRole(project: DomainFactory.createProject(customFinalNotification: true))
            seqTrack = DomainFactory.createSeqTrackWithOneDataFile(
                    [
                            ilseSubmission: null,
                            sample        : DomainFactory.createSample(
                                    individual: DomainFactory.createIndividual(
                                            project: userProjectRole.project
                                    )
                            ),
                    ],
                    [runSegment: DomainFactory.createRunSegment(otrsTicket: ticket), fileLinked: true])
            DomainFactory.createProcessingOptionForOtrsTicketPrefix(PREFIX)
            String recipient = HelperUtils.randomEmail
            DomainFactory.createProcessingOptionForNotificationRecipient(recipient)
            String expectedHeader = "${PREFIX}#${ticket.ticketNumber} Final Processing Status Update ${seqTrack.individual.pid} (${seqTrack.seqType.displayName})"
            trackingService.mailHelperService = Mock(MailHelperService) {
                1 * sendEmail(expectedHeader, _, [userProjectRole.user.email, recipient])
            }
        }

        expect:
        SessionUtils.withNewSession {
            trackingService.sendOperatorNotification(ticket, [seqTrack] as Set, new ProcessingStatus(), true)
            return true
        }
    }
}
