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
package de.dkfz.tbi.otp.workflowExecution

import grails.test.hibernate.HibernateSpec
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.IsPipeline
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.*

import java.time.LocalDate

abstract class AbstractAnalysisArtefactServiceSpec<T> extends HibernateSpec implements WorkflowSystemDomainFactory, IsPipeline {

    @Override
    List<Class> getDomainClasses() {
        return [
                AceseqInstance,
                ExternallyProcessedBamFile,
                ExternalMergingWorkPackage,
                FastqFile,
                IndelCallingInstance,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddySnvCallingInstance,
                RunYapsaConfig,
                RunYapsaInstance,
                SampleTypePerProject,
                SeqTrack,
                SophiaInstance,
                WorkflowArtefact,
                WorkflowVersionSelector,
        ]
    }

    protected AnalysisArtefactService analysisArtefactService

    protected WorkflowArtefact workflowArtefact1
    protected WorkflowArtefact workflowArtefact2
    protected WorkflowArtefact workflowArtefactRelated
    protected WorkflowArtefact workflowArtefactNew
    protected WorkflowArtefact workflowArtefactRelatedNew

    protected WorkflowArtefact workflowArtefactAnalysis
    protected WorkflowArtefact workflowArtefactAnalysisNew

    protected AbstractBamFile bamFile1
    protected AbstractBamFile bamFile2
    protected AbstractBamFile bamFileRelated
    protected AbstractBamFile bamFileNew
    protected AbstractBamFile bamFileRelatedNew

    protected BamFilePairAnalysis analysisRelated
    protected BamFilePairAnalysis analysisRelatedNew

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

