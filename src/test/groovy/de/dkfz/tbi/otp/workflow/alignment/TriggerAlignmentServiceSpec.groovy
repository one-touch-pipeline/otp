/*
 * Copyright 2011-2025 The OTP authors
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
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SnvDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryInstance
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.withdraw.RoddyBamFileWithdrawService
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.analysis.indel.IndelWorkflow
import de.dkfz.tbi.otp.workflow.analysis.runyapsa.RunYapsaWorkflow
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflow.bamImport.BamImportWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.*
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.*

import java.time.LocalDate

class TriggerAlignmentServiceSpec extends HibernateSpec implements IsRoddy, WorkflowSystemDomainFactory {

    TriggerAlignmentService service

    @Override
    List<Class> getDomainClasses() {
        return [
                AbstractBamFile,
                BamFilePairAnalysis,
                ExternallyProcessedBamFile,
                ExternalMergingWorkPackage,
                FastqFile,
                Individual,
                MergingWorkPackage,
                ProcessingThresholds,
                Project,
                RawSequenceFile,
                ReferenceGenomeSelector,
                RoddyBamFile,
                Sample,
                SampleType,
                SampleTypePerProject,
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

    void "run triggerAlignment with external BAM files only, should trigger analysis workflows"() {
        given:
        Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = createStandardDeciderActions([
                (PanCancerDecider): DeciderCreateWorkflowAction.SKIP,
                (SnvDecider)      : DeciderCreateWorkflowAction.CREATE_ALWAYS,
                (IndelDecider)    : DeciderCreateWorkflowAction.CREATE_ALWAYS,
        ])

        ExternalMergingWorkPackage workPackage1 = ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage()
        ExternalMergingWorkPackage workPackage2 = ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage()

        ExternallyProcessedBamFile bamFile1 = createExternalBamFileWithArtefact([workPackage: workPackage1])
        ExternallyProcessedBamFile bamFile2 = createExternalBamFileWithArtefact([workPackage: workPackage2])

        WorkflowArtefact snvArtefact = createWorkflowArtefact([artefactType: ArtefactType.SNV, outputRole: SnvWorkflow.ANALYSIS_OUTPUT])
        WorkflowArtefact indelArtefact = createWorkflowArtefact([artefactType: ArtefactType.INDEL, outputRole: IndelWorkflow.ANALYSIS_OUTPUT])

        DeciderResult deciderResult = createDeciderResultWithArtefacts([snvArtefact, indelArtefact])

        setupAllDeciderMock([], deciderResult) { artefacts, params, actions ->
            // Should only contain BAM file artefacts, NO seqTrack artefacts
            assert artefacts.containsAll([bamFile1.workflowArtefact, bamFile2.workflowArtefact])
            assert artefacts.size() == 2  // Only 2 BAM artefacts, no seqTrack artefacts
        }
        setupTicketServiceMock([])
        setupSamplePairDeciderServiceMock([])
        setupRoddyBamFileWithdrawServiceMock()

        when:
        TriggerAlignmentResult result = service.triggerAlignment([] as Set, [bamFile1, bamFile2] as Set, true, deciderAction)

        then:
        result.newArtefacts.size() == 2
        result.mergingWorkPackages.isEmpty()
    }

    void "run triggerAlignment with different workflow combinations"() {
        given:
        Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = createStandardDeciderActions(workflowOverrides)

        SeqTrack seqTrack = createSeqTrackWithArtefact()
        MergingWorkPackage workPackage = createMergingWorkPackage()
        RoddyBamFile bamFile = createBamFileWithArtefact([workPackage: workPackage, seqTracks: [seqTrack]])

        List<WorkflowArtefact> expectedArtefacts = createAnalysisArtefacts(enabledWorkflows)

        if (expectedMergingWorkPackages) {
            expectedArtefacts << bamFile.workflowArtefact
        }

        DeciderResult deciderResult = createDeciderResultWithArtefacts(expectedArtefacts)

        setupAllDeciderMock([], deciderResult) { artefacts, params, actions ->
            assert artefacts.contains(bamFile.workflowArtefact)
            assert artefacts.contains(seqTrack.workflowArtefact)
            assert artefacts.size() == 2
        }
        setupTicketServiceMock([])
        setupSamplePairDeciderServiceMock(expectedMergingWorkPackages ? [workPackage] : [])
        setupRoddyBamFileWithdrawServiceMock()

        when:
        TriggerAlignmentResult result = service.triggerAlignment([] as Set, [bamFile] as Set, true, deciderAction)

        then:
        result.newArtefacts.size() == expectedArtefactCount
        result.mergingWorkPackages.size() == (expectedMergingWorkPackages ? [workPackage] : []).size()

        where:
        scenario               | workflowOverrides                                                                                                                                                                                                                                                                                  | enabledWorkflows                     | expectedMergingWorkPackages | expectedArtefactCount
        "SNV and Indel"        | [(PanCancerDecider): DeciderCreateWorkflowAction.SKIP, (SnvDecider): DeciderCreateWorkflowAction.CREATE_ALWAYS, (IndelDecider): DeciderCreateWorkflowAction.CREATE_ALWAYS]                                                                                                                         | ['SNV', 'INDEL']                     | false                       | 2
        "All analysis"         | [(PanCancerDecider): DeciderCreateWorkflowAction.SKIP, (SnvDecider): DeciderCreateWorkflowAction.CREATE_ALWAYS, (IndelDecider): DeciderCreateWorkflowAction.CREATE_ALWAYS, (SophiaDecider): DeciderCreateWorkflowAction.CREATE_ALWAYS, (AceseqDecider): DeciderCreateWorkflowAction.CREATE_ALWAYS] | ['SNV', 'INDEL', 'SOPHIA', 'ACESEQ'] | false                       | 4
        "Alignment + Analysis" | [(SnvDecider): DeciderCreateWorkflowAction.CREATE_ALWAYS, (IndelDecider): DeciderCreateWorkflowAction.CREATE_ALWAYS]                                                                                                                                                                               | ['BAM', 'SNV', 'INDEL']              | true                        | 3
        "Alignment only"       | [:]                                                                                                                                                                                                                                                                                                | ['BAM']                              | true                        | 1
    }

    void "createWarningsForMissingWorkflowConfig returns the expected warnings with correct counts"() {
        given:
        final int COUNT = 3
        final Project project = createProject(name: "TestProject")
        final SeqType seqType = createSeqType(name: "TestSeqType", displayName: "TestSeqTypeDisplay")
        final List<String> enabledWorkflows = [
                WesFastQcWorkflow.WORKFLOW,
                PanCancerWorkflow.WORKFLOW,
                RnaAlignmentWorkflow.WORKFLOW,
                IndelWorkflow.WORKFLOW,
                SnvWorkflow.WORKFLOW,
                SophiaWorkflow.WORKFLOW,
                AceseqWorkflow.WORKFLOW,
                RunYapsaWorkflow.WORKFLOW,
        ].asImmutable()

        List<WorkflowVersion> workflowVersions = []
        List<Map<String, String>> expected = []

        enabledWorkflows.each { String workflowName ->
            workflowVersions.add(createWorkflowVersion([
                    apiVersion       : createWorkflowApiVersion(workflow: createWorkflow(name: workflowName)),
                    supportedSeqTypes: [seqType],
            ]))
            expected.add([
                    workflow: workflowName,
                    project : project.name,
                    seqType : seqType.displayNameWithLibraryLayout,
                    count   : COUNT.toString(),
            ])
        }

        List<SeqTrack> seqTracks = []
        for (int i = 0; i < COUNT; i++) {
            seqTracks.add(createSeqTrack([
                    sample : createSample([
                            individual: createIndividual([
                                    project: project,
                            ]),
                    ]),
                    seqType: seqType,
            ]))
        }

        if (createSelector) {
            createWorkflowVersionSelector([
                    project        : createProject(),
                    seqType        : createSeqType(),
                    workflowVersion: workflowVersions[0],
            ])
        }

        service.allDecider = Mock(AllDecider) {
            1 * getEnabledWorkflowNames() >> enabledWorkflows
            0 * _
        }

        service.workflowService = Mock(WorkflowService) {
            _ * isAlignment(_) >> true
        }

        when:
        List<Map<String, String>> seqTracksNotConfigured = service.createWarningsForMissingWorkflowConfig(seqTracks)

        then:
        TestCase.assertContainSame(seqTracksNotConfigured, expected)

        where:
        type || createSelector
        1    || true
        2    || false
    }

    void "createWarningsForMissingWorkflowConfig returns the expected warnings"() {
        given:
        List<SeqType> supportedSeqTypes = [createSeqType(), createSeqType(),]
        WorkflowVersion workflowVersion = createWorkflowVersion([
                apiVersion       : createWorkflowApiVersion(workflow: createWorkflow(name: PanCancerWorkflow.WORKFLOW)),
                supportedSeqTypes: supportedSeqTypes,
        ])

        SeqTrack seqTrackWithoutConfig = createSeqTrack([
                sample : createSample([
                        individual: createIndividual([
                                project: createProject(name: "seqTrackWithoutConfig"),
                        ]),
                ]),
                seqType: supportedSeqTypes[0],
        ])

        SeqTrack seqTrackWithConfig = createSeqTrack([
                sample : createSample([
                        individual: createIndividual([
                                project: createProject(name: "seqTrackWithConfig"),
                        ]),
                ]),
                seqType: supportedSeqTypes[0],
        ])
        createWorkflowVersionSelector([
                project        : seqTrackWithConfig.project,
                seqType        : seqTrackWithConfig.seqType,
                workflowVersion: workflowVersion,
        ])

        SeqTrack seqTrackWithDeprecatedConfig = createSeqTrack([
                sample : createSample([
                        individual: createIndividual([
                                project: createProject(name: "seqTrackWithDeprecatedConfig"),
                        ]),
                ]),
                seqType: supportedSeqTypes[1],
        ])
        createWorkflowVersionSelector([
                project        : seqTrackWithDeprecatedConfig.project,
                seqType        : seqTrackWithDeprecatedConfig.seqType,
                deprecationDate: LocalDate.now(),
                workflowVersion: workflowVersion,
        ])

        SeqTrack seqTrackWithDeprecatedAndValidConfig = createSeqTrack([
                seqType: supportedSeqTypes[1],
        ])
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
            1 * getEnabledWorkflowNames() >> [
                    PanCancerWorkflow.WORKFLOW,
                    WesFastQcWorkflow.WORKFLOW,
            ]
            0 * _
        }

        service.workflowService = Mock(WorkflowService) {
            _ * isAlignment(_) >> true
        }

        List<Map<String, String>> expected = [
                [
                        workflow: PanCancerWorkflow.WORKFLOW,
                        project : seqTrackWithoutConfig.project.name,
                        seqType : seqTrackWithoutConfig.seqType.displayNameWithLibraryLayout,
                        count   : "1",
                ],
                [
                        workflow: PanCancerWorkflow.WORKFLOW,
                        project : seqTrackWithDeprecatedConfig.project.name,
                        seqType : seqTrackWithDeprecatedConfig.seqType.displayNameWithLibraryLayout,
                        count   : "1",
                ],
        ]

        when:
        List<Map<String, String>> seqTracksNotConfigured = service.createWarningsForMissingWorkflowConfig(seqTracks)

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
                run   : createRun([seqPlatform: seqPlatform1]),
        ])
        SeqTrack seqTrackWithDefined2 = createSeqTrack([
                laneId : 'seqTrackWithDefined1',
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
                run   : createRun([seqPlatform: createSeqPlatform()]),
        ])
        SeqTrack seqTrackWithoutDefined2 = createSeqTrack([
                laneId : 'seqTrackWithoutDefined2',
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
                run   : createRun([seqPlatform: seqPlatform2]),
        ])
        SeqTrack seqTrackWithoutDefined3 = createSeqTrack([
                laneId : 'seqTrackWithoutDefined3',
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

    void "test createWarningsForMissingSampleTypePerProject"() {
        given:
        SeqTrack seqTrack1 = createSeqTrack()
        SeqTrack seqTrack2 = createSeqTrack()
        SnvDomainFactory.INSTANCE.createSampleTypePerProject(project: seqTrack1.project, sampleType: seqTrack1.sampleType)

        when:
        List<Map<String, String>> result = service.createWarningsForMissingSampleTypePerProject([seqTrack1, seqTrack2])

        then:
        Map<String, String> map = CollectionUtils.exactlyOneElement(result)
        map["project"] == seqTrack2.project.name
        map["sampleType"] == seqTrack2.sampleType.displayName
    }

    void "test createWarningsForMissingProcessingThresholds"() {
        given:
        SeqTrack seqTrack1 = createSeqTrack()
        SeqTrack seqTrack2 = createSeqTrack()
        SnvDomainFactory.INSTANCE.createProcessingThresholds(project: seqTrack1.project, seqType: seqTrack1.seqType, sampleType: seqTrack1.sampleType)

        service.workflowService = Mock(WorkflowService) {
            findAllAnalysisWorkflows() >> [new Workflow()]
            getSupportedSeqTypesOfVersions(_) >> [seqTrack1.seqType, seqTrack2.seqType]
        }

        when:
        List<Map<String, String>> result = service.createWarningsForMissingProcessingThresholds([seqTrack1, seqTrack2])

        then:
        Map<String, String> map = CollectionUtils.exactlyOneElement(result)
        map["project"] == seqTrack2.project.name
        map["seqType"] == seqTrack2.seqType.displayName
        map["sampleType"] == seqTrack2.sampleType.displayName
    }

    // some of these should be moved into a corresponding DomainFactory
    private Map<Class<? extends Decider>, DeciderCreateWorkflowAction> createStandardDeciderActions(Map overrides = [:]) {
        Map<Class<? extends Decider>, DeciderCreateWorkflowAction> defaults = [
                (FastqcDecider)      : DeciderCreateWorkflowAction.SKIP,
                (PanCancerDecider)   : DeciderCreateWorkflowAction.CREATE_ALWAYS,
                (WgbsDecider)        : DeciderCreateWorkflowAction.SKIP,
                (RnaAlignmentDecider): DeciderCreateWorkflowAction.SKIP,
                (SnvDecider)         : DeciderCreateWorkflowAction.SKIP,
                (IndelDecider)       : DeciderCreateWorkflowAction.SKIP,
                (SophiaDecider)      : DeciderCreateWorkflowAction.SKIP,
                (AceseqDecider)      : DeciderCreateWorkflowAction.SKIP,
        ]
        return defaults + overrides
    }

    private SeqTrack createSeqTrackWithArtefact(Map options = [:]) {
        Project project = options.project ?: createProject()
        Individual individual = options.individual ?: createIndividual(project: project)
        SeqType seqType = options.seqType ?: createSeqTypePaired()

        Workflow workflow = options.workflow ?: createWorkflow([
                defaultSeqTypesForWorkflowVersions: [seqType]
        ])
        WorkflowRun run = options.run ?: createWorkflowRun([
                workflow: workflow,
                project : project,
        ])

        WorkflowArtefact artefact = createWorkflowArtefact([
                producedBy: run,
        ])

        return createSeqTrackWithTwoFastqFile([
                sample          : options.sample ?: createSample(individual: individual),
                seqType         : seqType,
                workflowArtefact: artefact,
        ])
    }

    private RoddyBamFile createBamFileWithArtefact(Map options = [:]) {
        Project project = options.project ?: createProject()
        SeqType seqType = options.seqType ?: createSeqTypePaired()

        Workflow workflow = options.workflow ?: createWorkflow([
                defaultSeqTypesForWorkflowVersions: [seqType]
        ])
        WorkflowRun run = options.run ?: createWorkflowRun([
                workflow: workflow,
                project : project,
        ])

        WorkflowArtefact bamArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.BAM,
                outputRole  : PanCancerWorkflow.OUTPUT_BAM,
                producedBy  : run,
        ])

        MergingWorkPackage workPackage = options.workPackage ?: createMergingWorkPackage()
        List<SeqTrack> seqTracks = options.seqTracks ?: []

        return createRoddyBamFile([
                workflowArtefact: bamArtefact,
                workPackage     : workPackage,
                seqTracks       : seqTracks,
        ], RoddyBamFile)
    }

    private ExternallyProcessedBamFile createExternalBamFileWithArtefact(Map options = [:]) {
        Project project = options.project ?: createProject()

        Workflow workflow = options.workflow ?: createWorkflow()
        WorkflowRun run = options.run ?: createWorkflowRun([
                workflow: workflow,
                project : project,
        ])

        WorkflowArtefact bamArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.BAM,
                outputRole  : BamImportWorkflow.OUTPUT_BAM,
                producedBy  : run,
        ])

        ExternalMergingWorkPackage workPackage = options.workPackage ?: createExternalMergingWorkPackage()

        return ExternalBamFactoryInstance.INSTANCE.createBamFile([
                workflowArtefact: bamArtefact,
                workPackage     : workPackage,
        ])
    }

    private List<WorkflowArtefact> createAnalysisArtefacts(List<String> enabledWorkflows) {
        List<WorkflowArtefact> artefacts = []
        if (enabledWorkflows.contains('SNV')) {
            artefacts << createWorkflowArtefact([artefactType: ArtefactType.SNV, outputRole: SnvWorkflow.ANALYSIS_OUTPUT])
        }
        if (enabledWorkflows.contains('INDEL')) {
            artefacts << createWorkflowArtefact([artefactType: ArtefactType.INDEL, outputRole: IndelWorkflow.ANALYSIS_OUTPUT])
        }
        if (enabledWorkflows.contains('SOPHIA')) {
            artefacts << createWorkflowArtefact([artefactType: ArtefactType.SOPHIA, outputRole: SophiaWorkflow.ANALYSIS_OUTPUT])
        }
        if (enabledWorkflows.contains('ACESEQ')) {
            artefacts << createWorkflowArtefact([artefactType: ArtefactType.ACESEQ, outputRole: AceseqWorkflow.ANALYSIS_OUTPUT])
        }
        return artefacts
    }

    private DeciderResult createDeciderResultWithArtefacts(List<WorkflowArtefact> artefacts) {
        DeciderResult result = new DeciderResult()
        artefacts.each { result.newArtefacts << it }
        return result
    }

    private void setupAllDeciderMock(List<SeqTrack> alignableSeqTracks, DeciderResult result, Closure additionalAssertions = null) {
        service.allDecider = Mock(AllDecider) {
            1 * decide(_, _, _) >> { List<WorkflowArtefact> artefacts, Map<String, String> params, Map<Class<? extends Decider>, DeciderCreateWorkflowAction> actions ->
                if (additionalAssertions) {
                    additionalAssertions.call(artefacts, params, actions)
                }
                return result
            }
            1 * findAlignableSeqTracks(_) >> alignableSeqTracks
            0 * _
        }
    }

    private void setupTicketServiceMock(Collection<SeqTrack> expectedSeqTracks) {
        List tickets = expectedSeqTracks.collect { createTicket() }
        service.ticketService = Mock(TicketService) {
            1 * findAllTickets { Collection<SeqTrack> actual ->
                actual.containsAll(expectedSeqTracks) && expectedSeqTracks.containsAll(actual)
            } >> tickets
            tickets.size() * resetAlignmentAndAnalysisNotification(_)
        }
    }

    private void setupSamplePairDeciderServiceMock(List<MergingWorkPackage> workPackages) {
        service.samplePairDeciderService = Mock(SamplePairDeciderService) {
            if (workPackages.isEmpty()) {
                0 * findOrCreateSamplePairs(_)
            } else {
                1 * findOrCreateSamplePairs(workPackages)
            }
        }
    }

    private void setupRoddyBamFileWithdrawServiceMock(Map<List<SeqTrack>, List<AbstractBamFile>> collectObjectsMap = [:]) {
        service.roddyBamFileWithdrawService = Mock(RoddyBamFileWithdrawService)

        collectObjectsMap.each { seqTracks, bamFiles ->
            service.roddyBamFileWithdrawService.collectObjects(seqTracks) >> bamFiles
        }

        if (collectObjectsMap.isEmpty()) {
            service.roddyBamFileWithdrawService.collectObjects(_) >> []
        }
    }
}
