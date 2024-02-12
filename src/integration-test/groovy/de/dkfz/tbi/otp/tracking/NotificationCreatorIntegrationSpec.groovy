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

import grails.core.GrailsApplication
import grails.web.mapping.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.AbstractIntegrationSpecWithoutRollbackAnnotation
import de.dkfz.tbi.otp.TestMessageSourceService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import de.dkfz.tbi.otp.utils.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class NotificationCreatorIntegrationSpec extends AbstractIntegrationSpecWithoutRollbackAnnotation implements DomainFactoryCore, IsRoddy {

    @Autowired
    GrailsApplication grailsApplication

    NotificationCreator notificationCreator
    MailHelperService mailHelperService
    IndelCallingService indelCallingService
    SnvCallingService snvCallingService
    SophiaService sophiaService
    AceseqService aceseqService
    RunYapsaService runYapsaService
    CreateNotificationTextService createNotificationTextService
    ProcessingOptionService processingOptionService
    UserProjectRoleService userProjectRoleService
    TicketService ticketService

    List<ProcessingOption> referenceGenomeProcessingOptions

    final static String EMAIL = HelperUtils.randomEmail
    final static String PREFIX = "TICKET_PREFIX"
    final static String EMAIL_TICKET_SYSTEM = HelperUtils.randomEmail

    static List listPairAnalysis = [
            [
                    analysisType           : Ticket.ProcessingStep.INDEL,
                    createRoddyBamFile     : "createIndelCallingInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeIndelCallingInstance",
                    processingStatus       : "indelProcessingStatus",
            ], [
                    analysisType           : Ticket.ProcessingStep.SNV,
                    createRoddyBamFile     : "createRoddySnvInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeSnvCallingInstance",
                    processingStatus       : "snvProcessingStatus",
            ], [
                    analysisType           : Ticket.ProcessingStep.SOPHIA,
                    createRoddyBamFile     : "createSophiaInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeSophiaInstance",
                    processingStatus       : "sophiaProcessingStatus",
            ], [
                    analysisType           : Ticket.ProcessingStep.ACESEQ,
                    createRoddyBamFile     : "createAceseqInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeAceseqInstance",
                    processingStatus       : "aceseqProcessingStatus",
            ], [
                    analysisType           : Ticket.ProcessingStep.RUN_YAPSA,
                    createRoddyBamFile     : "createRunYapsaInstanceWithRoddyBamFiles",
                    completeCallingInstance: "completeRunYapsaInstance",
                    processingStatus       : "runYapsaProcessingStatus",
            ],
    ]*.asImmutable().asImmutable()

    void setupData() {
        createNotificationTextService.messageSourceService = new TestMessageSourceService()

        // Overwrite the autowired service with a new instance for each test, so mocks do not have to be cleaned up
        notificationCreator = new NotificationCreator(
                mailHelperService: mailHelperService,
                indelCallingService: indelCallingService,
                snvCallingService: snvCallingService,
                sophiaService: sophiaService,
                aceseqService: aceseqService,
                runYapsaService: runYapsaService,
                createNotificationTextService: createNotificationTextService,
                processingOptionService: processingOptionService,
                userProjectRoleService: userProjectRoleService,
                ticketService: ticketService,
                grailsApplication: grailsApplication,
        )
        SessionUtils.withTransaction {
            DomainFactory.createAllAnalysableSeqTypes()

            referenceGenomeProcessingOptions = DomainFactory.createReferenceGenomeAndAnalysisProcessingOptions()

            findOrCreateProcessingOption(
                    name: ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM,
                    value: EMAIL_TICKET_SYSTEM,
            )
        }
    }

    void "check that all analyses are provided in the list 'listPairAnalysis'"() {
        given:
        List<Ticket.ProcessingStep> analysisProcessingSteps
        setupData()

        analysisProcessingSteps = Ticket.ProcessingStep.values() - [
                Ticket.ProcessingStep.INSTALLATION,
                Ticket.ProcessingStep.FASTQC,
                Ticket.ProcessingStep.ALIGNMENT,
        ]

        expect:
        TestCase.assertContainSame(analysisProcessingSteps, listPairAnalysis*.analysisType)
    }

    void 'processFinished calls setFinishedTimestampsAndNotify for the tickets of the passed SeqTracks'() {
        given: "tickets with (at least one) fastQC still in progress"
        Ticket ticketA, ticketB
        SeqTrack seqTrackA, seqTrackB
        setupData()
        SessionUtils.withTransaction {
            ticketA = createTicket()
            seqTrackA = createSeqTrackWithOneFastqFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.IN_PROGRESS,
                    ],
                    [fastqImportInstance: createFastqImportInstance(ticket: ticketA), fileLinked: true])

            ticketB = createTicket()
            seqTrackB = createSeqTrackWithOneFastqFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                    ],
                    [fastqImportInstance: createFastqImportInstance(ticket: ticketB), fileLinked: true])
            createSeqTrackWithOneFastqFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.IN_PROGRESS,
                    ],
                    [fastqImportInstance: createFastqImportInstance(ticket: ticketB), fileLinked: true])
            DomainFactory.createProcessingOptionForTicketPrefix("the prefix")
        }

        when: "running our tracking update"
        SessionUtils.withTransaction {
            notificationCreator.processFinished([seqTrackA, seqTrackB] as Set)
            ticketA = Ticket.get(ticketA.id)
            ticketB = Ticket.get(ticketA.id)
        }

        then: "installation should be marked as done, but fastQC as still running"
        ticketA.installationFinished != null
        ticketB.installationFinished != null
        ticketB.fastqcFinished == null
    }

    void 'setFinishedTimestampsAndNotify, when final notification has already been sent, does nothing'() {
        given:
        Ticket ticket
        Date installationFinished = new Date()
        setupData()
        SessionUtils.withTransaction {
            // Installation: finished timestamp set,     all done,     won't do more
            // FastQC:       finished timestamp not set, all done,     won't do more
            // Alignment:    finished timestamp not set, nothing done, won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = createTicket(
                    installationFinished: installationFinished,
                    finalNotificationSent: true,
            )
            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)
            createSeqTrackWithOneFastqFile(
                    [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                    [fastqImportInstance: fastqImportInstance, fileLinked: true])

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                0 * _
            }
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.notifyAndSetFinishedTimestamps(ticket)
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
        Ticket ticket
        Date installationFinished = new Date()
        setupData()
        SessionUtils.withTransaction {
            // Installation: finished timestamp set,     all done,     won't do more
            // FastQC:       finished timestamp not set, partly done,  might do more
            // Alignment:    finished timestamp not set, nothing done, won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = createTicket(
                    installationFinished: installationFinished,
            )
            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)
            createSeqTrackWithOneFastqFile(
                    [fastqcState: SeqTrack.DataProcessingState.FINISHED],
                    [fastqImportInstance: fastqImportInstance, fileLinked: true])
            createSeqTrackWithOneFastqFile(
                    [fastqcState: SeqTrack.DataProcessingState.IN_PROGRESS],
                    [fastqImportInstance: fastqImportInstance, fileLinked: true])

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                0 * _
            }
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.notifyAndSetFinishedTimestamps(ticket)
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
        Ticket ticket
        setupData()
        SessionUtils.withTransaction {
            // Installation: finished timestamp not set, all done,     won't do more
            // FastQC:       finished timestamp not set, nothing done, might do more
            // Alignment:    finished timestamp not set, nothing done, won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = createTicket()
            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)
            SeqTrack seqTrack = createSeqTrackWithOneFastqFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.IN_PROGRESS,
                    ],
                    [fastqImportInstance: fastqImportInstance, fileLinked: true])
            ProcessingStatus expectedStatus = [
                    getInstallationProcessingStatus: { -> ALL_DONE },
                    getFastqcProcessingStatus      : { -> NOTHING_DONE_MIGHT_DO },
                    getAlignmentProcessingStatus   : { -> NOTHING_DONE_WONT_DO },
                    getSnvProcessingStatus         : { -> NOTHING_DONE_WONT_DO },
            ] as ProcessingStatus

            String prefix = "the prefix"
            DomainFactory.createProcessingOptionForTicketPrefix(prefix)

            String expectedEmailSubjectOperator = "${prefix}#${ticket.ticketNumber} Processing Status Update"
            String expectedEmailSubjectCustomer = "[${prefix}#${ticket.ticketNumber}] TO BE SENT: ${seqTrack.project.name} sequencing data installed"

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                1 * sendEmailToTicketSystem(expectedEmailSubjectOperator, _) >> { String emailSubject, String content ->
                    assert content.contains(expectedStatus.toString())
                }
                1 * sendEmailToTicketSystem(expectedEmailSubjectCustomer, _)
                0 * _
            }
        }

        when:
        SessionUtils.withTransaction {
            Ticket ticketToFinish = Ticket.get(ticket.id)
            notificationCreator.notifyAndSetFinishedTimestamps(ticketToFinish)
        }

        then:
        SessionUtils.withTransaction {
            Ticket result = Ticket.get(ticket.id)
            assert result.installationFinished != null
            assert result.fastqcFinished == null
            assert result.alignmentFinished == null
            assert result.snvFinished == null
            assert !result.finalNotificationSent
            return true
        }
    }

    void "setFinishedTimestampsAndNotify, when something just completed and won't do more, sends final notification"() {
        given:
        Ticket ticket
        setupData()
        SessionUtils.withTransaction {
            // Installation: finished timestamp not set, all done,     won't do more
            // FastQC:       finished timestamp not set, all done,     won't do more
            // Alignment:    finished timestamp not set, partly done,  won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = createTicket()
            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)
            SeqTrack seqTrack1 = createSeqTrackWithOneFastqFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                    ],
                    [
                            fastqImportInstance: fastqImportInstance,
                            fileLinked         : true,
                    ],
            )
            SeqTrack seqTrack2 = createSeqTrackWithOneFastqFile(
                    [
                            sample               : createSample(individual: createIndividual(project: seqTrack1.project)),
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                            run                  : createRun(
                                    seqPlatform: createSeqPlatformWithSeqPlatformGroup(
                                            seqPlatformGroups: [createSeqPlatformGroup()],
                                    ),
                            ),
                    ],
                    [
                            fastqImportInstance: fastqImportInstance,
                            fileLinked         : true,
                    ],
            )
            createMergingCriteriaLazy(project: seqTrack1.project, seqType: seqTrack1.seqType)
            createMergingCriteriaLazy(project: seqTrack2.project, seqType: seqTrack2.seqType)
            RoddyBamFile abstractBamFile = DomainFactory.createRoddyBamFile([
                    workPackage: DomainFactory.createMergingWorkPackage(
                            MergingWorkPackage.getMergingProperties(seqTrack2) +
                                    [pipeline: DomainFactory.createPanCanPipeline()]
                    )
            ] + DomainFactory.randomBamFileProperties)

            abstractBamFile.seqTracks +=  [seqTrack2] + abstractBamFile.seqTracks
            abstractBamFile.numberOfMergedLanes = 2
            abstractBamFile.save(flush: true)

            saveBamFileInProjectFolder(abstractBamFile)

            ((MergingWorkPackage) (abstractBamFile.workPackage)).seqTracks.add(seqTrack2)
            abstractBamFile.workPackage.save(flush: true)
            ProcessingStatus expectedStatus = [
                    getInstallationProcessingStatus: { -> ALL_DONE },
                    getFastqcProcessingStatus      : { -> ALL_DONE },
                    getAlignmentProcessingStatus   : { -> PARTLY_DONE_WONT_DO_MORE },
                    getSnvProcessingStatus         : { -> NOTHING_DONE_WONT_DO },
            ] as ProcessingStatus

            String prefix = "the prefix"
            DomainFactory.createProcessingOptionForTicketPrefix(prefix)

            String notificationText1 = HelperUtils.uniqueString
            String notificationText2 = HelperUtils.uniqueString

            String expectedEmailSubjectOperator = "${prefix}#${ticket.ticketNumber} Final Processing Status Update"
            String expectedEmailSubjectCustomer = "[${prefix}#${ticket.ticketNumber}] TO BE SENT: ${seqTrack1.project.name} sequencing data "
            String expectedEmailSubjectCustomer1 = expectedEmailSubjectCustomer + Ticket.ProcessingStep.INSTALLATION.notificationSubject
            String expectedEmailSubjectCustomer2 = expectedEmailSubjectCustomer + Ticket.ProcessingStep.ALIGNMENT.notificationSubject

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                1 * sendEmailToTicketSystem(expectedEmailSubjectOperator, _) >> { String emailSubject, String content ->
                    assert content.contains(expectedStatus.toString())
                }
                1 * sendEmailToTicketSystem(expectedEmailSubjectCustomer1, notificationText1)
                1 * sendEmailToTicketSystem(expectedEmailSubjectCustomer2, notificationText2)
                0 * _
            }

            notificationCreator.createNotificationTextService = Mock(CreateNotificationTextService) {
                getMessageSourceService() >> Mock(MessageSourceService)
                1 * notification(ticket, _, Ticket.ProcessingStep.INSTALLATION, seqTrack1.project) >> notificationText1
                1 * notification(ticket, _, Ticket.ProcessingStep.ALIGNMENT, seqTrack1.project) >> notificationText2
                0 * notification(_, _, _, _)
                getLinkGenerator() >> Mock(LinkGenerator)
            }
        }

        when:
        SessionUtils.withTransaction {
            Ticket ticketToNotify = Ticket.get(ticket.id)
            notificationCreator.notifyAndSetFinishedTimestamps(ticketToNotify)
        }

        then:
        SessionUtils.withTransaction {
            Ticket result = Ticket.get(ticket.id)
            assert result.installationFinished != null
            assert result.fastqcFinished != null
            assert result.alignmentFinished != null
            assert result.snvFinished == null
            assert result.indelFinished == null
            assert result.sophiaFinished == null
            assert result.aceseqFinished == null
            assert result.runYapsaFinished == null
            assert result.finalNotificationSent
            return true
        }
    }

    void "setFinishedTimestampsAndNotify, when alignment is finished but installation is not, don't send notification"() {
        given:
        Ticket ticket
        setupData()
        SessionUtils.withTransaction {
            // Installation: finished timestamp not set, all done,     won't do more
            // FastQC:       finished timestamp not set, all done,     won't do more
            // Alignment:    finished timestamp not set, partly done,  won't do more
            // SNV:          finished timestamp not set, nothing done, won't do more
            ticket = createTicket()
            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket)
            SeqTrack seqTrack1 = createSeqTrackWithOneFastqFile(
                    [
                            dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                            fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                    ],
                    [fastqImportInstance: fastqImportInstance, fileLinked: true,],
            )
            SeqTrack seqTrack2 = createSeqTrackWithOneFastqFile(
                    [
                            sample               : createSample(individual: createIndividual(project: seqTrack1.project)),
                            dataInstallationState: SeqTrack.DataProcessingState.IN_PROGRESS,
                            fastqcState          : SeqTrack.DataProcessingState.NOT_STARTED,
                            run                  : createRun(
                                    seqPlatform: createSeqPlatformWithSeqPlatformGroup(
                                            seqPlatformGroups: [createSeqPlatformGroup()],
                                    ),
                            ),
                    ],
                    [
                            fastqImportInstance: fastqImportInstance,
                            fileLinked         : true,
                    ],
            )
            createMergingCriteriaLazy(project: seqTrack1.project, seqType: seqTrack1.seqType)
            createMergingCriteriaLazy(project: seqTrack2.project, seqType: seqTrack2.seqType)

            RoddyBamFile abstractBamFile = DomainFactory.createRoddyBamFile([
                    workPackage: DomainFactory.createMergingWorkPackage(
                            MergingWorkPackage.getMergingProperties(seqTrack2) +
                                    [pipeline: DomainFactory.createPanCanPipeline()]
                    )
            ] + DomainFactory.randomBamFileProperties)

            abstractBamFile.seqTracks +=  [seqTrack2] + abstractBamFile.seqTracks
            abstractBamFile.numberOfMergedLanes = 2
            abstractBamFile.save(flush: true)

            saveBamFileInProjectFolder(abstractBamFile)

            String prefix = "the prefix"
            DomainFactory.createProcessingOptionForTicketPrefix(prefix)

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                0 * _
            }

            notificationCreator.createNotificationTextService = Mock(CreateNotificationTextService) {
                0 * notification(_, _, _, _)
            }
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.notifyAndSetFinishedTimestamps(ticket)
        }

        then:
        ticket.installationFinished == null
        ticket.alignmentFinished == null
        ticket.fastqcFinished == null
        ticket.snvFinished == null
        ticket.indelFinished == null
        ticket.sophiaFinished == null
        ticket.aceseqFinished == null
        ticket.runYapsaFinished == null
        !ticket.finalNotificationSent
    }

    @SuppressWarnings('Indentation')
    @Unroll
    void 'sendCustomerNotification sends expected notification'(int dataCase, boolean automaticNotification, boolean processingNotification,
                                                                Ticket.ProcessingStep notificationStep, List<String> recipients, String subject) {
        given:
        Ticket ticket
        ProcessingStatus status
        setupData()
        SessionUtils.withTransaction {
            UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                    project: createProject(name: 'Project1', processingNotification: processingNotification),
                    user: DomainFactory.createUser(email: EMAIL),
            )
            Sample sample1 = createSample(
                    individual: createIndividual(
                            project: userProjectRole.project,
                    )
            )
            Sample sample2 = createSample(
                    individual: createIndividual(
                            project: createProject(
                                    name: 'Project2',
                            )
                    )
            )

            Collection<SeqTrack> seqTracks
            switch (dataCase) {
                case 1:
                    seqTracks = [
                            createSeqTrack(sample: sample1),
                    ]
                    break
                case 2:
                    seqTracks = [
                            createSeqTrack(sample: sample1),
                    ]
                    break
                case 3:
                    seqTracks = [
                            createSeqTrack(sample: sample1),
                            createSeqTrack(
                                    sample: sample1,
                                    ilseSubmission: createIlseSubmission(ilseNumber: 1234),
                            ),
                    ]
                    break
                case 4:
                    seqTracks = [
                            createSeqTrack(sample: sample1),
                    ]
                    break
                case 5:
                    seqTracks = [
                            createSeqTrack(sample: sample2),
                    ]
                    break
                case 6:
                    IlseSubmission ilse = createIlseSubmission(ilseNumber: 9876)
                    seqTracks = [
                            createSeqTrack(
                                    sample: sample2,
                                    ilseSubmission: ilse,
                            ),
                            createSeqTrack(
                                    sample: sample2,
                                    ilseSubmission: ilse,
                            ),
                            createSeqTrack(
                                    sample: sample2,
                                    ilseSubmission: createIlseSubmission(ilseNumber: 1234),
                            ),
                            createSeqTrack(sample: sample2),
                    ]
                    break
                case 7:
                    seqTracks = [
                            createSeqTrack(sample: sample1),
                    ]
                    break
            }

            ticket = createTicket(automaticNotification: automaticNotification)
            status = new ProcessingStatus(seqTracks.collect { new SeqTrackProcessingStatus(it) })
            String prefix = HelperUtils.uniqueString
            DomainFactory.createProcessingOptionForTicketPrefix(prefix)
            String finalSubject = "[${prefix}#${ticket.ticketNumber}] ${subject}"
            int callCount = subject ? 1 : 0
            String content = HelperUtils.randomEmail

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                if (recipients.isEmpty() && callCount == 1) {
                    1 * sendEmailToTicketSystem(finalSubject, content)
                } else if (callCount == 1) {
                    1 * sendEmail(finalSubject, content, recipients)
                }
                0 * sendEmailToTicketSystem(_, _)
                0 * sendEmail(_, _, _)
            }

            notificationCreator.createNotificationTextService = Mock(CreateNotificationTextService) {
                Project project = exactlyOneElement(seqTracks*.project.unique())
                callCount * notification(ticket, _, notificationStep, project) >> {
                    Ticket ticket1, ProcessingStatus status1, Ticket.ProcessingStep processingStep, Project project1 ->
                        TestCase.assertContainSame(status.seqTrackProcessingStatuses, status1.seqTrackProcessingStatuses)
                        return content
                }
            }
        }

        expect:
        SessionUtils.withTransaction {
            notificationCreator.sendCustomerNotification(ticket, status, notificationStep)
            return true
        }

        where:
        dataCase | automaticNotification | processingNotification | notificationStep                       | recipients | subject
        1        | true                  | true                   | Ticket.ProcessingStep.INSTALLATION | [EMAIL] | 'Project1 sequencing data installed'
        2        | false                 | true                   | Ticket.ProcessingStep.INSTALLATION | []      | 'TO BE SENT: Project1 sequencing data installed'
        3        | true                  | true                   | Ticket.ProcessingStep.INSTALLATION | [EMAIL] | '[S#1234] Project1 sequencing data installed'
        4        | true                  | true                   | Ticket.ProcessingStep.FASTQC       | []      | null
        5        | true                  | true                   | Ticket.ProcessingStep.ALIGNMENT    | []      | 'TO BE SENT: Project2 sequencing data aligned'
        6        | true                  | true                   | Ticket.ProcessingStep.SNV          | []      | 'TO BE SENT: [S#1234,9876] Project2 sequencing data SNV-called'
        7        | true                  | false                  | Ticket.ProcessingStep.INSTALLATION | []      | 'TO BE SENT: Project1 sequencing data installed'
    }

    void 'sendCustomerNotification, with multiple projects, sends multiple notifications'() {
        given:
        Ticket ticket
        ProcessingStatus status
        setupData()
        SessionUtils.withTransaction {
            ticket = createTicket()
            status = new ProcessingStatus([1, 2].collect { int index ->
                UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                        user: DomainFactory.createUser(email: "project${index}@test.com"),
                        project: createProject(name: "Project_X${index}"),
                )
                new SeqTrackProcessingStatus(createSeqTrack(
                        sample: createSample(
                                individual: createIndividual(
                                        project: userProjectRole.project,
                                )
                        )
                ))
            })
            String prefix = HelperUtils.uniqueString
            DomainFactory.createProcessingOptionForTicketPrefix(prefix)

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                1 * sendEmail("[${prefix}#${ticket.ticketNumber}] Project_X1 sequencing data installed",
                        'Project_X1', ['project1@test.com'])
                1 * sendEmail("[${prefix}#${ticket.ticketNumber}] Project_X2 sequencing data installed",
                        'Project_X2', ['project2@test.com'])
                0 * sendEmail(_, _, _)
            }
            notificationCreator.createNotificationTextService = Mock(CreateNotificationTextService) {
                notification(ticket, _, Ticket.ProcessingStep.INSTALLATION, _) >> { Ticket ticket1, ProcessingStatus status1,
                                                                                    Ticket.ProcessingStep processingStep, Project project ->
                    return project.name
                }
            }
        }

        expect:
        SessionUtils.withTransaction {
            notificationCreator.sendCustomerNotification(ticket, status, Ticket.ProcessingStep.INSTALLATION)
            return true
        }
    }

    void 'sendCustomerNotification, with multiple projects, when non are finished, doesnt send emails'() {
        given:
        Ticket ticket
        ProcessingStatus status
        setupData()
        SessionUtils.withTransaction {
            ticket = createTicket()
            status = new ProcessingStatus([1, 2].collect { int index ->
                UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                        user: DomainFactory.createUser(email: "project${index}@test.com"),
                        project: createProject(name: "Project_X${index}"),
                )
                new SeqTrackProcessingStatus(createSeqTrack(
                        sample: createSample(
                                individual: createIndividual(
                                        project: userProjectRole.project,
                                )
                        ),
                ))
            })

            String prefix = HelperUtils.uniqueString
            DomainFactory.createProcessingOptionForTicketPrefix(prefix)

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                0 * sendEmail(_, _, _)
            }
        }

        expect:
        SessionUtils.withTransaction {
            notificationCreator.sendCustomerNotification(ticket, status, Ticket.ProcessingStep.INSTALLATION)
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
        SessionUtils.withTransaction {
            assert notificationCreator.fillInMergingWorkPackageProcessingStatuses([]).empty
            return true
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 ST not alignable, returns NOTHING_DONE_WONT_DO"() {
        given:
        SeqTrackProcessingStatus seqTrackStatus
        ProcessingStatus processingStatus
        setupData()
        SessionUtils.withTransaction {
            seqTrackStatus = createSeqTrackProcessingStatus(createSeqTrackWithOneFastqFile([:], [fileWithdrawn: true]))
            processingStatus = createProcessingStatus(seqTrackStatus)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
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
        AbstractBamFile bamFile
        SeqTrackProcessingStatus seqTrackStatus
        setupData()
        SessionUtils.withTransaction {
            bamFile = createBamFileInProjectFolder(DomainFactory.randomBamFileProperties)
            seqTrackStatus = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
        }

        then:
        SessionUtils.withTransaction {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
            assert mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
            assert mwpStatus.completeProcessableBamFileInProjectFolder == bamFile
            assert mwpStatus.alignmentProcessingStatus == ALL_DONE
            assert seqTrackStatus.alignmentProcessingStatus == ALL_DONE
            assert createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == ALL_DONE
            return true
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 MWP NOTHING_DONE_MIGHT_DO, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        AbstractBamFile bamFile
        SeqTrackProcessingStatus seqTrackStatus
        setupData()
        SessionUtils.withTransaction {
            bamFile = createBamFileInProjectFolder(DomainFactory.randomBamFileProperties)
            seqTrackStatus = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithFastqFiles(bamFile.mergingWorkPackage))
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
        }

        then:
        SessionUtils.withTransaction {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
            assert mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
            assert mwpStatus.completeProcessableBamFileInProjectFolder == null
            assert mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            assert seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            assert createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            return true
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 1 ST not alignable, rest merged, returns NOTHING_DONE_WONT_DO_MORE and ALL_DONE"() {
        given:
        SeqTrackProcessingStatus seqTrack1Status
        SeqTrackProcessingStatus seqTrack2Status
        AbstractBamFile bamFile
        setupData()
        SessionUtils.withTransaction {
            bamFile = createBamFileInProjectFolder(DomainFactory.randomBamFileProperties)
            Set<SeqTrack> seqTracks = new HashSet<SeqTrack>(((MergingWorkPackage) (bamFile.workPackage)).seqTracks)
            seqTrack1Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithFastqFiles(bamFile.mergingWorkPackage, [:], [fileWithdrawn: true]))
            ((MergingWorkPackage) (bamFile.workPackage)).seqTracks = seqTracks
            bamFile.workPackage.save(flush: true)
            seqTrack2Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status])
        }

        then:
        SessionUtils.withTransaction {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack2Status.mergingWorkPackageProcessingStatuses)
            assert seqTrack1Status.mergingWorkPackageProcessingStatuses.isEmpty()
            assert seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
            assert mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
            assert mwpStatus.completeProcessableBamFileInProjectFolder == bamFile
            assert mwpStatus.alignmentProcessingStatus == ALL_DONE
            assert seqTrack2Status.alignmentProcessingStatus == ALL_DONE
            assert createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_WONT_DO_MORE
            return true
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, no MWP, returns NOTHING_DONE_WONT_DO"() {
        given:
        SeqTrackProcessingStatus seqTrackStatus
        ProcessingStatus processingStatus
        setupData()
        SessionUtils.withTransaction {
            seqTrackStatus = createSeqTrackProcessingStatus(createSeqTrackWithOneFastqFile())
            processingStatus = createProcessingStatus(seqTrackStatus)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
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
        AbstractBamFile bamFile
        SeqTrackProcessingStatus seqTrackStatus
        setupData()
        SessionUtils.withTransaction {
            bamFile = createBamFile()
            seqTrackStatus = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrackStatus])
        }

        then:
        SessionUtils.withTransaction {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrackStatus.mergingWorkPackageProcessingStatuses)
            assert mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
            assert mwpStatus.completeProcessableBamFileInProjectFolder == null
            assert mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            assert seqTrackStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            assert createProcessingStatus(seqTrackStatus).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            return true
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP ALL_DONE, returns PARTLY_DONE_WONT_DO_MORE"() {
        given:
        AbstractBamFile bamFile
        SeqTrackProcessingStatus seqTrack1Status, seqTrack2Status
        setupData()

        SessionUtils.withTransaction {
            bamFile = createBamFileInProjectFolder(DomainFactory.randomBamFileProperties)
            seqTrack1Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
            seqTrack2Status = createSeqTrackProcessingStatus(createSeqTrackWithOneFastqFile())
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status])
        }

        then:
        SessionUtils.withTransaction {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
            assert mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
            assert mwpStatus.completeProcessableBamFileInProjectFolder == bamFile
            assert mwpStatus.alignmentProcessingStatus == ALL_DONE
            assert seqTrack1Status.alignmentProcessingStatus == ALL_DONE
            assert seqTrack2Status.mergingWorkPackageProcessingStatuses.isEmpty()
            assert seqTrack2Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
            assert createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_WONT_DO_MORE
            return true
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP in progress, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        AbstractBamFile bamFile
        SeqTrackProcessingStatus seqTrack1Status, seqTrack2Status
        setupData()
        SessionUtils.withTransaction {
            bamFile = createBamFile()
            seqTrack1Status = createSeqTrackProcessingStatus(bamFile.containedSeqTracks.first())
            seqTrack2Status = createSeqTrackProcessingStatus(createSeqTrackWithOneFastqFile())
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status])
        }

        then:
        SessionUtils.withTransaction {
            MergingWorkPackageProcessingStatus mwpStatus = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
            assert mwpStatus.mergingWorkPackage == bamFile.mergingWorkPackage
            assert mwpStatus.completeProcessableBamFileInProjectFolder == null
            assert mwpStatus.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            assert seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            assert seqTrack2Status.mergingWorkPackageProcessingStatuses.isEmpty()
            assert seqTrack2Status.alignmentProcessingStatus == NOTHING_DONE_WONT_DO
            assert createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            return true
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, 2 MergingProperties, 1 MWP NOTHING_DONE_MIGHT_DO, 1 MWP ALL_DONE, returns PARTLY_DONE_MIGHT_DO_MORE"() {
        given:
        setupData()

        AbstractBamFile bamFile1, bamFile2
        SeqTrackProcessingStatus seqTrack1Status, seqTrack2Status, seqTrack3Status

        SessionUtils.withTransaction {
            bamFile1 = createBamFileInProjectFolder(DomainFactory.randomBamFileProperties)
            bamFile2 = createBamFileInProjectFolder(DomainFactory.randomBamFileProperties)
            seqTrack1Status = createSeqTrackProcessingStatus(bamFile1.containedSeqTracks.first())
            seqTrack2Status = createSeqTrackProcessingStatus(bamFile2.containedSeqTracks.first())
            seqTrack3Status = createSeqTrackProcessingStatus(DomainFactory.createSeqTrackWithFastqFiles(bamFile1.mergingWorkPackage))
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInMergingWorkPackageProcessingStatuses([seqTrack1Status, seqTrack2Status, seqTrack3Status])
        }

        then:
        SessionUtils.withTransaction {
            MergingWorkPackageProcessingStatus mwp1Status = exactlyOneElement(seqTrack1Status.mergingWorkPackageProcessingStatuses)
            MergingWorkPackageProcessingStatus mwp2Status = exactlyOneElement(seqTrack2Status.mergingWorkPackageProcessingStatuses)

            assert mwp1Status.mergingWorkPackage == bamFile1.mergingWorkPackage
            assert mwp1Status.completeProcessableBamFileInProjectFolder == null
            assert mwp1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            assert seqTrack1Status.alignmentProcessingStatus == NOTHING_DONE_MIGHT_DO
            assert mwp2Status.mergingWorkPackage == bamFile2.mergingWorkPackage
            assert mwp2Status.completeProcessableBamFileInProjectFolder == bamFile2
            assert mwp2Status.alignmentProcessingStatus == ALL_DONE
            assert seqTrack2Status.alignmentProcessingStatus == ALL_DONE
            assert exactlyOneElement(seqTrack3Status.mergingWorkPackageProcessingStatuses).is(mwp1Status)
            assert createProcessingStatus(seqTrack1Status, seqTrack2Status).alignmentProcessingStatus == PARTLY_DONE_MIGHT_DO_MORE
            return true
        }
    }

    private static MergingWorkPackageProcessingStatus createMergingWorkPackageProcessingStatus(
            MergingWorkPackage mergingWorkPackage, WorkflowProcessingStatus processingStatus) {
        return new MergingWorkPackageProcessingStatus(mergingWorkPackage, processingStatus, null, [])
    }

    private MergingWorkPackageProcessingStatus createMergingWorkPackageProcessingStatus(AbstractBamFile bamFile) {
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
        notificationCreator.fillInSamplePairStatuses([])
    }

    void "fillInSamplePairStatuses, no SP, returns NOTHING_DONE_WONT_DO"() {
        given:
        setupData()

        MergingWorkPackageProcessingStatus mwpStatus
        SessionUtils.withTransaction {
            mwpStatus = createMergingWorkPackageProcessingStatus(DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
            assert mwpStatus.samplePairProcessingStatuses.isEmpty()
            assert mwpStatus.snvProcessingStatus == NOTHING_DONE_WONT_DO
            assert mwpStatus.indelProcessingStatus == NOTHING_DONE_WONT_DO
            assert mwpStatus.sophiaProcessingStatus == NOTHING_DONE_WONT_DO
            assert mwpStatus.aceseqProcessingStatus == NOTHING_DONE_WONT_DO
            assert mwpStatus.runYapsaProcessingStatus == NOTHING_DONE_WONT_DO
            assert createSeqTrackProcessingStatus(mwpStatus).snvProcessingStatus == NOTHING_DONE_WONT_DO
            return true
        }
    }

    @Unroll("fillInSamplePairStatuses, #pairAnalysis.analysisType withdrawn, returns NOTHING_DONE_WONT_DO")
    void "fillInSamplePairStatuses, analysisInstance withdrawn, returns NOTHING_DONE_WONT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                saveBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withTransaction {
            analysisInstance = BamFilePairAnalysis.get(analysisInstance.id)
            analysisInstance.withdrawn = true
            analysisInstance.save(flush: true)
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"()
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                saveBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
            analysisInstance.delete(flush: true)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            assert samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO
            assert mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO
            assert createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO
            return true
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"([:], [coverage: 2], [coverage: 2])
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                saveBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
                DomainFactory.createProcessingThresholdsForBamFile(analysisInstance."sampleType${it}BamFile", [coverage: 1, numberOfLanes: null])
            }

            referenceGenomeProcessingOptions.each {
                it.value = analysisInstance.samplePair.mergingWorkPackage1.referenceGenome.name
                it.save(flush: true)
            }

            switch (pairAnalysis.analysisType) {
                case Ticket.ProcessingStep.ACESEQ:
                    analysisInstance.samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
                    analysisInstance.samplePair.save(flush: true)
                    DomainFactory.createSophiaInstance(analysisInstance.samplePair)
                    break
                case Ticket.ProcessingStep.RUN_YAPSA:
                    analysisInstance.samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
                    analysisInstance.samplePair.save(flush: true)
                    DomainFactory.createRoddySnvInstanceWithRoddyBamFiles([samplePair: analysisInstance.samplePair])
                    break
            }

            analysisInstance.delete(flush: true)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            assert samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory.createAceseqInstanceWithRoddyBamFiles([:], [coverage: 2], [coverage: 2])
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)

            [1, 2].each {
                saveBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
                DomainFactory.createProcessingThresholdsForBamFile(analysisInstance."sampleType${it}BamFile", [coverage: 1, numberOfLanes: null])
            }

            referenceGenomeProcessingOptions.each {
                it.value = analysisInstance.samplePair.mergingWorkPackage1.referenceGenome.name
                it.save(flush: true)
            }

            analysisInstance.delete(flush: true)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus.completeAceseqInstance == null
            assert samplePairStatus.aceseqProcessingStatus == NOTHING_DONE_WONT_DO
            assert mwpStatus.aceseqProcessingStatus == NOTHING_DONE_WONT_DO
            assert createSeqTrackProcessingStatus(mwpStatus).aceseqProcessingStatus == NOTHING_DONE_WONT_DO
            return true
        }
    }

    @Unroll("fillInSamplePairStatuses, no #pairAnalysis.analysisType, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO")
    void "fillInSamplePairStatuses, no analysisInstance, bamFileInProjectFolder unset, returns NOTHING_DONE_MIGHT_DO"() {
        given:
        setupData()

        BamFilePairAnalysis analysisInstance
        MergingWorkPackageProcessingStatus mwpStatus

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"()
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile.mergingWorkPackage, NOTHING_DONE_MIGHT_DO)
            analysisInstance.delete(flush: true)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            assert samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            return true
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                saveBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == analysisInstance
            assert samplePairStatus."${pairAnalysis.processingStatus}" == ALL_DONE
            assert mwpStatus."${pairAnalysis.processingStatus}" == ALL_DONE
            assert createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == ALL_DONE
            return true
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile.mergingWorkPackage, NOTHING_DONE_MIGHT_DO)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            assert samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            return true
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"()
            mwpStatus = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            [1, 2].each {
                saveBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwpStatus])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwpStatus.samplePairProcessingStatuses)
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            assert samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert mwpStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert createSeqTrackProcessingStatus(mwpStatus)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            return true
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwp1Status = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            mwp2Status = createMergingWorkPackageProcessingStatus(DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)
            [1, 2].each {
                saveBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwp1Status, mwp2Status])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwp1Status.samplePairProcessingStatuses)
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == analysisInstance
            assert samplePairStatus."${pairAnalysis.processingStatus}" == ALL_DONE
            assert mwp1Status."${pairAnalysis.processingStatus}" == ALL_DONE
            assert mwp2Status.samplePairProcessingStatuses.isEmpty()
            assert mwp2Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO
            assert createSeqTrackProcessingStatus(mwp1Status, mwp2Status)."${pairAnalysis.processingStatus}" == PARTLY_DONE_WONT_DO_MORE
            return true
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwp1Status = createMergingWorkPackageProcessingStatus(DomainFactory.createMergingWorkPackage(), NOTHING_DONE_MIGHT_DO)
            mwp2Status = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwp1Status, mwp2Status])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePairStatus = exactlyOneElement(mwp2Status.samplePairProcessingStatuses)

            assert mwp1Status.samplePairProcessingStatuses.isEmpty()
            assert mwp1Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_WONT_DO
            assert samplePairStatus.samplePair == analysisInstance.samplePair
            assert samplePairStatus."${pairAnalysis.completeCallingInstance}" == null
            assert samplePairStatus."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert mwp2Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert createSeqTrackProcessingStatus(mwp1Status, mwp2Status)."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            return true
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

        SessionUtils.withTransaction {
            analysisInstance = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            analysisInstance2 = DomainFactory."${pairAnalysis.createRoddyBamFile}"(processingState: AnalysisProcessingStates.FINISHED)
            mwp1Status = createMergingWorkPackageProcessingStatus(analysisInstance.sampleType1BamFile)
            mwp2Status = createMergingWorkPackageProcessingStatus(analysisInstance2.sampleType1BamFile)
            [1, 2].each {
                saveBamFileInProjectFolder(analysisInstance."sampleType${it}BamFile")
            }
        }

        when:
        SessionUtils.withTransaction {
            notificationCreator.fillInSamplePairStatuses([mwp1Status, mwp2Status])
        }

        then:
        SessionUtils.withTransaction {
            SamplePairProcessingStatus samplePair1Status = exactlyOneElement(mwp1Status.samplePairProcessingStatuses)
            SamplePairProcessingStatus samplePair2Status = exactlyOneElement(mwp2Status.samplePairProcessingStatuses)

            assert samplePair1Status.samplePair == analysisInstance.samplePair
            assert samplePair1Status."${pairAnalysis.completeCallingInstance}" == analysisInstance
            assert samplePair1Status."${pairAnalysis.processingStatus}" == ALL_DONE
            assert mwp1Status."${pairAnalysis.processingStatus}" == ALL_DONE
            assert samplePair2Status.samplePair == analysisInstance2.samplePair
            assert samplePair2Status."${pairAnalysis.completeCallingInstance}" == null
            assert samplePair2Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert mwp2Status."${pairAnalysis.processingStatus}" == NOTHING_DONE_MIGHT_DO
            assert createSeqTrackProcessingStatus(mwp1Status, mwp2Status)."${pairAnalysis.processingStatus}" == PARTLY_DONE_MIGHT_DO_MORE
            return true
        }

        where:
        pairAnalysis << listPairAnalysis
    }

    private AbstractBamFile createBamFileInProjectFolder(Map bamFileProperties = [:]) {
        AbstractBamFile bamFile = createBamFile(bamFileProperties)

        return saveBamFileInProjectFolder(bamFile)
    }

    private static AbstractBamFile saveBamFileInProjectFolder(AbstractBamFile bamFile) {
        bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
        bamFile.mergingWorkPackage.save(flush: true)

        return bamFile
    }

    void "fillInMergingWorkPackageProcessingStatuses, when a SeqTrack is added manually to an existing MergingWorkPackage, then find the MergingWorkPackage for this SeqTrack"() {
        given: "two seqtracks, with different merging properties, but manually combined in the same MergingWorkPackage"
        setupData()

        SeqTrackProcessingStatus statusB
        MergingWorkPackage mergePackageA

        SessionUtils.withTransaction {
            // shared
            Sample sharedSample = createSample()
            SeqType sharedSeqType = createSeqType()

            // seqtrack A
            SeqPlatformGroup groupA = createSeqPlatformGroup()
            SeqTrack seqTrackA = createSeqTrack(sharedSample, sharedSeqType, groupA)
            new SeqTrackProcessingStatus(seqTrackA,
                    ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                    ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                    []
            )
            // seqtrack B
            SeqPlatformGroup groupB = createSeqPlatformGroup()
            SeqTrack seqTrackB = createSeqTrack(sharedSample, sharedSeqType, groupB)
            statusB = new SeqTrackProcessingStatus(seqTrackB,
                    ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE,
                    ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE,
                    []
            )
            // "existing" merge group
            createMergingCriteriaLazy(project: sharedSample.project, seqType: sharedSeqType)
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
        SessionUtils.withTransaction {
            statuses = notificationCreator.fillInMergingWorkPackageProcessingStatuses([statusB])
        }

        then: "we should find A's merging package"
        SessionUtils.withTransaction {
            assert statuses*.mergingWorkPackage == [mergePackageA]
            return true
        }
    }

    void "fillInMergingWorkPackageProcessingStatuses, when a SeqTrack is removed manually from an existing MergingWorkPackage, then do not find the MergingWorkPackage for this SeqTrack"() {
        given: "two seqtracks, with same merging properties, but one manually removed from the MergingWorkPackage"
        setupData()

        SeqTrackProcessingStatus statusB

        SessionUtils.withTransaction {
            // shared
            Sample sharedSample = createSample()
            SeqType sharedSeqType = createSeqType()

            // seqtrack A
            SeqPlatformGroup groupA = createSeqPlatformGroup()
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
            createMergingCriteriaLazy(project: sharedSample.project, seqType: sharedSeqType)
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
        SessionUtils.withTransaction {
            statuses = notificationCreator.fillInMergingWorkPackageProcessingStatuses([statusB])
        }

        then: "we should not find a merging package"
        SessionUtils.withTransaction {
            assert statuses.empty
            return true
        }
    }

    SeqTrack createSeqTrack(Sample sample, SeqType seqType, SeqPlatformGroup groupA) {
        SeqTrack seqTrackA = createSeqTrackWithOneFastqFile([
                sample : sample,
                seqType: seqType,
                run    : createRun(
                        seqPlatform: createSeqPlatformWithSeqPlatformGroup(
                                seqPlatformGroups: [groupA],
                        ),
                ),
        ])
        return seqTrackA
    }

    void 'sendProcessingStatusOperatorNotification, when finalNotification is true and no Ilse, sends final notification with correct subject and to project list'() {
        given:
        setupData()

        Ticket ticket
        UserProjectRole userProjectRole
        SeqTrack seqTrack

        SessionUtils.withTransaction {
            ticket = createTicket()
            userProjectRole = DomainFactory.createUserProjectRole(project: createProject())
            seqTrack = createSeqTrackWithOneFastqFile(
                    [
                            ilseSubmission: null,
                            sample        : createSample(
                                    individual: createIndividual(
                                            project: userProjectRole.project
                                    )
                            ),
                    ],
                    [fastqImportInstance: createFastqImportInstance(ticket: ticket), fileLinked: true])
            DomainFactory.createProcessingOptionForTicketPrefix(PREFIX)
            String prefix = ticketService.getPrefixedTicketNumber(ticket)
            String expectedHeader = "${prefix} Final Processing Status Update"
            notificationCreator.mailHelperService = Mock(MailHelperService) {
                1 * sendEmailToTicketSystem(expectedHeader, _)
            }
        }

        expect:
        SessionUtils.withTransaction {
            notificationCreator.sendProcessingStatusOperatorNotification(ticket, [seqTrack] as Set, new ProcessingStatus(), true)
            return true
        }
    }

    ProcessingOption setupBlacklistImportSourceNotificationProcessingOption(String blacklist) {
        return DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.BLACKLIST_IMPORT_SOURCE_NOTIFICATION,
                type: null,
                project: null,
                value: blacklist,
        )
    }

    void "sendImportSourceOperatorNotification, no blacklisted paths"() {
        given:
        setupData()

        Ticket ticket

        SessionUtils.withTransaction {
            DomainFactory.createProcessingOptionForTicketPrefix(PREFIX)
            setupBlacklistImportSourceNotificationProcessingOption("")

            ticket = createTicket()

            RawSequenceFile rawSequenceFile = createFastqFile()
            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [rawSequenceFile])

            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance)

            String prefix = ticketService.getPrefixedTicketNumber(ticket)
            String expectedHeader = "Import source ready for deletion [${prefix}]"

            String expectedEnd = [
                    rawSequenceFile.fullInitialPath,
            ].collect { "rm -f ${it}" }.join("\n")

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                1 * sendEmailToTicketSystem(expectedHeader) { it.endsWith(expectedEnd) }
            }
        }

        expect:
        SessionUtils.withTransaction {
            notificationCreator.sendImportSourceOperatorNotification(ticket)
            return true
        }
    }

    void "sendImportSourceOperatorNotification, does not send a mail when all paths are filtered out"() {
        given:
        setupData()

        Ticket ticket

        SessionUtils.withTransaction {
            DomainFactory.createProcessingOptionForTicketPrefix(PREFIX)

            String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("/blacklisted").value
            ticket = createTicket()

            FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [
                    createFastqFile(initialDirectory: "${blacklisted}"),
                    createFastqFile(initialDirectory: "${blacklisted}"),
            ])
            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstance, filePathSource: "${blacklisted}")

            notificationCreator.mailHelperService = Mock(MailHelperService) {
                0 * sendEmail(_, _, _)
            }
        }

        expect:
        SessionUtils.withTransaction {
            notificationCreator.sendImportSourceOperatorNotification(ticket)
            return true
        }
    }

    void "getPathsToDelete returns the paths of all DataFiles associated with the ticket"() {
        given:
        Ticket ticket
        List<String> expected = []

        SessionUtils.withTransaction {
            ticket = createTicket()

            List<RawSequenceFile> rawSequenceFilesA = [createFastqFile(), createFastqFile()]
            FastqImportInstance fastqImportInstanceA = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesA)

            List<RawSequenceFile> rawSequenceFilesB = [createFastqFile(), createFastqFile(), createFastqFile()]
            FastqImportInstance fastqImportInstanceB = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesB)

            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceA)
            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceB)

            expected.addAll(rawSequenceFilesA*.fullInitialPath)
            expected.addAll(rawSequenceFilesB*.fullInitialPath)
        }

        expect:
        SessionUtils.withTransaction {
            assert notificationCreator.getPathsToDelete(ticket).sort() == expected.sort()
            return true
        }
    }

    void "getPathsToDelete leaves out blacklisted paths"() {
        given:
        Ticket ticket
        List<String> expected = []

        SessionUtils.withTransaction {
            String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("/blacklisted").value

            ticket = createTicket()

            List<RawSequenceFile> rawSequenceFilesA = [createFastqFile(), createFastqFile()]
            FastqImportInstance fastqImportInstanceA = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesA)

            Closure<RawSequenceFile> createBlacklistedRawSequenceFile = {
                createFastqFile(initialDirectory: "${blacklisted}/path/dataFile")
            }
            List<RawSequenceFile> rawSequenceFilesB = [createFastqFile()]
            List<RawSequenceFile> rawSequenceFilesBBlacklisted = [createBlacklistedRawSequenceFile(), createBlacklistedRawSequenceFile()]
            FastqImportInstance fastqImportInstanceB = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFilesB + rawSequenceFilesBBlacklisted)

            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceA, filePathSource: "${blacklisted}/path/metaDataFile")
            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceB)

            expected.addAll(rawSequenceFilesA*.fullInitialPath)
            expected.addAll(rawSequenceFilesB*.fullInitialPath)
        }

        expect:
        SessionUtils.withTransaction {
            assert notificationCreator.getPathsToDelete(ticket).sort() == expected.sort()
            return true
        }
    }

    void "getPathsToDelete returns empty list if all paths are blacklisted"() {
        given:
        Ticket ticket

        SessionUtils.withTransaction {
            String blacklisted = setupBlacklistImportSourceNotificationProcessingOption("/blacklisted").value

            ticket = createTicket()

            Closure<RawSequenceFile> createBlacklistedRawSequenceFile = {
                createFastqFile(initialDirectory: "${blacklisted}/path/dataFile")
            }

            FastqImportInstance fastqImportInstanceA = createFastqImportInstance(ticket: ticket, sequenceFiles: [
                    createBlacklistedRawSequenceFile(),
            ])

            FastqImportInstance fastqImportInstanceB = createFastqImportInstance(ticket: ticket, sequenceFiles: [
                    createBlacklistedRawSequenceFile(),
                    createBlacklistedRawSequenceFile(),
            ])

            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceA, filePathSource: "${blacklisted}/path/metaDataFile")
            DomainFactory.createMetaDataFile(fastqImportInstance: fastqImportInstanceB, filePathSource: "${blacklisted}/path/metaDataFile")
        }

        expect:
        SessionUtils.withTransaction {
            assert notificationCreator.getPathsToDelete(ticket).sort() == []
            return true
        }
    }
}
