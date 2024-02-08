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
package de.dkfz.tbi.otp.domainFactory.pipelines.analysis

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.DomainFactoryHelper
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryInstance
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

abstract class AbstractAnalysisDomainFactory<T extends BamFilePairAnalysis> implements DomainFactoryHelper, DomainFactoryCore {

    static final String DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY = 'exec_123456_123456789_test_test'
    static final double DEFAULT_COVERAGE = 30.0

    ProcessingThresholds createProcessingThresholds(Map properties = [:]) {
        return createDomainObject(ProcessingThresholds, [
                project      : { createProject() },
                seqType      : { createSeqType() },
                sampleType   : { createSampleType() },
                coverage     : DEFAULT_COVERAGE,
                numberOfLanes: 3,
        ], properties)
    }

    ProcessingThresholds findOrCreateProcessingThresholdsForMergingWorkPackage(AbstractMergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        return findOrCreateDomainObject(ProcessingThresholds, [
                project   : mergingWorkPackage.project,
                seqType   : mergingWorkPackage.seqType,
                sampleType: mergingWorkPackage.sampleType,
        ], properties, [:])
    }

    ProcessingThresholds findOrCreateProcessingThresholdsForSeqTrack(SeqTrack seqTrack, Map properties = [:]) {
        return findOrCreateDomainObject(ProcessingThresholds, [
                project   : seqTrack.project,
                seqType   : seqTrack.seqType,
                sampleType: seqTrack.sampleType,
        ], properties, [:])
    }

    ProcessingThresholds findOrCreateProcessingThresholdsForBamFile(AbstractBamFile bamFile, Map properties = [:]) {
        return findOrCreateProcessingThresholdsForMergingWorkPackage(bamFile.mergingWorkPackage, properties)
    }

    private Map getDefaultSampleTypeProperties() {
        return [
                project   : { createProject() },
                sampleType: { createSampleType() },
                category  : SampleTypePerProject.Category.DISEASE,
        ]
    }

    SampleTypePerProject createSampleTypePerProject(Map properties = [:]) {
        return createDomainObject(SampleTypePerProject, defaultSampleTypeProperties, properties)
    }

    SampleTypePerProject findOrCreateSampleTypePerProject(Map properties = [:]) {
        return findOrCreateDomainObject(SampleTypePerProject, properties, defaultSampleTypeProperties, [:])
    }

    SampleTypePerProject findOrCreateSampleTypePerProjectForMergingWorkPackage(
            AbstractMergingWorkPackage mergingWorkPackage, SampleTypePerProject.Category category = SampleTypePerProject.Category.DISEASE) {
        return findOrCreateSampleTypePerProject([
                project   : mergingWorkPackage.project,
                sampleType: mergingWorkPackage.sampleType,
                category  : category,
        ])
    }

    SampleTypePerProject findOrCreateSampleTypePerProjectForBamFile(
            AbstractBamFile bamFile, SampleTypePerProject.Category category = SampleTypePerProject.Category.DISEASE) {
        return findOrCreateSampleTypePerProjectForMergingWorkPackage(bamFile.mergingWorkPackage, category)
    }

    SamplePair createSamplePair(Map properties = [:]) {
        AbstractMergingWorkPackage mergingWorkPackage1 = properties.mergingWorkPackage1 ?:
                AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage()
        AbstractMergingWorkPackage mergingWorkPackage2 = properties.mergingWorkPackage2 ?:
                AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage(mergingWorkPackage1)
        findOrCreateSampleTypePerProject(
                sampleType: mergingWorkPackage1.sampleType,
                project: mergingWorkPackage1.project,
        )
        findOrCreateSampleTypePerProject(
                sampleType: mergingWorkPackage2.sampleType,
                project: mergingWorkPackage2.project,
                category: SampleTypePerProject.Category.CONTROL,
        )
        return createSamplePair(mergingWorkPackage1, mergingWorkPackage2, properties)
    }

    SamplePair createSamplePair(AbstractMergingWorkPackage mergingWorkPackage1, AbstractMergingWorkPackage mergingWorkPackage2, Map properties = [:]) {
        return createDomainObject(SamplePair, [
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        ], properties)
    }

    Map createProcessableSamplePair(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties,
                [coverage: DEFAULT_COVERAGE] + bamFile1Properties, [coverage: DEFAULT_COVERAGE] + bamFile2Properties)

