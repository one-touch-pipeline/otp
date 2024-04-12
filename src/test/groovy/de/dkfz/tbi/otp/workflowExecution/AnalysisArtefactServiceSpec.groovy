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
package de.dkfz.tbi.otp.workflowExecution

import grails.test.hibernate.HibernateSpec
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentArtefactData
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.*

import java.time.LocalDate

class AnalysisArtefactServiceSpec<T> extends HibernateSpec implements WorkflowSystemDomainFactory, RoddyPanCancerFactory {

    private AnalysisArtefactService analysisArtefactService

    private WorkflowArtefact workflowArtefact1
    private WorkflowArtefact workflowArtefact2
    private WorkflowArtefact workflowArtefactRelated

    private WorkflowArtefact workflowArtefactAnalysis

    private RoddyBamFile bamFile1
    private RoddyBamFile bamFile2
    private RoddyBamFile bamFileRelated

    private BamFilePairAnalysis analysisRelated

    @Override
    List<Class> getDomainClasses() {
        return [
                AceseqInstance,
                FastqFile,
                IndelCallingInstance,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddySnvCallingInstance,
                RunYapsaInstance,
                SampleTypePerProject,
                SeqTrack,
                SophiaInstance,
                WorkflowArtefact,
                WorkflowVersionSelector,
        ]
    }

    void setup() {
        analysisArtefactService = new AnalysisArtefactService()
    }

    void setupData() {
        // artefact in input and seqType in input
        workflowArtefact1 = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFile1 = createBamFile([workflowArtefact: workflowArtefact1])

        // artefact in input, but wrong seq type
        workflowArtefact2 = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFile2 = createBamFile([
                workflowArtefact: workflowArtefact2,
                workPackage     : createMergingWorkPackage([
                        seqType: createSeqTypePaired(),
                ]),
        ])
    }

    void setupDataWithRelated() {
        setupData()

        // artefact related to input, but not part of the artefact input
        workflowArtefactRelated = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFileRelated = createCorrespondingBamFile(bamFile1, [
                workflowArtefact: workflowArtefactRelated,
        ])
    }

    void setupDataWithAnalysis(AbstractAnalysisDomainFactory<?> factory, ArtefactType analysisArtefactType) {
        setupDataWithRelated()

        workflowArtefactAnalysis = createWorkflowArtefact([artefactType: analysisArtefactType])
        analysisRelated = factory.createInstance([
                workflowArtefact  : workflowArtefactAnalysis,
                samplePair        : factory.createSamplePair([
                        mergingWorkPackage1: bamFile1.workPackage,
                        mergingWorkPackage2: bamFileRelated.workPackage,
                ]),
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFileRelated,
        ])
    }

    void "fetchBamFileArtefacts, when called for workflowArtefacts and seqTypes, then return AnalysisBamFileArtefactData of expected BamFile"() {
        given:
        setupData()

        AnalysisBamFileArtefactData expected = createAnalysisBamFileArtefactData(bamFile1)

        when:
        List<AnalysisBamFileArtefactData> result = analysisArtefactService.fetchBamFileArtefacts([
                workflowArtefact1,
                workflowArtefact2,
        ], [bamFile1.seqType])

        then:
        result.size() == 1
        result.first() == expected
    }

    void "fetchBamFileArtefacts, when called for workflowArtefacts and another seqTypes, then return empty list"() {
        given:
        setupData()

        when:
        List<AnalysisBamFileArtefactData> result = analysisArtefactService.fetchBamFileArtefacts([
                workflowArtefact1,
                workflowArtefact2,
        ], [createSeqTypePaired()])

        then:
        result.empty
    }

    void "fetchBamFileArtefacts, when called for workflowArtefacts and empty seqType list, then return empty list"() {
        given:
        setupData()

        when:
        List<AnalysisBamFileArtefactData> result = analysisArtefactService.fetchBamFileArtefacts([
                workflowArtefact1,
                workflowArtefact2,
        ], [])

        then:
        result.empty
    }

    void "fetchBamFileArtefacts, when called for empty workflowArtefact list, then return empty list"() {
        given:
        setupData()

        when:
        List<AnalysisBamFileArtefactData> result = analysisArtefactService.fetchBamFileArtefacts([], [bamFile1.seqType])

        then:
        result.empty
    }

    void "fetchRelatedBamFilesArtefactsForBamFiles, when called for bamFiles, then return AnalysisBamFileArtefactData of expected BamFiles"() {
        given:
        setupDataWithRelated()

        List<AnalysisBamFileArtefactData> expected = [
                createAnalysisBamFileArtefactData(bamFileRelated),
        ]

        when:
        List<AnalysisBamFileArtefactData> result = analysisArtefactService.fetchRelatedBamFilesArtefactsForBamFiles([bamFile1])

        then:
        result.size() == 1
        TestCase.assertContainSame(result, expected)
    }

    void "fetchRelatedBamFilesArtefactsForBamFiles, when called for empty bamFile list, then return empty list"() {
        given:
        setupDataWithRelated()

        when:
        List<AnalysisBamFileArtefactData> result = analysisArtefactService.fetchRelatedBamFilesArtefactsForBamFiles([])

        then:
        result.empty
    }

