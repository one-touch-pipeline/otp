/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.stat.Statistics
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryInstance
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.*

@Rollback
@Integration
@Slf4j
class TriggerWorkflowsServiceIntegrationSpec extends Specification implements DomainFactoryCore, RoddyPanCancerFactory, WorkflowSystemDomainFactory {

    @Autowired
    TriggerWorkflowsService service

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    SessionFactory sessionFactory

    void "test getBamFiles, with empty input"() {
        when:
        List<AbstractBamFile> result = service.getBamFiles([])

        then:
        TestCase.assertContainSame(result, [])
    }

    void "test getBamFiles, with seq. track with a BAM file"() {
        when:
        List<AbstractBamFile> result = service.getBamFiles([createSeqTrack().id])

        then:
        TestCase.assertContainSame(result, [])
    }

    void "test getBamFiles, with single BAM file with single seq. track"() {
        given:
        RoddyBamFile bamFile = createBamFile(seqTracks: [createSeqTrack()])

        when:
        List<AbstractBamFile> result = service.getBamFiles(bamFile.containedSeqTracks*.id)

        then:
        TestCase.assertContainSame(result, [bamFile])
    }

    void "test getBamFiles, with single BAM file with multiple seq. tracks, passing one seq. track"() {
        given:
        SeqTrack seqTrack1 = createSeqTrack()
        RoddyBamFile bamFile = createBamFile(seqTracks: [seqTrack1, createSeqTrack(), createSeqTrack()])

        when:
        List<AbstractBamFile> result = service.getBamFiles([seqTrack1.id])

        then:
        TestCase.assertContainSame(result, [bamFile])
    }

    void "test getBamFiles, with single BAM file with multiple seq. tracks, passing all seq. tracks"() {
        given:
        RoddyBamFile bamFile = createBamFile(seqTracks: [createSeqTrack(), createSeqTrack(), createSeqTrack()])

        when:
        List<AbstractBamFile> result = service.getBamFiles(bamFile.containedSeqTracks*.id)

        then:
        TestCase.assertContainSame(result, [bamFile])
    }

    void "test getBamFiles, with multiple BAM files"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        RoddyBamFile bamFile2 = createBamFile(seqTracks: bamFile.containedSeqTracks as List, workPackage: bamFile.workPackage)

        when:
        List<AbstractBamFile> result = service.getBamFiles(bamFile.containedSeqTracks*.id + bamFile2.containedSeqTracks*.id)