        SamplePair samplePair = map.samplePair
        AbstractBamFile bamFile1 = map.sampleType1BamFile
        AbstractBamFile bamFile2 = map.sampleType2BamFile
        bamFile1.mergingWorkPackage.bamFileInProjectFolder = bamFile1
        bamFile2.mergingWorkPackage.bamFileInProjectFolder = bamFile2

        findOrCreateProcessingThresholdsForBamFile(bamFile1, [numberOfLanes: null])
        findOrCreateProcessingThresholdsForBamFile(bamFile2, [numberOfLanes: null])

        return [
                samplePair : samplePair,
                bamFile1   : bamFile1,
                bamFile2   : bamFile2,
        ]
    }

    SamplePair createSamplePairWithBamFiles() {
        MergingWorkPackage tumorMwp = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage(
                seqType: DomainFactory.createWholeGenomeSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                referenceGenome: createReferenceGenome(name: 'hs37d5')
        )
        AbstractBamFile bamFileTumor = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createRoddyBamFile(
                DomainFactory.randomBamFileProperties + [coverage: DEFAULT_COVERAGE], tumorMwp, RoddyBamFile)

        AbstractBamFile bamFileControl = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createRoddyBamFile(
                DomainFactory.randomBamFileProperties + [coverage: DEFAULT_COVERAGE],
                AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage(bamFileTumor.mergingWorkPackage), RoddyBamFile)

        bamFileTumor.mergingWorkPackage.bamFileInProjectFolder = bamFileTumor
        bamFileTumor.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        bamFileControl.mergingWorkPackage.save(flush: true)

        createSampleTypePerProject(
                project: bamFileTumor.project,
                sampleType: bamFileTumor.sampleType,
                category: SampleTypePerProject.Category.DISEASE,
        )

        createSampleTypePerProject(
                project: bamFileControl.project,
                sampleType: bamFileControl.sampleType,
                category: SampleTypePerProject.Category.CONTROL,
        )

        SamplePair samplePair = createSamplePair(bamFileTumor.mergingWorkPackage, bamFileControl.mergingWorkPackage)

        findOrCreateProcessingThresholdsForBamFile(bamFileTumor, [numberOfLanes: null])
        findOrCreateProcessingThresholdsForBamFile(bamFileControl, [numberOfLanes: null])

        return samplePair
    }

    SamplePair createSamplePairWithExternallyProcessedBamFiles(Map bamFileProperties = [:]) {
        ExternalMergingWorkPackage tumorMwp = ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage(
                seqType: DomainFactory.createWholeGenomeSeqType(),
                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
        )

        ExternalMergingWorkPackage controlMwp = ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage(
                seqType: tumorMwp.seqType,
                pipeline: tumorMwp.pipeline,
                referenceGenome: tumorMwp.referenceGenome,
                sample: createSample(
                        individual: tumorMwp.individual
                )
        )

        [
                tumorMwp,
                controlMwp,
        ].each {
            ExternallyProcessedBamFile bamFile = ExternalBamFactoryInstance.INSTANCE.createBamFile(
                    DomainFactory.randomBamFileProperties + [
                            workPackage      : it,
                            coverage         : DEFAULT_COVERAGE,
                            insertSizeFile   : 'insertSize.txt',
                            maximumReadLength: 101,
                    ] + bamFileProperties,
            )
            bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
            assert bamFile.mergingWorkPackage.save(flush: true)
        }

        findOrCreateSampleTypePerProjectForMergingWorkPackage(tumorMwp, SampleTypePerProject.Category.DISEASE)
        findOrCreateSampleTypePerProjectForMergingWorkPackage(controlMwp, SampleTypePerProject.Category.CONTROL)

        SamplePair samplePair = createSamplePair(tumorMwp, controlMwp)

        findOrCreateProcessingThresholdsForMergingWorkPackage(samplePair.mergingWorkPackage1, [numberOfLanes: null, coverage: 10])
        findOrCreateProcessingThresholdsForMergingWorkPackage(samplePair.mergingWorkPackage2, [numberOfLanes: null, coverage: 10])

        return samplePair
    }

    T createInstance(Map properties = [:]) {
        return createDomainObject(instanceClass, [
                processingState: AnalysisProcessingStates.IN_PROGRESS,
                instanceName   : "instance-${nextId}",
        ], properties)
    }

    T createInstance(SamplePair samplePair, Map properties = [:]) {
        return createDomainObject(instanceClass, [
                samplePair        : samplePair,
                processingState   : AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                instanceName      : "instance-${nextId}",
        ], properties)
    }

    T createInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        map += [
                roddyExecutionDirectoryNames: [DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ]
        return createDomainObject(instanceClass, map, properties)
    }

    T createInstanceWithSameSamplePair(BamFilePairAnalysis instance) {
        return createDomainObject(instanceClass, [
                samplePair        : instance.samplePair,
                processingState   : AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: instance.sampleType1BamFile,
                sampleType2BamFile: instance.sampleType2BamFile,
                instanceName      : "instance-${nextId}",
        ], [:])
    }

    abstract protected Class<T> getInstanceClass()

    private Map createAnalysisInstanceWithRoddyBamFilesMapHelper(Map properties, Map bamFile1Properties, Map bamFile2Properties) {
        Pipeline pipeline = DomainFactory.createPanCanPipeline()

        SamplePair samplePair = properties.samplePair
        AbstractBamFile diseaseBamFile = properties.sampleType1BamFile
        AbstractBamFile controlBamFile = properties.sampleType2BamFile

        AbstractMergingWorkPackage diseaseWorkPackage = diseaseBamFile?.mergingWorkPackage
        AbstractMergingWorkPackage controlWorkPackage = controlBamFile?.mergingWorkPackage

        Collection<SeqTrack> diseaseSeqTracks = bamFile1Properties.seqTracks ?: []
        Collection<SeqTrack> controlSeqTracks = bamFile2Properties.seqTracks ?: []

        SeqType seqType = CollectionUtils.atMostOneElement([
                samplePair?.seqType,
                diseaseWorkPackage?.seqType,
                controlWorkPackage?.seqType,
                diseaseSeqTracks*.seqType,
                controlSeqTracks*.seqType,
        ].findAll().flatten().unique(), "All sources have to contain the same seqType") ?: DomainFactory.createWholeGenomeSeqType()

        Sample diseaseSample = CollectionUtils.atMostOneElement([
                samplePair?.mergingWorkPackage1?.sample,
                diseaseWorkPackage?.sample,
                diseaseSeqTracks*.sample,
        ].findAll().flatten().unique(), "All disease sources have to contain the same sample")

        Sample controlSample = CollectionUtils.atMostOneElement([
                samplePair?.mergingWorkPackage2?.sample,
                controlWorkPackage?.sample,
                controlSeqTracks*.sample,
        ].findAll().flatten().unique(), "All control sources have to contain the same sample")

        if (samplePair) {
            if (diseaseWorkPackage) {
                assert samplePair.mergingWorkPackage1 == diseaseWorkPackage
            } else {
                diseaseWorkPackage = samplePair.mergingWorkPackage1
            }
            if (controlWorkPackage) {
                assert samplePair.mergingWorkPackage2 == controlWorkPackage
            } else {
                controlWorkPackage = samplePair.mergingWorkPackage2
            }
        } else {
            if (!controlWorkPackage) {
                Sample sample = controlSample ?:
                        createSample([
                                individual: diseaseWorkPackage?.individual ?: diseaseSample?.individual ?: createIndividual(),
                        ])
                controlWorkPackage = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage([
                        pipeline        : pipeline,
                        statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                        seqType         : seqType,
                        sample          : sample,
                ])
            }
            if (!diseaseWorkPackage) {
                Sample sample = diseaseSample ?:
                        createSample([
                                individual: controlWorkPackage.individual,
                        ])
                diseaseWorkPackage = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage(
                        pipeline: pipeline,
                        statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                        seqType: seqType,
                        sample: sample,
                )
            }
            findOrCreateSampleTypePerProject([
                    project   : diseaseWorkPackage.project,
                    sampleType: diseaseWorkPackage.sampleType,
                    category  : SampleTypePerProject.Category.DISEASE,
            ])
            samplePair = createSamplePair(diseaseWorkPackage, controlWorkPackage)
        }

        diseaseBamFile = diseaseBamFile ?: AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createRoddyBamFile([workPackage: diseaseWorkPackage] +
                bamFile1Properties, RoddyBamFile
        )
        controlBamFile = controlBamFile ?: AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createRoddyBamFile([
                workPackage: controlWorkPackage,
        ] + bamFile2Properties, RoddyBamFile)

        return [
                instanceName      : "instance-${nextId}",
                samplePair        : samplePair,
                sampleType1BamFile: diseaseBamFile,
                sampleType2BamFile: controlBamFile,
        ]
    }
}