        workflowArtefactNew = createWorkflowArtefact([artefactType: ArtefactType.BAM, producedBy: createWorkflowRun(workflowVersion: createWorkflowVersion())])
        bamFileNew = createBamFile([
                workflowArtefact: workflowArtefactNew,
                workPackage     : createMergingWorkPackage([
                        seqType: createSeqTypePaired(),
                ]),
        ])
        if (!(bamFileNew instanceof ExternallyProcessedBamFile)) {
            bamFileNew.config = null
            bamFileNew.save(flush: true)
        }
    }

    void setupDataWithRelated() {
        setupData()

        // artefact related to input, but not part of the artefact input
        workflowArtefactRelated = createWorkflowArtefact([artefactType: ArtefactType.BAM])
        bamFileRelated = createCorrespondingBamFile(bamFile1, [
                workflowArtefact: workflowArtefactRelated,
        ])
        workflowArtefactRelatedNew = createWorkflowArtefact([artefactType: ArtefactType.BAM, producedBy: createWorkflowRun(workflowVersion: createWorkflowVersion())])
        bamFileRelatedNew = createCorrespondingBamFile(bamFileNew, [
                workflowArtefact: workflowArtefactRelatedNew,
        ])

        if (!(bamFileRelatedNew instanceof ExternallyProcessedBamFile)) {
            bamFileRelatedNew.config = null
            bamFileRelatedNew.save(flush: true)
        }
    }

    void setupDataWithAnalysis(AbstractAnalysisDomainFactory<? extends BamFilePairAnalysis> factory, ArtefactType analysisArtefactType) {
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
                config            : factory.createConfig([
                        seqType: bamFile1.seqType,
                        project: bamFile1.project,
                ]),
        ])

        workflowArtefactAnalysisNew = createWorkflowArtefact([artefactType: ArtefactType.BAM, producedBy: createWorkflowRun(workflowVersion: createWorkflowVersion())])
        analysisRelatedNew = factory.createInstance([
                workflowArtefact  : workflowArtefactAnalysisNew,
                samplePair        : factory.createSamplePair([
                        mergingWorkPackage1: bamFileNew.workPackage,
                        mergingWorkPackage2: bamFileRelatedNew.workPackage,
                ]),
                sampleType1BamFile: bamFileNew,
                sampleType2BamFile: bamFileRelatedNew,
        ])
    }

    void "fetchBamFileArtefacts, when called for workflowArtefacts and seqTypes in old system, then return AnalysisBamFileArtefactData of expected BamFile"() {
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

    void "fetchBamFileArtefacts, when called for workflowArtefacts and seqTypes in new system, then return AnalysisBamFileArtefactData of expected BamFile"() {
        given:
        setupData()

        AnalysisBamFileArtefactData expected = createAnalysisBamFileArtefactData(bamFileNew)

        when:
        List<AnalysisBamFileArtefactData> result = analysisArtefactService.fetchBamFileArtefacts([
                workflowArtefactNew,
        ], [
                bamFileNew.seqType,
        ])

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

    void "fetchRelatedBamFilesArtefactsForBamFiles, when called for bamFiles in old system, then return AnalysisBamFileArtefactData of expected BamFiles"() {
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

    void "fetchRelatedBamFilesArtefactsForBamFiles, when called for bamFiles in new system, then return AnalysisBamFileArtefactData of expected BamFiles"() {
        given:
        setupDataWithRelated()

        List<AnalysisBamFileArtefactData> expected = [
                createAnalysisBamFileArtefactData(bamFileRelatedNew),
        ]

        when:
        List<AnalysisBamFileArtefactData> result = analysisArtefactService.fetchRelatedBamFilesArtefactsForBamFiles([bamFileNew])

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
    void "fetchRelatedAnalysisArtefactsForBamFiles, when called #artefactType for bamFiles in old system, then return AnalysisAnalysisArtefactData of expected analysis"() {
        given:
        setupDataWithAnalysis(factory, artefactType)

        List<AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis>> expected = [
                createAnalysisAnalysisArtefactData(analysisRelated),
        ]

        when:
        List<AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis>> result =
                analysisArtefactService.fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], factory.instanceClass)

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
    void "fetchRelatedAnalysisArtefactsForBamFiles, when called #artefactType for bamFiles in new system, then return AnalysisAnalysisArtefactData of expected analysis"() {
        given:
        setupDataWithAnalysis(factory, artefactType)

        List<AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis>> expected = [
                createAnalysisAnalysisArtefactData(analysisRelatedNew),
        ]

        when:
        List<AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis>> result =
                analysisArtefactService.fetchRelatedAnalysisArtefactsForBamFiles([bamFileNew], factory.instanceClass)

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
        List<AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis>> result = analysisArtefactService.fetchRelatedAnalysisArtefactsForBamFiles([], factory.instanceClass)

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

        WorkflowVersion workflowVersion = createWorkflowVersion([
                allowedReferenceGenomes: [createReferenceGenome(), createReferenceGenome()],
        ])

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
                mergingWorkPackage2: createMergingWorkPackage(bamFile1.workPackage),
        ])

        SamplePair samplePair3 = SnvDomainFactory.INSTANCE.createSamplePair([
                mergingWorkPackage1: createMergingWorkPackage(bamFileRelated.workPackage),
                mergingWorkPackage2: bamFileRelated.workPackage,
        ])
        AbstractMergingWorkPackage mwp = createMergingWorkPackage()
        SnvDomainFactory.INSTANCE.createSamplePair(
                mergingWorkPackage1: mwp,
                mergingWorkPackage2: createMergingWorkPackage(mwp),
        )

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

        AbstractBamFile bamFile3 = createBamFile()
        AbstractBamFile bamFile4 = createBamFile()

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
        String workflowVersion = bamFile.workflowArtefact?.producedBy?.workflowVersion?.workflowVersion ?:
                (bamFile instanceof ExternallyProcessedBamFile ? null : bamFile.config?.programVersion)

        return new AnalysisBamFileArtefactData(
                bamFile.workflowArtefact,
                bamFile,
                workflowVersion,
                bamFile.project,
                bamFile.seqType,
                bamFile.individual,
                bamFile.sampleType,
                bamFile.sample,
                mergingWorkPackage,
                mergingWorkPackage.referenceGenome,
                seqPlatformGroup
        )
    }

    protected <T extends BamFilePairAnalysis> AnalysisAnalysisArtefactData<T> createAnalysisAnalysisArtefactData(T analysis) {
        String workflowVersion = analysis.workflowArtefact?.producedBy?.workflowVersion?.workflowVersion ?:
                analysis.config?.programVersion?.split(':')?.last()
        return new AnalysisAnalysisArtefactData(
                analysis.workflowArtefact,
                analysis,
                workflowVersion,
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