        then:
        TestCase.assertContainSame(result, [bamFile, bamFile2])
    }

    void "test getBamFiles, with multiple BAM files, one is withdrawn"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        RoddyBamFile bamFile2 = createBamFile(seqTracks: bamFile.containedSeqTracks as List, withdrawn: true, workPackage: bamFile.workPackage)

        when:
        List<AbstractBamFile> result = service.getBamFiles(bamFile.containedSeqTracks*.id + bamFile2.containedSeqTracks*.id)

        then:
        TestCase.assertContainSame(result, [bamFile])
    }

    void "test getSeqTracks, with empty input"() {
        when:
        List<SeqTrack> result = service.getSeqTracks([])

        then:
        TestCase.assertContainSame(result, [])
    }

    void "test getSeqTracks, with single BAM file"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        createSeqTrack()

        when:
        List<SeqTrack> result = service.getSeqTracks([bamFile.id])

        then:
        TestCase.assertContainSame(result, bamFile.containedSeqTracks)
    }

    void "test getSeqTracks, with multiple BAM files"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        RoddyBamFile bamFile2 = createBamFile()

        when:
        List<SeqTrack> result = service.getSeqTracks([bamFile.id, bamFile2.id])

        then:
        TestCase.assertContainSame(result, bamFile.containedSeqTracks + bamFile2.containedSeqTracks)
    }

    void "test getSeqTracks, with multiple BAM files, should return each seq tracks only once"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        RoddyBamFile bamFile2 = createBamFile(seqTracks: bamFile.containedSeqTracks as List, withdrawn: true, workPackage: bamFile.workPackage)

        when:
        List<SeqTrack> result = service.getSeqTracks([bamFile.id, bamFile2.id])

        then:
        result.size() == bamFile.containedSeqTracks.size()
        TestCase.assertContainSame(result, bamFile.containedSeqTracks)
    }

    void "test getInfo, with empty input"() {
        expect:
        service.getInfo([]) == []
    }

    void "test getInfo, with several seq tracks"() {
        given:
        createBamFile()
        SeqTrack seqTrack1 = createSeqTrack()
        WorkflowVersionSelector workflowVersionSelector1 = createWorkflowVersionSelector(project: seqTrack1.project, seqType: seqTrack1.seqType)
        ReferenceGenomeSelector referenceGenomeSelector1a = createReferenceGenomeSelector(project: seqTrack1.project, seqType: seqTrack1.seqType, workflow: workflowVersionSelector1.workflowVersion.workflow)
        ReferenceGenomeSelector referenceGenomeSelector1b = createReferenceGenomeSelector(project: seqTrack1.project, seqType: seqTrack1.seqType, workflow: workflowVersionSelector1.workflowVersion.workflow)
        SeqTrack seqTrack2 = createSeqTrack()
        WorkflowVersionSelector workflowVersionSelector2 = createWorkflowVersionSelector(project: seqTrack2.project, seqType: seqTrack2.seqType)
        ReferenceGenomeSelector referenceGenomeSelector2a = createReferenceGenomeSelector(project: seqTrack2.project, seqType: seqTrack2.seqType, workflow: workflowVersionSelector2.workflowVersion.workflow)
        ReferenceGenomeSelector referenceGenomeSelector2b = createReferenceGenomeSelector(project: seqTrack2.project, seqType: seqTrack2.seqType, workflow: workflowVersionSelector2.workflowVersion.workflow)
        createWorkflowVersionSelector(project: seqTrack1.project, seqType: seqTrack2.seqType)

        when:
        List<WorkflowVersionAndReferenceGenomeSelector> result = service.getInfo([seqTrack1, seqTrack2])

        then:
        TestCase.assertContainSame(result*.workflowVersionSelector, [workflowVersionSelector1, workflowVersionSelector2])
        TestCase.assertContainSame(result.find { it.workflowVersionSelector == workflowVersionSelector1 }.referenceGenomeSelectors,
                [referenceGenomeSelector1a, referenceGenomeSelector1b])
        TestCase.assertContainSame(result.find { it.workflowVersionSelector == workflowVersionSelector2 }.referenceGenomeSelectors,
                [referenceGenomeSelector2a, referenceGenomeSelector2b])
    }

    void "triggerWorkflowByProjectAndSampleTypes should trigger allDecider for workflow artefacts"() {
        given:
        service.allDecider = Mock(AllDecider)

        Project project = createProject()
        SampleType sampleType1 = createSampleType()
        SampleType sampleType2 = createSampleType()
        SampleType sampleType3 = createSampleType()
        WorkflowArtefact workflowArtefact1 = createWorkflowArtefact()
        WorkflowArtefact workflowArtefact2 = createWorkflowArtefact()
        WorkflowArtefact workflowArtefact3 = createWorkflowArtefact()
        createBamFile([
                workflowArtefact: workflowArtefact1,
                workPackage     : createMergingWorkPackage([sample: createSample([sampleType: sampleType1, individual: createIndividual([project: project])])]),
        ])
        createBamFile([
                workflowArtefact: workflowArtefact2,
                workPackage     : createMergingWorkPackage([sample: createSample([sampleType: sampleType2, individual: createIndividual([project: project])])]),
        ])
        createBamFile([
                workflowArtefact: workflowArtefact3,
                workPackage     : createMergingWorkPackage([sample: createSample([sampleType: sampleType3, individual: createIndividual([project: project])])]),
        ])
        createBamFile([workPackage: createMergingWorkPackage([sample: createSample([individual: createIndividual([project: project])])])])
        createBamFile([workPackage: createMergingWorkPackage([sample: createSample([sampleType: sampleType3])])])
        createBamFile()
        createWorkflow(name: PanCancerWorkflow.WORKFLOW)
        createWorkflow(name: RnaAlignmentWorkflow.WORKFLOW)

        when:
        service.triggerWorkflowByProjectAndSampleTypes(project, [sampleType1, sampleType2, sampleType3] as Set)

        then:
        1 * service.allDecider.decide { data -> data.toSet() == [workflowArtefact1, workflowArtefact2, workflowArtefact3] as Set }
    }

    void "test getAllDecidersWithActions"() {
        given:
        service.allDecider = new AllDecider()
        service.allDecider.applicationContext = applicationContext

        when:
        List<DeciderWithActions> result = service.allDecidersWithActions

        then:
        result.size() == service.allDecider.deciders.size()

        result.each { DeciderWithActions deciderWithActions ->
            // Only the FastqcDecider needs to be checked specifically: having only two actions
            if (deciderWithActions.name == FastqcDecider.simpleName) {
                assert(deciderWithActions.createActions == [DeciderCreateWorkflowAction.CREATE_MISSING, DeciderCreateWorkflowAction.SKIP])
            } else {
                assert(deciderWithActions.createActions == DeciderCreateWorkflowAction.values() as List)
            }
        }
    }

    void "getTableData, with empty input, returns empty rows"() {
        when:
        TriggerWorkflowsTableData result = service.getTableData(ids)

        then:
        result.bamRows == []
        result.analysisRows == []

        where:
        ids << [[], null]
    }

    void "getTableData, returns SNV, Indel, ACEseq and Sophia analyses together and excludes other analysis types"() {
        given:
        BamFilePairAnalysis snv = SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        BamFilePairAnalysis indel = IndelDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        BamFilePairAnalysis aceseq = AceseqDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        BamFilePairAnalysis sophia = SophiaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        BamFilePairAnalysis runYapsa = RunYapsaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        List<BamFilePairAnalysis> analyses = [snv, indel, aceseq, sophia, runYapsa]

        when:
        TriggerWorkflowsTableData result = service.getTableData(analyses.collectMany { [it.sampleType1BamFile.id, it.sampleType2BamFile.id] })

        then:
        result.analysisRows.collect { [it.analysisType, it.id] } as Set == [
                ['SNV', snv.id],
                ['Indel', indel.id],
                ['ACEseq', aceseq.id],
                ['Sophia', sophia.id],
        ] as Set
        result.bamRows.size() == 10
        result.bamRows.find { it.id == runYapsa.sampleType1BamFile.id }.analysisIds == []
        result.bamRows.find { it.id == snv.sampleType1BamFile.id }.analysisIds == [snv.id]
    }

    @Unroll
    void "getTableData, when #description of the analysis is selected, returns the analysis #expectedCount time(s) with both BAM IDs"() {
        given:
        BamFilePairAnalysis analysis = SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        RoddyBamFile otherBamFile = createBamFile()
        Map<String, Long> bamIds = [
                disease: analysis.sampleType1BamFile.id,
                control: analysis.sampleType2BamFile.id,
                other  : otherBamFile.id,
        ]

        when:
        TriggerWorkflowsTableData result = service.getTableData(selection.collect { bamIds[it] })

        then:
        result.analysisRows.size() == expectedCount
        result.analysisRows.every {
            it.id == analysis.id && it.diseaseBamId == bamIds.disease && it.controlBamId == bamIds.control
        }
        result.bamRows*.id == selection.unique(false).collect { bamIds[it] }

        where:
        description              | selection                                         || expectedCount
        'only the disease BAM'   | ['disease']                                       || 1
        'only the control BAM'   | ['control']                                       || 1
        'both BAMs'              | ['disease', 'control']                            || 1
        'both BAMs repeatedly'   | ['disease', 'control', 'disease', 'control']      || 1
        'neither BAM'            | ['other']                                         || 0
    }

    void "getTableData, excludes withdrawn analyses and keeps all non-withdrawn historical analyses regardless of processing state"() {
        given:
        BamFilePairAnalysis analysis1 = SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.IN_PROGRESS)
        BamFilePairAnalysis analysis2 = SnvDomainFactory.INSTANCE.createInstanceWithSameSamplePair(analysis1)
        BamFilePairAnalysis analysis3 = IndelDomainFactory.INSTANCE.createInstanceWithSameSamplePair(analysis1)
        BamFilePairAnalysis withdrawnAnalysis = SnvDomainFactory.INSTANCE.createInstanceWithSameSamplePair(analysis1)
        withdrawnAnalysis.withdrawn = true
        withdrawnAnalysis.save(flush: true)

        when:
        TriggerWorkflowsTableData result = service.getTableData([analysis1.sampleType1BamFile.id])

        then:
        result.analysisRows*.id as Set == [analysis1.id, analysis2.id, analysis3.id] as Set
        result.bamRows.size() == 1
        result.bamRows.first().analysisIds == [analysis1.id, analysis2.id, analysis3.id].sort()
    }

    void "getTableData, returns all projected analysis fields"() {
        given:
        Project project = createProject(state: Project.State.CLOSED)
        SpeciesWithStrain species = createSpeciesWithStrain()
        Individual individual = createIndividual(project: project, species: species)
        SeqType seqType = createSeqType(displayName: 'WGS', libraryLayout: SequencingReadType.PAIRED, singleCell: false)
        ReferenceGenome referenceGenome = createReferenceGenome()
        LibraryPreparationKit kit = createLibraryPreparationKit()
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(sample: createSample(individual: individual), seqType: seqType,
                referenceGenome: referenceGenome, libraryPreparationKit: kit)
        MergingWorkPackage controlMwp = createMergingWorkPackage(sample: createSample(individual: individual), seqType: seqType,
                referenceGenome: referenceGenome, libraryPreparationKit: kit)
        RoddyBamFile diseaseBamFile = createBamFile(workPackage: diseaseMwp)
        RoddyBamFile controlBamFile = createBamFile(workPackage: controlMwp)
        BamFilePairAnalysis analysis = SophiaDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(
                sampleType1BamFile: diseaseBamFile, sampleType2BamFile: controlBamFile)

        when:
        TriggerWorkflowsTableData result = service.getTableData([diseaseBamFile.id])

        then:
        result.analysisRows == [
                new TriggerWorkflowsAnalysisRow(
                        analysisType: 'Sophia',
                        id: analysis.id,
                        project: project.displayName,
                        individual: individual.displayName,
                        sampleType: "${diseaseMwp.sampleType.displayName} \u2013 ${controlMwp.sampleType.displayName}".toString(),
                        seqType: 'WGS PAIRED bulk',
                        libraryPreparationKits: kit.name,
                        species: species.displayName,
                        referenceGenomes: referenceGenome.name,
                        diseaseBamId: diseaseBamFile.id,
                        controlBamId: controlBamFile.id,
                ),
        ]
        project.displayName.endsWith('(closed)')
    }

    @Unroll
    void "getTableData, returns the full sequencing type label '#expected' in analysis and BAM rows"() {
        given:
        SeqType seqType = createSeqType(displayName: 'Type', libraryLayout: libraryLayout, singleCell: singleCell)
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(seqType: seqType)
        MergingWorkPackage controlMwp = createMergingWorkPackage(diseaseMwp)
        BamFilePairAnalysis analysis = SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(
                sampleType1BamFile: createBamFile(workPackage: diseaseMwp), sampleType2BamFile: createBamFile(workPackage: controlMwp))

        when:
        TriggerWorkflowsTableData result = service.getTableData([analysis.sampleType1BamFile.id, analysis.sampleType2BamFile.id])

        then:
        seqType.displayNameWithLibraryLayout == expected
        result.analysisRows*.seqType == [expected]
        result.bamRows*.seqType == [expected, expected]

        where:
        libraryLayout               | singleCell || expected
        SequencingReadType.PAIRED   | false      || 'Type PAIRED bulk'
        SequencingReadType.SINGLE   | false      || 'Type SINGLE bulk'
        SequencingReadType.MATE_PAIR | false     || 'Type MATE_PAIR bulk'
        SequencingReadType.PAIRED   | true       || 'Type PAIRED single cell'
    }

    void "getTableData, aggregates distinct non-empty metadata of both BAMs in lexical order"() {
        given:
        SpeciesWithStrain mainSpecies = createSpeciesWithStrain(species: createSpecies(speciesCommonName: createSpeciesCommonName(name: 'B common')))
        SpeciesWithStrain mixedIn1 = createSpeciesWithStrain(species: createSpecies(speciesCommonName: createSpeciesCommonName(name: 'A common')))
        SpeciesWithStrain mixedIn2 = createSpeciesWithStrain(species: createSpecies(speciesCommonName: createSpeciesCommonName(name: 'C common')))
        Individual individual = createIndividual(species: mainSpecies)
        Sample diseaseSample = createSample(individual: individual, mixedInSpecies: [mixedIn1, mixedIn2] as Set)
        Sample controlSample = createSample(individual: individual, mixedInSpecies: [mixedIn2] as Set)
        LibraryPreparationKit kitA = createLibraryPreparationKit(name: 'kit A')
        LibraryPreparationKit kitB = createLibraryPreparationKit(name: 'kit B')
        LibraryPreparationKit kitC = createLibraryPreparationKit(name: 'kit C')
        ReferenceGenome referenceGenome1 = createReferenceGenome(name: 'ref 2')
        ReferenceGenome referenceGenome2 = createReferenceGenome(name: 'ref 1')
        SeqType seqType = createSeqType()
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(sample: diseaseSample, seqType: seqType, referenceGenome: referenceGenome1,
                libraryPreparationKit: null)
        MergingWorkPackage controlMwp = createMergingWorkPackage(sample: controlSample, seqType: seqType, referenceGenome: referenceGenome2,
                libraryPreparationKit: null)
        RoddyBamFile diseaseBamFile = createBamFileWithSeqTrackKits(diseaseMwp, [kitB, kitA, kitB, null])
        RoddyBamFile controlBamFile = createBamFileWithSeqTrackKits(controlMwp, [kitC, kitB])
        BamFilePairAnalysis analysis = AceseqDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(
                sampleType1BamFile: diseaseBamFile, sampleType2BamFile: controlBamFile)
        AceseqDomainFactory.INSTANCE.createInstanceWithSameSamplePair(analysis)

        when:
        TriggerWorkflowsTableData result = service.getTableData([diseaseBamFile.id, controlBamFile.id])

        then:
        result.analysisRows.size() == 2
        result.analysisRows.every {
            it.libraryPreparationKits == 'kit A, kit B, kit C' &&
                    it.species == [mainSpecies, mixedIn1, mixedIn2]*.displayName.sort().join(', ') &&
                    it.referenceGenomes == 'ref 1, ref 2'
        }
        TriggerWorkflowsBamRow diseaseRow = result.bamRows.find { it.id == diseaseBamFile.id }
        diseaseRow.libPrepKit == ''
        diseaseRow.species == mainSpecies.displayName
        diseaseRow.mixedInSpecies == [mixedIn1, mixedIn2]*.displayName.sort().join(', ')
        diseaseRow.referenceGenome == 'ref 2'
    }

    void "getTableData, collects kits from the SeqTracks actually contained in the BAM, not from SeqTracks added to the work package later"() {
        given:
        LibraryPreparationKit kitA = createLibraryPreparationKit(name: 'kit A')
        LibraryPreparationKit kitB = createLibraryPreparationKit(name: 'kit B')
        LibraryPreparationKit laterKit = createLibraryPreparationKit(name: 'kit later')
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(libraryPreparationKit: null)
        MergingWorkPackage controlMwp = createMergingWorkPackage(diseaseMwp, [libraryPreparationKit: null])
        RoddyBamFile diseaseBamFile = createBamFileWithSeqTrackKits(diseaseMwp, [kitA, kitB])
        RoddyBamFile controlBamFile = createBamFileWithSeqTrackKits(controlMwp, [kitA])
        createSeqTrackWithKit(diseaseMwp, laterKit)
        BamFilePairAnalysis analysis = IndelDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(
                sampleType1BamFile: diseaseBamFile, sampleType2BamFile: controlBamFile)

        when:
        TriggerWorkflowsTableData result = service.getTableData([controlBamFile.id])

        then:
        result.analysisRows*.id == [analysis.id]
        result.analysisRows.first().libraryPreparationKits == 'kit A, kit B'
    }

    void "getTableData, renders missing optional metadata as empty text"() {
        given:
        Individual individual = createIndividual(species: null)
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(sample: createSample(individual: individual), libraryPreparationKit: null)
        MergingWorkPackage controlMwp = createMergingWorkPackage(diseaseMwp, [libraryPreparationKit: null])
        RoddyBamFile diseaseBamFile = createBamFileWithSeqTrackKits(diseaseMwp, [null])
        RoddyBamFile controlBamFile = createBamFileWithSeqTrackKits(controlMwp, [null])
        SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(sampleType1BamFile: diseaseBamFile, sampleType2BamFile: controlBamFile)

        when:
        TriggerWorkflowsTableData result = service.getTableData([diseaseBamFile.id])

        then:
        TriggerWorkflowsAnalysisRow analysisRow = result.analysisRows.first()
        analysisRow.libraryPreparationKits == ''
        analysisRow.species == ''
        TriggerWorkflowsBamRow bamRow = result.bamRows.first()
        bamRow.libPrepKit == ''
        bamRow.species == ''
        bamRow.mixedInSpecies == ''
    }

    void "getTableData, keeps the existing BAM row metadata"() {
        given:
        Sample sample = createSample()
        SeqType seqType = createSeqType()
        SeqPlatformGroup seqPlatformGroup = createProjectSpecificSeqPlatformGroup(sample.project, seqType, [
                createSeqPlatform(name: 'platform B'),
                createSeqPlatform(name: 'platform A', seqPlatformModelLabel: null),
        ])
        SeqPlatformGroup globalSeqPlatformGroup = createSeqPlatformGroup()
        RoddyBamFile bamFile = createBamFile(workPackage: createMergingWorkPackage(sample: sample, seqType: seqType, seqPlatformGroup: seqPlatformGroup),
                withdrawn: true)
        RoddyBamFile globalBamFile = createBamFile(workPackage: createMergingWorkPackage(seqPlatformGroup: globalSeqPlatformGroup))

        when:
        TriggerWorkflowsTableData result = service.getTableData([bamFile.id, globalBamFile.id])

        then:
        result.bamRows == [
                new TriggerWorkflowsBamRow(
                        id: bamFile.id,
                        project: bamFile.project.displayName,
                        individual: bamFile.individual.displayName,
                        sampleType: bamFile.sampleType.displayName,
                        seqType: bamFile.seqType.displayNameWithLibraryLayout,
                        withdrawn: true,
                        libPrepKit: bamFile.workPackage.libraryPreparationKit.name,
                        species: bamFile.individual.species.displayName,
                        mixedInSpecies: '',
                        referenceGenome: bamFile.referenceGenome.name,
                        seqPlatformGroup: seqPlatformGroup.toString(),
                        analysisIds: [],
                ),
                new TriggerWorkflowsBamRow(
                        id: globalBamFile.id,
                        project: globalBamFile.project.displayName,
                        individual: globalBamFile.individual.displayName,
                        sampleType: globalBamFile.sampleType.displayName,
                        seqType: globalBamFile.seqType.displayNameWithLibraryLayout,
                        withdrawn: false,
                        libPrepKit: globalBamFile.workPackage.libraryPreparationKit.name,
                        species: globalBamFile.individual.species.displayName,
                        mixedInSpecies: '',
                        referenceGenome: globalBamFile.referenceGenome.name,
                        seqPlatformGroup: globalSeqPlatformGroup.toString(),
                        analysisIds: [],
                ),
        ]
        seqPlatformGroup.toString().contains("'platform A unknown kit', 'platform B ")
    }

    void "getTableData, with external BAMs only, returns their rows, work package kit and eligible analyses"() {
        given:
        LibraryPreparationKit kit = createLibraryPreparationKit(name: 'external kit')
        SamplePair samplePair = SnvDomainFactory.INSTANCE.createSamplePairWithExternallyProcessedBamFiles()
        [samplePair.mergingWorkPackage1, samplePair.mergingWorkPackage2].each {
            it.libraryPreparationKit = kit
            it.save(flush: true)
        }
        BamFilePairAnalysis analysis = SnvDomainFactory.INSTANCE.createInstance(samplePair)
        AbstractBamFile diseaseBamFile = analysis.sampleType1BamFile
        ExternallyProcessedBamFile otherExternalBamFile = ExternalBamFactoryInstance.INSTANCE.createBamFile()

        when:
        TriggerWorkflowsTableData result = service.getTableData([diseaseBamFile.id, otherExternalBamFile.id])

        then:
        result.bamRows*.id == [diseaseBamFile.id, otherExternalBamFile.id]
        TriggerWorkflowsBamRow bamRow = result.bamRows.first()
        bamRow.libPrepKit == 'external kit'
        bamRow.seqPlatformGroup == ''
        bamRow.seqType == diseaseBamFile.seqType.displayNameWithLibraryLayout
        bamRow.analysisIds == [analysis.id]
        result.bamRows.last().analysisIds == []
        result.analysisRows*.id == [analysis.id]
        result.analysisRows.first().libraryPreparationKits == 'external kit'
        result.analysisRows.first().controlBamId == analysis.sampleType2BamFile.id
    }

    void "getTableData, with mixed internal and external BAMs, returns rows for all of them"() {
        given:
        RoddyBamFile roddyBamFile = createBamFile()
        ExternallyProcessedBamFile externalBamFile = ExternalBamFactoryInstance.INSTANCE.createBamFile()

        when:
        TriggerWorkflowsTableData result = service.getTableData([roddyBamFile.id, externalBamFile.id])

        then:
        result.bamRows*.id == [roddyBamFile.id, externalBamFile.id]
        result.bamRows.last().individual == externalBamFile.individual.displayName
        result.bamRows.last().referenceGenome == externalBamFile.referenceGenome.name
        result.analysisRows == []
    }

    @Unroll
    void "getTableData, uses scalar batch queries without loading entities (#description)"() {
        given:
        List<Long> smallIds = createMeasurementBamIds(external, 1)
        List<Long> largeIds = createMeasurementBamIds(external, 4)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        when:
        Map<String, Long> small = measure(smallIds)
        Map<String, Long> large = measure(largeIds)
        log.info("Table data measurement (${description}): small (${smallIds.size()} BAMs) ${small}, larger (${largeIds.size()} BAMs) ${large}")

        then:
        small.entityLoads == 0
        large.entityLoads == 0
        small.rows < large.rows
        small.queries == large.queries

        where:
        description          | external
        'internal analyses'  | false
        'external BAMs only' | true
    }

    private List<Long> createMeasurementBamIds(boolean external, int count) {
        return (1..count).collectMany {
            external ? [createFullyPopulatedExternalBamFile().id] : createFullyPopulatedAnalysisBamIds()
        }
    }

    private Map<String, Long> measure(List<Long> bamFileIds) {
        Statistics statistics = sessionFactory.statistics
        boolean statisticsEnabled = statistics.statisticsEnabled
        try {
            statistics.statisticsEnabled = true
            statistics.clear()
            TriggerWorkflowsTableData result = service.getTableData(bamFileIds)
            return [
                    entityLoads: statistics.entityLoadCount,
                    queries    : statistics.prepareStatementCount,
                    rows       : (long) (result.bamRows.size() + result.analysisRows.size()),
            ]
        } finally {
            statistics.statisticsEnabled = statisticsEnabled
            sessionFactory.currentSession.clear()
        }
    }

    private List<Long> createFullyPopulatedAnalysisBamIds() {
        Individual individual = createIndividual()
        SeqType seqType = createSeqType()
        SeqPlatformGroup seqPlatformGroup = createProjectSpecificSeqPlatformGroup(individual.project, seqType, [createSeqPlatform()])
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(seqPlatformGroup: seqPlatformGroup, seqType: seqType,
                sample: createSample(individual: individual, mixedInSpecies: [createSpeciesWithStrain()] as Set))
        MergingWorkPackage controlMwp = createMergingWorkPackage(diseaseMwp, [seqPlatformGroup: seqPlatformGroup,
                sample: createSample(individual: individual, mixedInSpecies: [createSpeciesWithStrain()] as Set)])
        BamFilePairAnalysis analysis = SnvDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles(
                sampleType1BamFile: createBamFile(workPackage: diseaseMwp), sampleType2BamFile: createBamFile(workPackage: controlMwp))
        IndelDomainFactory.INSTANCE.createInstanceWithSameSamplePair(analysis)
        return [analysis.sampleType1BamFile.id, analysis.sampleType2BamFile.id]
    }

    private ExternallyProcessedBamFile createFullyPopulatedExternalBamFile() {
        ExternalMergingWorkPackage mwp = ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage(
                libraryPreparationKit: createLibraryPreparationKit(),
                sample: createSample(mixedInSpecies: [createSpeciesWithStrain()] as Set),
        )
        return ExternalBamFactoryInstance.INSTANCE.createBamFile(workPackage: mwp)
    }

    private SeqPlatformGroup createProjectSpecificSeqPlatformGroup(Project project, SeqType seqType, List<SeqPlatform> seqPlatforms) {
        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup(mergingCriteria: createMergingCriteriaLazy(
                project: project,
                seqType: seqType,
                useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC,
        ))
        seqPlatforms.each {
            it.addToSeqPlatformGroups(seqPlatformGroup)
            it.save(flush: true)
        }
        return seqPlatformGroup
    }

    private RoddyBamFile createBamFileWithSeqTrackKits(MergingWorkPackage mwp, List<LibraryPreparationKit> kits) {
        createMergingCriteriaLazy(project: mwp.project, seqType: mwp.seqType, useLibPrepKit: false)
        List<SeqTrack> seqTracks = kits.collect { LibraryPreparationKit kit ->
            createSeqTrackWithKit(mwp, kit)
        }
        return createBamFile(workPackage: mwp, seqTracks: seqTracks)
    }

    private SeqTrack createSeqTrackWithKit(MergingWorkPackage mwp, LibraryPreparationKit kit) {
        return DomainFactory.createSeqTrackWithFastqFiles(mwp, [
                libraryPreparationKit: kit,
                kitInfoReliability   : kit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
        ])
    }
}