    @Unroll
    void "fetchRelatedAnalysisArtefactsForBamFiles, when called #artefactType for bamFiles, then return AnalysisAnalysisArtefactData of expected analysis"() {
        given:
        setupDataWithAnalysis(factory, artefactType)

        List<AlignmentArtefactData<RoddyBamFile>> expected = [
                createAnalysisAnalysisArtefactData(analysisRelated),
        ]

        when:
        List<AlignmentArtefactData<RoddyBamFile>> result = analysisArtefactService.fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], factory.instanceClass)

        then:
        result.size() == 1
        TestCase.assertContainSame(result, expected)

        where:
        factory                        | artefactType
        SnvDomainFactory.INSTANCE      | ArtefactType.SNV
        IndelDomainFactory.INSTANCE    | ArtefactType.INDEL
        SophiaDomainFactory.INSTANCE   | ArtefactType.SOPHIA
        AceseqDomainFactory.INSTANCE   | ArtefactType.ACESEQ
        RunYapsaDomainFactory.INSTANCE | ArtefactType.RUN_YAPSA
    }

    @Unroll
    void "fetchRelatedAnalysisArtefactsForBamFiles, when called #artefactType for empty bamFile list, then return empty list"() {
        given:
        setupDataWithAnalysis(factory, artefactType)

        when:
        List<AlignmentArtefactData<RoddyBamFile>> result = analysisArtefactService.fetchRelatedAnalysisArtefactsForBamFiles([], factory.instanceClass)

        then:
        result.empty

        where:
        factory                        | artefactType
        SnvDomainFactory.INSTANCE      | ArtefactType.SNV
        IndelDomainFactory.INSTANCE    | ArtefactType.INDEL
        SophiaDomainFactory.INSTANCE   | ArtefactType.SOPHIA
        AceseqDomainFactory.INSTANCE   | ArtefactType.ACESEQ
        RunYapsaDomainFactory.INSTANCE | ArtefactType.RUN_YAPSA
    }

    void "fetchWorkflowVersionSelectorForBamFiles, when called for workflow and bamFiles, then return WorkflowVersionSelector"() {
        given:
        setupData()

        WorkflowVersion workflowVersion = createWorkflowVersion()

        WorkflowVersionSelector selector = createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : bamFile1.project,
                seqType        : bamFile1.seqType,
        ])

        createWorkflowVersionSelector([
                project: bamFile1.project,
                seqType: bamFile1.seqType,
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                seqType        : bamFile1.seqType,
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : bamFile1.project,
                seqType        : createSeqTypePaired(),
        ])
        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : bamFile1.project,
                seqType        : bamFile1.seqType,
                deprecationDate: LocalDate.now(),
        ])

        when:
        List<WorkflowVersionSelector> result = analysisArtefactService.fetchWorkflowVersionSelectorForBamFiles(workflowVersion.workflow, [bamFile1])

        then:
        result.size() == 1
        result.first() == selector
    }

    void "fetchWorkflowVersionSelectorForBamFiles, when called for workflow and empty bamFile list, then return empty list"() {
        given:
        setupData()

        WorkflowVersion workflowVersion = createWorkflowVersion()

        createWorkflowVersionSelector([
                workflowVersion: workflowVersion,
                project        : bamFile1.project,
                seqType        : bamFile1.seqType,
        ])

        when:
        List<WorkflowVersionSelector> result = analysisArtefactService.fetchWorkflowVersionSelectorForBamFiles(workflowVersion.workflow, [])

        then:
        result.isEmpty()
    }

    void "fetchSamplePairs, when called for bamFiles, then return SamplePairMap"() {
        given:
        setupDataWithRelated()

        SamplePair samplePair1 = SnvDomainFactory.INSTANCE.createSamplePair([
                mergingWorkPackage1: bamFile1.workPackage,
                mergingWorkPackage2: bamFileRelated.workPackage,
        ])

        SamplePair samplePair2 = SnvDomainFactory.INSTANCE.createSamplePair([
                mergingWorkPackage1: bamFile1.workPackage,
        ])

        SamplePair samplePair3 = SnvDomainFactory.INSTANCE.createSamplePair([
                mergingWorkPackage2: bamFileRelated.workPackage,
        ])
        SnvDomainFactory.INSTANCE.createSamplePair()

        AnalysisGroup analysisGroup1 = new AnalysisGroup(samplePair1.mergingWorkPackage1, samplePair1.mergingWorkPackage2)
        AnalysisGroup analysisGroup2 = new AnalysisGroup(samplePair2.mergingWorkPackage1, samplePair2.mergingWorkPackage2)
        AnalysisGroup analysisGroup3 = new AnalysisGroup(samplePair3.mergingWorkPackage1, samplePair3.mergingWorkPackage2)

        Map<AnalysisGroup, SamplePair> expected = [
                (analysisGroup1): samplePair1,
                (analysisGroup2): samplePair2,
                (analysisGroup3): samplePair3,
        ]

        when:
        Map<AnalysisGroup, SamplePair> result = analysisArtefactService.fetchSamplePairs([bamFile1, bamFileRelated])

        then:
        TestCase.assertContainSame(result, expected)
    }

    void "fetchSamplePairs, when called for empty bamFile list, then return empty map"() {
        given:
        setupDataWithRelated()

        SnvDomainFactory.INSTANCE.createSamplePair([
                mergingWorkPackage1: bamFile1.workPackage,
                mergingWorkPackage2: bamFileRelated.workPackage,
        ])

        when:
        Map<AnalysisGroup, SamplePair> result = analysisArtefactService.fetchSamplePairs([])

        then:
        result.isEmpty()
    }

    void "fetchCategoryPerSampleTypeAndProject, when called for bamFiles, then return CategoryMap"() {
        given:
        setupData()

        RoddyBamFile bamFile3 = createBamFile()
        RoddyBamFile bamFile4 = createBamFile()

        SampleTypePerProject category1 = SnvDomainFactory.INSTANCE.createSampleTypePerProject([
                project   : bamFile1.project,
                sampleType: bamFile1.sampleType,
                category  : SampleTypePerProject.Category.DISEASE,
        ])
        SampleTypePerProject category2 = SnvDomainFactory.INSTANCE.createSampleTypePerProject([
                project   : bamFile2.project,
                sampleType: bamFile2.sampleType,
                category  : SampleTypePerProject.Category.CONTROL,
        ])
        SampleTypePerProject category3 = SnvDomainFactory.INSTANCE.createSampleTypePerProject([
                project   : bamFile3.project,
                sampleType: bamFile3.sampleType,
                category  : SampleTypePerProject.Category.UNDEFINED,
        ])
        SampleTypePerProject category4 = SnvDomainFactory.INSTANCE.createSampleTypePerProject([
                project   : bamFile4.project,
                sampleType: bamFile4.sampleType,
                category  : SampleTypePerProject.Category.IGNORED,
        ])

        ProjectSampleTypeGroup group1 = new ProjectSampleTypeGroup(bamFile1.project, bamFile1.sampleType)
        ProjectSampleTypeGroup group2 = new ProjectSampleTypeGroup(bamFile2.project, bamFile2.sampleType)
        ProjectSampleTypeGroup group3 = new ProjectSampleTypeGroup(bamFile3.project, bamFile3.sampleType)
        ProjectSampleTypeGroup group4 = new ProjectSampleTypeGroup(bamFile4.project, bamFile4.sampleType)

        Map<ProjectSampleTypeGroup, SampleTypePerProject.Category> expected = [
                (group1): category1.category,
                (group2): category2.category,
                (group3): category3.category,
                (group4): category4.category,
        ]

        when:
        Map<ProjectSampleTypeGroup, SampleTypePerProject.Category> result =
                analysisArtefactService.fetchCategoryPerSampleTypeAndProject([bamFile1, bamFile2, bamFile3, bamFile4])

        then:
        TestCase.assertContainSame(result, expected)
    }

    void "fetchCategoryPerSampleTypeAndProject, when called for empty bamFile list, then return empty map"() {
        given:
        setupData()

        SnvDomainFactory.INSTANCE.createSampleTypePerProject([
                project   : bamFile1.project,
                sampleType: bamFile1.sampleType,
                category  : SampleTypePerProject.Category.DISEASE,
        ])

        when:
        Map<ProjectSampleTypeGroup, SampleTypePerProject.Category> result = analysisArtefactService.fetchCategoryPerSampleTypeAndProject([])

        then:
        result.isEmpty()
    }

    protected AnalysisBamFileArtefactData createAnalysisBamFileArtefactData(AbstractBamFile bamFile) {
        AbstractMergingWorkPackage mergingWorkPackage = bamFile.mergingWorkPackage
        SeqPlatformGroup seqPlatformGroup = mergingWorkPackage.class.isAssignableFrom(MergingWorkPackage) ?
                ((MergingWorkPackage) mergingWorkPackage).seqPlatformGroup : null
        return new AnalysisBamFileArtefactData(
                bamFile.workflowArtefact,
                bamFile,
                bamFile.project,
                bamFile.seqType,
                bamFile.individual,
                bamFile.sampleType,
                bamFile.sample,
                mergingWorkPackage,
                seqPlatformGroup
        )
    }

    protected <T extends BamFilePairAnalysis> AnalysisAnalysisArtefactData<T> createAnalysisAnalysisArtefactData(T analysis) {
        return new AnalysisAnalysisArtefactData(
                analysis.workflowArtefact,
                analysis,
                analysis.project,
                analysis.seqType,
                analysis.samplePair,
                analysis.individual,
                analysis.sampleType1BamFile.sampleType,
                analysis.sampleType2BamFile.sampleType,
                analysis.sampleType1BamFile.sample,
                analysis.sampleType2BamFile.sample,
                analysis.sampleType1BamFile,
                analysis.sampleType2BamFile,
        )
    }
}
