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
package de.dkfz.tbi.otp.workflow.alignment

import grails.test.hibernate.HibernateSpec
import grails.web.mapping.LinkGenerator

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.withdraw.RoddyBamFileWithdrawService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

import java.time.LocalDate

class TriggerAlignmentServiceSpec extends HibernateSpec implements IsRoddy, WorkflowSystemDomainFactory {

    TriggerAlignmentService service

    @Override
    List<Class> getDomainClasses() {
        return [
                AbstractBamFile,
                BamFilePairAnalysis,
                ExternallyProcessedBamFile,
                RawSequenceFile,
                FastqFile,
                Individual,
                MergingWorkPackage,
                Project,
                ReferenceGenomeSelector,
                RoddyBamFile,
                Sample,
                SampleType,
                SeqTrack,
                SeqType,
                Workflow,
                WorkflowArtefact,
                WorkflowRun,
                WorkflowVersionSelector,
        ]
    }

    void setup() {
        service = new TriggerAlignmentService()
    }

    void "run triggerAlignment, which should trigger one workflow"() {
        given:
        final SeqType st1 = createSeqTypePaired()
        final SeqType st2 = createSeqTypePaired()
        final SeqType st3 = createSeqTypePaired()

        Project project = createProject()
        Individual individual = createIndividual(project: project)

        Workflow wf = createWorkflow([
                defaultSeqTypesForWorkflowVersions: [st1, st2]
        ])
        WorkflowRun run = createWorkflowRun([
                workflow: wf,
                project : project,
        ])

        WorkflowArtefact workflowArtefact1 = createWorkflowArtefact([
                producedBy: run,
        ])
        WorkflowArtefact workflowArtefact2 = createWorkflowArtefact([
                producedBy: run
        ])

        SeqTrack seqTrack1 = createSeqTrackWithTwoFastqFile([
                sample          : createSample(individual: individual),
                seqType         : st1,
                workflowArtefact: workflowArtefact1,
        ])
        SeqTrack seqTrack2 = createSeqTrackWithTwoFastqFile([
                sample          : createSample(individual: individual),
                seqType         : st2,
                workflowArtefact: workflowArtefact2,
        ])

        SeqTrack seqTrack3 = createSeqTrackWithTwoFastqFile([
                seqType: st3,
        ])

        WorkflowArtefact outputArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.BAM,
                outputRole  : PanCancerWorkflow.OUTPUT_BAM,
                producedBy  : run,
        ])
        DeciderResult deciderResultToReturn = new DeciderResult()
        deciderResultToReturn.newArtefacts << outputArtefact

        MergingWorkPackage mergingWorkPackage1 = createMergingWorkPackage()
        RoddyBamFile bamFile1 = createRoddyBamFile([
                workflowArtefact: outputArtefact,
                workPackage     : mergingWorkPackage1,
                seqTracks       : [seqTrack1, seqTrack2],
        ], RoddyBamFile)

        // Mock service for workflow system
        service.allDecider = Mock(AllDecider) {
            1 * decide(_, _) >> deciderResultToReturn
            1 * findAlignableSeqTracks(_) >> [seqTrack1, seqTrack2]
            0 * _
        }

        // Check resetting tickets works
        service.ticketService = Mock(TicketService) {
            1 * findAllTickets(_) >> [createTicket(), createTicket()]
            2 * resetAlignmentAndAnalysisNotification(_)
        }

        // Make sure sample pairs are created
        service.samplePairDeciderService = Mock(SamplePairDeciderService) {
            1 * findOrCreateSamplePairs([mergingWorkPackage1])
        }

        service.roddyBamFileWithdrawService = Mock(RoddyBamFileWithdrawService) {
            _ * collectObjects(_) >> {
                return [bamFile1]
            }
        }

        when:
        TriggerAlignmentResult triggerAlignmentResult = service.triggerAlignment([seqTrack1, seqTrack2, seqTrack3] as Set, true, true)

        then:
        triggerAlignmentResult.newArtefacts.size() == 1
        TestCase.assertContainSame(triggerAlignmentResult.mergingWorkPackages, [mergingWorkPackage1])

        bamFile1.withdrawn
    }

    void "createWarningsForMissingAlignmentConfig, when run, then return the expected warnings"() {
        given:
        WorkflowVersion workflowVersion = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: createWorkflow(name: PanCancerWorkflow.WORKFLOW)),
        ])

        SeqTrack seqTrackWithoutConfig = createSeqTrackWithProjectName("seqTrackWithoutConfig")

        SeqTrack seqTrackWithConfig = createSeqTrackWithProjectName("seqTrackWithConfig")
        createWorkflowVersionSelector([
                project        : seqTrackWithConfig.project,
                seqType        : seqTrackWithConfig.seqType,
                workflowVersion: workflowVersion,
        ])

        SeqTrack seqTrackWithDeprecatedConfig = createSeqTrackWithProjectName("seqTrackWithDeprecatedConfig")
        createWorkflowVersionSelector([
                project        : seqTrackWithDeprecatedConfig.project,
                seqType        : seqTrackWithDeprecatedConfig.seqType,
                deprecationDate: LocalDate.now(),
                workflowVersion: workflowVersion,
        ])

        SeqTrack seqTrackWithDeprecatedAndValidConfig = createSeqTrackWithProjectName("seqTrackWithDeprecatedAndValidConfig")
        createWorkflowVersionSelector([
                project        : seqTrackWithDeprecatedAndValidConfig.project,
                seqType        : seqTrackWithDeprecatedAndValidConfig.seqType,
                deprecationDate: LocalDate.now(),
                workflowVersion: workflowVersion,
        ])
        createWorkflowVersionSelector([
                project        : seqTrackWithDeprecatedAndValidConfig.project,
                seqType        : seqTrackWithDeprecatedAndValidConfig.seqType,
                workflowVersion: workflowVersion,
        ])

        List<SeqTrack> seqTracks = [
                seqTrackWithoutConfig,
                seqTrackWithConfig,
                seqTrackWithDeprecatedConfig,
                seqTrackWithDeprecatedAndValidConfig,
        ]

        assert seqTracks.size() == SeqTrack.count(): "Not all created seqTracks are in the list"

        service.allDecider = Mock(AllDecider) {
            1 * findAlignableSeqTracks(_) >> [
                    seqTrackWithoutConfig,
                    seqTrackWithConfig,
                    seqTrackWithDeprecatedConfig,
                    seqTrackWithDeprecatedAndValidConfig,
            ]
            0 * _
        }
        service.workflowService = Mock(WorkflowService) {
            2 * isAlignment(_) >> true
        }

        List<Map<String, String>> expected = [
                [
                        project: seqTrackWithoutConfig.project.name,
                        seqType: seqTrackWithoutConfig.seqType.displayNameWithLibraryLayout,
                        count  : "1",
                ],
                [
                        project: seqTrackWithDeprecatedConfig.project.name,
                        seqType: seqTrackWithDeprecatedConfig.seqType.displayNameWithLibraryLayout,
                        count  : "1",
                ],
        ]

        when:
        List<Map<String, String>> seqTracksNotConfigured = service.createWarningsForMissingAlignmentConfig(seqTracks)

        then:
        TestCase.assertContainSame(seqTracksNotConfigured, expected)
    }

    void "createWarningsForMissingReferenceGenomeConfiguration, when run, then return the expected warnings"() {
        given:
        SpeciesWithStrain speciesWithStrain1 = createSpeciesWithStrain()
        SpeciesWithStrain speciesWithStrain2 = createSpeciesWithStrain()
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain()

        SeqTrack seqTrackNoSpeciesNoReferenceGenome = createSeqTrackWithProjectName("seqTrackNoSpeciesNoReferenceGenome")
        SeqTrack seqTrackSpecies1NoReferenceGenome = createSeqTrackWithProjectName("seqTrackSpecies1NoReferenceGenome", speciesWithStrain1)
        SeqTrack seqTrackSpecies2NoReferenceGenome = createSeqTrackWithProjectName("seqTrackSpecies2NoReferenceGenome", speciesWithStrain2)
        SeqTrack seqTrackSpecies1And2NoReferenceGenome = createSeqTrackWithProjectName("seqTrackSpecies1And2NoReferenceGenome", speciesWithStrain1, [speciesWithStrain2])

        SeqTrack seqTrackSpecies1WithReferenceGenome = createSeqTrackWithProjectName("seqTrackSpecies1WithReferenceGenome", speciesWithStrain1)
        SeqTrack seqTrackSpecies2WithReferenceGenome = createSeqTrackWithProjectName("seqTrackSpecies2WithReferenceGenome", speciesWithStrain2)
        SeqTrack seqTrackSpecies1And2WithReferenceGenome = createSeqTrackWithProjectName("seqTrackSpecies1And2WithReferenceGenome", speciesWithStrain1, [speciesWithStrain2])
        SeqTrack seqTrackSpecies1And2And3WithReferenceGenome = createSeqTrackWithProjectName("seqTrackSpecies1And2And3WithReferenceGenome", speciesWithStrain1, [speciesWithStrain2, speciesWithStrain3])

        [
                seqTrackSpecies1WithReferenceGenome,
                seqTrackSpecies2WithReferenceGenome,
                seqTrackSpecies1And2WithReferenceGenome,
                seqTrackSpecies1And2And3WithReferenceGenome,
        ].each {
            createReferenceGenomeSelector([
                    project: it.project,
                    seqType: it.seqType,
                    species: ([it.individual.species] + it.sample.mixedInSpecies) as Set,
            ])
        }

        List<SeqTrack> seqTracks = [
                seqTrackNoSpeciesNoReferenceGenome,
                seqTrackSpecies1NoReferenceGenome,
                seqTrackSpecies2NoReferenceGenome,
                seqTrackSpecies1And2NoReferenceGenome,
                seqTrackSpecies1WithReferenceGenome,
                seqTrackSpecies2WithReferenceGenome,
                seqTrackSpecies1And2WithReferenceGenome,
                seqTrackSpecies1And2And3WithReferenceGenome,
        ]

        assert seqTracks.size() == SeqTrack.count(): "Not all created seqTracks are in the list"

        List<Map<String, String>> expected = [
                seqTrackNoSpeciesNoReferenceGenome,
                seqTrackSpecies1NoReferenceGenome,
                seqTrackSpecies2NoReferenceGenome,
                seqTrackSpecies1And2NoReferenceGenome,
        ].collect {
            [
                    project: it.project.name,
                    seqType: it.seqType.displayNameWithLibraryLayout,
                    species: ([it.individual.species] + it.sample.mixedInSpecies)*.toString().sort().join(', '),
                    count  : "1",
            ]
        }

        service.allDecider = Mock(AllDecider) {
            1 * findAlignableSeqTracks(_) >> seqTracks
            0 * _
        }

        when:
        List<Map<String, String>> seqTracksMissingRefGenomes = service.createWarningsForMissingReferenceGenomeConfiguration(seqTracks)

        then:
        TestCase.assertContainSame(seqTracksMissingRefGenomes, expected)
    }

    // nestedCollect not usable, since objects collected in different levels
    @SuppressWarnings("UseCollectNested")
    void "createWarningsForSamplesHavingMultipleSeqPlatformGroups, when run, then return the expected warnings"() {
        given:
        service.mergingCriteriaService = new MergingCriteriaService()
        List<SeqTrack> mergingCriteriaNoneSameGroup = createSeqTracksForCreateWarningsForSamplesHavingMultipleSeqPlatformGroups(
                "mergingCriteriaNoneSameGroup", true, null)

        List<SeqTrack> mergingCriteriaOtpDefaultSameGroup = createSeqTracksForCreateWarningsForSamplesHavingMultipleSeqPlatformGroups(
                "mergingCriteriaOtpDefaultSameGroup", true, MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT)
        List<SeqTrack> mergingCriteriaProjectSpecificSameGroup = createSeqTracksForCreateWarningsForSamplesHavingMultipleSeqPlatformGroups(
                "mergingCriteriaProjectSpecificSameGroup", true, MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        List<SeqTrack> mergingCriteriaMergeAllSameGroup = createSeqTracksForCreateWarningsForSamplesHavingMultipleSeqPlatformGroups(
                "mergingCriteriaMergeAllSameGroup", true, MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING)

        List<SeqTrack> mergingCriteriaOtpDefaultDifferentGroups = createSeqTracksForCreateWarningsForSamplesHavingMultipleSeqPlatformGroups(
                "mergingCriteriaOtpDefaultDifferentGroups", false, MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT)
        List<SeqTrack> mergingCriteriaProjectSpecificDifferentGroups = createSeqTracksForCreateWarningsForSamplesHavingMultipleSeqPlatformGroups(
                "mergingCriteriaProjectSpecificDifferentGroups", false, MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC)
        List<SeqTrack> mergingCriteriaMergeAllDifferentGroups = createSeqTracksForCreateWarningsForSamplesHavingMultipleSeqPlatformGroups(
                "mergingCriteriaMergeAllDifferentGroups", false, MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING)

        List<SeqTrack> allSeqTracks = [
                mergingCriteriaNoneSameGroup,
                mergingCriteriaOtpDefaultSameGroup,
                mergingCriteriaProjectSpecificSameGroup,
                mergingCriteriaMergeAllSameGroup,
                mergingCriteriaOtpDefaultDifferentGroups,
                mergingCriteriaProjectSpecificDifferentGroups,
                mergingCriteriaMergeAllDifferentGroups,
        ].flatten()

        assert allSeqTracks.size() == SeqTrack.count(): "Not all created seqTracks are in the list"

        List<Map<String, Object>> expected = [
                mergingCriteriaOtpDefaultDifferentGroups,
                mergingCriteriaProjectSpecificDifferentGroups,
        ].collect { List<SeqTrack> seqTracks ->
            SeqTrack first = seqTracks.first()
            [
                    project              : first.project.name,
                    individual           : first.individual.pid,
                    seqType              : first.seqType.displayNameWithLibraryLayout,
                    sampleType           : first.sampleType.name,
                    seqPlatformGroupTable: seqTracks.collect { SeqTrack seqTrack ->
                        [
                                seqPlatformGroupId: seqTrack.seqPlatformGroup.id,
                                count             : 1,
                                seqPlatforms      : seqTrack.seqPlatformGroup.seqPlatforms*.fullName.sort(),
                        ]
                    }.sort {
                        it.seqPlatformGroupId
                    },
            ]
        }

        when:
        List<Map<String, Object>> countedSeqPlatformGroup = service.createWarningsForSamplesHavingMultipleSeqPlatformGroups(allSeqTracks)

        then:
        TestCase.assertContainSame(countedSeqPlatformGroup, expected)
    }

    void "createWarningsForMissingSeqPlatformGroup, should create warning for project and seqType specific seqPlatformGroup and ignore the once"() {
        given:
        service.mergingCriteriaService = new MergingCriteriaService()
        service.linkGenerator = Mock(LinkGenerator)
        service.messageSourceService = Mock(MessageSourceService)

        SeqPlatform seqPlatform1 = createSeqPlatform()
        SeqPlatform seqPlatform2 = createSeqPlatform()

        SeqTrack seqTrackWithDefined1 = createSeqTrack([
                laneId: 'seqTrackWithDefined1',
                run: createRun([seqPlatform: seqPlatform1]),
        ])
        SeqTrack seqTrackWithDefined2 = createSeqTrack([
                laneId: 'seqTrackWithDefined1',
                sample : seqTrackWithDefined1.sample,
                seqType: seqTrackWithDefined1.seqType,
                run    : createRun([seqPlatform: seqPlatform2]),
        ])
        MergingCriteria mergingCriteria1 = createMergingCriteria([
                project            : seqTrackWithDefined1.project,
                seqType            : seqTrackWithDefined1.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ])
        createSeqPlatformGroup([
                seqPlatforms   : [seqPlatform1, seqPlatform2],
                mergingCriteria: mergingCriteria1,
        ])

        SeqTrack seqTrackWithoutDefined1 = createSeqTrack([
                laneId: 'seqTrackWithoutDefined1',
                run: createRun([seqPlatform: createSeqPlatform()]),
        ])
        SeqTrack seqTrackWithoutDefined2 = createSeqTrack([
                laneId: 'seqTrackWithoutDefined2',
                sample : seqTrackWithoutDefined1.sample,
                seqType: seqTrackWithoutDefined1.seqType,
                run    : createRun([seqPlatform: seqPlatform2]),
        ])
        MergingCriteria mergingCriteria2 = createMergingCriteria([
                project            : seqTrackWithoutDefined1.project,
                seqType            : seqTrackWithoutDefined1.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ])
        createSeqPlatformGroup([
                seqPlatforms   : [seqPlatform1],
                mergingCriteria: mergingCriteria2,
        ])

        SeqTrack seqTrackWithDefined3 = createSeqTrack([
                laneId: 'seqTrackWithDefined3',
                run: createRun([seqPlatform: seqPlatform2]),
        ])
        SeqTrack seqTrackWithoutDefined3 = createSeqTrack([
                laneId: 'seqTrackWithoutDefined3',
                sample : seqTrackWithDefined3.sample,
                seqType: seqTrackWithDefined3.seqType,
                run    : createRun([seqPlatform: seqPlatform1]),
        ])
        MergingCriteria mergingCriteria3 = createMergingCriteria([
                project            : seqTrackWithDefined3.project,
                seqType            : seqTrackWithDefined3.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ])
        createSeqPlatformGroup([
                seqPlatforms   : [seqPlatform2],
                mergingCriteria: mergingCriteria3,
        ])

        SeqTrack seqTrackIgnored = createSeqTrack([
                run: createRun([seqPlatform: createSeqPlatform()]),
        ])
        createMergingCriteria([
                project            : seqTrackIgnored.project,
                seqType            : seqTrackIgnored.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING,
        ])

        List<SeqTrack> seqTracks = [
                seqTrackWithDefined1,
                seqTrackWithDefined2,
                seqTrackWithDefined3,
                seqTrackWithoutDefined1,
                seqTrackWithoutDefined2,
                seqTrackWithoutDefined3,
                seqTrackIgnored,
        ]

        List<SeqTrack> expectedSeqTracks = [seqTrackWithoutDefined1, seqTrackWithoutDefined2, seqTrackWithoutDefined3]

        List<Map<String, Object>> expected = expectedSeqTracks
                .groupBy { it.sample }
                .collect {
                    [
                            project     : it.key.project.name,
                            individual  : it.key.individual.displayName,
                            seqType     : it.value[0].seqType.displayNameWithLibraryLayout,
                            sampleType  : it.key.sampleType.name,
                            seqPlatforms: it.value*.seqPlatform.fullName.sort().join(', '),
                            link        : [name: 'message', path: 'link'],
                    ]
                }

        when:
        List<Map<String, Object>> missingSeqPlatformGroup = service.createWarningsForMissingSeqPlatformGroup(seqTracks)

        then:
        TestCase.assertContainSame(missingSeqPlatformGroup, expected)
        2 * service.linkGenerator.link(_) >> 'link'
        2 * service.messageSourceService.createMessage(_) >> 'message'
    }

    void "createWarningsForMissingSeqPlatformGroup, should warn for seqTracks with missing default seq platform groups"() {
        given:
        service.mergingCriteriaService = new MergingCriteriaService()
        service.linkGenerator = Mock(LinkGenerator)
        service.messageSourceService = Mock(MessageSourceService)

        // Create two seqPlatforms for which a default is defined
        SeqPlatform seqPlatform1 = createSeqPlatform()
        SeqPlatform seqPlatform2 = createSeqPlatform()
        createSeqPlatformGroup([
                mergingCriteria: null,
                seqPlatforms   : [createSeqPlatform(), seqPlatform2],
        ])
        createSeqPlatformGroup([
                mergingCriteria: null,
                seqPlatforms   : [createSeqPlatform(), seqPlatform1],
        ])

        // Two Seq Tracks in one merging criteria
        SeqTrack seqTrackWithDefault1 = createSeqTrack([
                run: createRun([seqPlatform: seqPlatform1]),
        ])
        SeqTrack seqTrackWithDefault2 = createSeqTrack([
                sample : seqTrackWithDefault1.sample,
                seqType: seqTrackWithDefault1.seqType,
                run    : createRun([seqPlatform: seqPlatform2]),
        ])
        createMergingCriteria([
                project            : seqTrackWithDefault2.project,
                seqType            : seqTrackWithDefault2.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        ])

        // Two Seq Tracks in one merging criteria
        SeqTrack seqTrackWithDefault3 = createSeqTrack([
                run: createRun([seqPlatform: seqPlatform1]),
        ])
        SeqTrack seqTrackWithoutDefault1 = createSeqTrack([
                sample : seqTrackWithDefault3.sample,
                seqType: seqTrackWithDefault3.seqType,
                run    : createRun([seqPlatform: createSeqPlatform()]),
        ])
        createMergingCriteria([
                project            : seqTrackWithoutDefault1.project,
                seqType            : seqTrackWithoutDefault1.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        ])

        // One Seq Track in one merging criteria
        SeqTrack seqTrackWithoutDefault2 = createSeqTrack([
                run: createRun([seqPlatform: createSeqPlatform()]),
        ])
        createMergingCriteria([
                project            : seqTrackWithoutDefault2.project,
                seqType            : seqTrackWithoutDefault2.seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
        ])

        List<SeqTrack> seqTracks = [seqTrackWithDefault1, seqTrackWithDefault2, seqTrackWithDefault3, seqTrackWithoutDefault1, seqTrackWithoutDefault2]
        List<SeqTrack> expectedSeqTracks = [seqTrackWithoutDefault1, seqTrackWithoutDefault2]

        List<Map<String, Object>> expected = expectedSeqTracks
                .groupBy { it.sample }
                .collect {
                    [
                            project     : it.key.project.name,
                            individual  : it.key.individual.displayName,
                            seqType     : it.value[0].seqType.displayNameWithLibraryLayout,
                            sampleType  : it.key.sampleType.name,
                            seqPlatforms: it.value*.seqPlatform.fullName.sort().join(', '),
                            link        : [name: 'message', path: 'link'],
                    ]
                }

        when:
        List<Map<String, Object>> missingSeqPlatformGroup = service.createWarningsForMissingSeqPlatformGroup(seqTracks)

        then:
        TestCase.assertContainSame(missingSeqPlatformGroup, expected)
        2 * service.linkGenerator.link(_) >> 'link'
        2 * service.messageSourceService.createMessage(_) >> 'message'
    }

    // nestedCollect not usable, since objects collected in different levels
    @SuppressWarnings("UseCollectNested")
    void "createWarningsForSamplesHavingMultipleLibPrepKits, when run, then return the expected warnings"() {
        given:
        service.mergingCriteriaService = new MergingCriteriaService()

        List<SeqTrack> mergingCriteriaUseLibPrepKitHaveSameLibPrepKit = createSeqTracksForCreateWarningsForSamplesHavingMultipleLibPrepKits(
                "mergingCriteriaUseLibPrepKitHaveSameLibPrepKit", true, true)
        List<SeqTrack> mergingCriteriaUseLibPrepKitHaveDifferentLibPrepKit = createSeqTracksForCreateWarningsForSamplesHavingMultipleLibPrepKits(
                "mergingCriteriaUseLibPrepKitHaveDifferentLibPrepKit", false, true)
        List<SeqTrack> mergingCriteriaIgnoreLibPrepKitHaveSameLibPrepKit = createSeqTracksForCreateWarningsForSamplesHavingMultipleLibPrepKits(
                "mergingCriteriaIgnoreLibPrepKitHaveSameLibPrepKit", true, false)
        List<SeqTrack> mergingCriteriaIgnoreLibPrepKitHaveDifferentLibPrepKit = createSeqTracksForCreateWarningsForSamplesHavingMultipleLibPrepKits(
                "mergingCriteriaIgnoreLibPrepKitHaveDifferentLibPrepKit", false, false)

        List<SeqTrack> allSeqTracks = [
                mergingCriteriaUseLibPrepKitHaveSameLibPrepKit,
                mergingCriteriaUseLibPrepKitHaveDifferentLibPrepKit,
                mergingCriteriaIgnoreLibPrepKitHaveSameLibPrepKit,
                mergingCriteriaIgnoreLibPrepKitHaveDifferentLibPrepKit,
        ].flatten()

        assert allSeqTracks.size() == SeqTrack.count(): "Not all created seqTracks are in the list"

        List<Map<String, Object>> expected = [
                mergingCriteriaUseLibPrepKitHaveDifferentLibPrepKit,
        ].collect { List<SeqTrack> seqTracks ->
            SeqTrack first = seqTracks.first()
            [
                    project                   : first.project.name,
                    individual                : first.individual.pid,
                    seqType                   : first.seqType.displayNameWithLibraryLayout,
                    sampleType                : first.sampleType.name,
                    libraryPreparationKitTable: seqTracks.collect { SeqTrack seqTrack ->
                        [
                                libraryPreparationKit: seqTrack.libraryPreparationKit?.name ?: '-',
                                count                : 1,
                        ]
                    }.sort {
                        it.libraryPreparationKit
                    },
            ]
        }

        when:
        List<Map<String, Object>> countedLibPrepKits = service.createWarningsForSamplesHavingMultipleLibPrepKits(allSeqTracks)

        then:
        TestCase.assertContainSame(countedLibPrepKits, expected)
    }

    void "createWarningsForWithdrawnSeqTracks, when run, then return the expected warnings"() {
        given:
        service.mergingCriteriaService = new MergingCriteriaService()

        SeqTrack seqTract1 = createSeqTrack()
        SeqTrack seqTract2 = createSeqTrack([sample: seqTract1.sample, seqType: seqTract1.seqType])
        SeqTrack seqTract3 = createSeqTrack()
        SeqTrack seqTract4 = createSeqTrack([sample: seqTract3.sample, seqType: seqTract3.seqType])
        SeqTrack seqTract5 = createSeqTrack()
        SeqTrack seqTract6 = createSeqTrack([sample: seqTract5.sample, seqType: seqTract5.seqType])

        [
                seqTract4,
                seqTract5,
                seqTract6,
        ].each {
            createFastqFile([seqTrack: it, fileWithdrawn: true])
        }

        List<SeqTrack> allSeqTracks = [
                seqTract1,
                seqTract2,
                seqTract3,
                seqTract4,
                seqTract5,
                seqTract6,
        ]

        assert allSeqTracks.size() == SeqTrack.count(): "Not all created seqTracks are in the list"

        List<Map<String, Object>> expected = [
                [seqTract4, "1"],
                [seqTract5, "2"],
        ].collect {
            SeqTrack seqTrack = it[0]
            [
                    project   : seqTrack.project.name,
                    individual: seqTrack.individual.pid,
                    seqType   : seqTrack.seqType.displayNameWithLibraryLayout,
                    sampleType: seqTrack.sampleType.name,
                    count     : it[1],
            ]
        }

        when:
        List<Map<String, Object>> countedWithdrawn = service.createWarningsForWithdrawnSeqTracks(allSeqTracks)

        then:
        TestCase.assertContainSame(countedWithdrawn, expected)
    }

    private SeqTrack createSeqTrackWithProjectName(String name, SpeciesWithStrain mainSpecies = null, Collection<SpeciesWithStrain> mixedInSpecies = []) {
        return createSeqTrack([
                sample: createSample([
                        individual    : createIndividual([
                                project: createProject([
                                        name: name,
                                ]),
                                species: mainSpecies,
                        ]),
                        mixedInSpecies: mixedInSpecies as Set
                ]),
        ])
    }

    private List<SeqTrack> createSeqTracksForCreateWarningsForSamplesHavingMultipleSeqPlatformGroups(String name, boolean sameSeqPlatformGroup, MergingCriteria.SpecificSeqPlatformGroups specificSeqPlatformGroups = null) {
        SeqTrack seqTrack1 = createSeqTrack([
                sample: createSample([
                        individual: createIndividual([
                                project: createProject([
                                        name: name,
                                ]),
                        ]),
                ]),
                run   : createRun([
                        /**
                         * the method createRun uses createSeqPlatformWithSeqPlatformGroup, but here we only want a seqPlatform without an seqPlatformGroup
                         */
                        seqPlatform: createSeqPlatform(),
                ]),
        ])
        SeqTrack seqTrack2 = createSeqTrack([
                sample : seqTrack1.sample,
                seqType: seqTrack1.seqType,
                run    : createRun([
                        seqPlatform: createSeqPlatform(),
                ]),
        ])
        SeqTrack seqTrack3 = createSeqTrack([
                sample : seqTrack1.sample,
                seqType: seqTrack1.seqType,
                run    : createRun([
                        seqPlatform: createSeqPlatform(),
                ]),
        ])

        List<SeqTrack> seqTracks = [
                seqTrack1,
                seqTrack2,
                seqTrack3,
        ]

        MergingCriteria mergingCriteria = specificSeqPlatformGroups ?
                createMergingCriteria([
                        project            : seqTrack1.project,
                        seqType            : seqTrack1.seqType,
                        useSeqPlatformGroup: specificSeqPlatformGroups,
                ]) : null

        MergingCriteria mergingCriteriaForSeqPlatform = specificSeqPlatformGroups == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC ? mergingCriteria : null

        if (sameSeqPlatformGroup) {
            createSeqPlatformGroup([
                    seqPlatforms   : seqTracks*.seqPlatform as Set,
                    mergingCriteria: mergingCriteriaForSeqPlatform,
            ])
        } else {
            seqTracks.each {
                createSeqPlatformGroup([
                        seqPlatforms   : [it.seqPlatform, createSeqPlatform()] as Set,
                        mergingCriteria: mergingCriteriaForSeqPlatform,
                ])
            }
        }

        return seqTracks
    }

    private List<SeqTrack> createSeqTracksForCreateWarningsForSamplesHavingMultipleLibPrepKits(String name, boolean sameLibPrepKit, boolean libPrepKitInMergingCriteria) {
        SeqTrack seqTrack1 = createSeqTrack([
                sample               : createSample([
                        individual: createIndividual([
                                project: createProject([
                                        name: name,
                                ]),
                        ]),
                ]),
                libraryPreparationKit: createLibraryPreparationKit(),
        ])
        SeqTrack seqTrack2 = createSeqTrack([
                sample               : seqTrack1.sample,
                seqType              : seqTrack1.seqType,
                libraryPreparationKit: sameLibPrepKit ? seqTrack1.libraryPreparationKit : createLibraryPreparationKit(),
        ])
        SeqTrack seqTrack3 = createSeqTrack([
                sample               : seqTrack1.sample,
                seqType              : seqTrack1.seqType,
                libraryPreparationKit: sameLibPrepKit ? seqTrack1.libraryPreparationKit : null,
        ])

        List<SeqTrack> seqTracks = [
                seqTrack1,
                seqTrack2,
                seqTrack3,
        ]

        createMergingCriteria([
                project      : seqTrack1.project,
                seqType      : seqTrack1.seqType,
                useLibPrepKit: libPrepKitInMergingCriteria,
        ])

        return seqTracks
    }
}
