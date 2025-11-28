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
package de.dkfz.tbi.otp.workflowTest.analysis

import de.dkfz.tbi.otp.analysis.pair.bamfiles.BamFileSet
import de.dkfz.tbi.otp.analysis.pair.bamfiles.SeqTypeAndInputBamFiles
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SnvDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryInstance
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentLinkFileServiceFactoryService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentWorkFileServiceFactoryService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowTest.AbstractDecidedWorkflowSpec
import de.dkfz.tbi.otp.workflowTest.referenceGenome.UsingReferenceGenome

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

abstract class AbstractAnalysisWorkflowSpec extends AbstractDecidedWorkflowSpec implements DomainFactoryCore, SeqTypeAndInputBamFiles, UsingReferenceGenome {

    AlignmentLinkFileServiceFactoryService alignmentLinkFileServiceFactoryService
    AlignmentWorkFileServiceFactoryService alignmentWorkFileServiceFactoryService
    ReferenceGenomeService referenceGenomeService
    FilestoreService filestoreService

    static final Double COVERAGE = 30.0

    static final String PID = 'stds' // name have to be the same as in the reference data for OTP snv

    AbstractBamFile bamFileControl
    AbstractBamFile bamFileTumor
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    SamplePair samplePair
    SampleType sampleTypeControl
    SampleType sampleTypeTumor
    SeqType seqType
    Set<Species> species
    Set<SpeciesWithStrain> speciesWithStrain
    Workflow workflowAnalysis
    WorkflowVersion workflowVersionAnalysis

    void setupData() {
        workflowAnalysis = CollectionUtils.exactlyOneElement(Workflow.findAllByName(workflowName))
        log.info("Fetch analysis workflow ${workflowAnalysis}")

        WorkflowApiVersion wav = CollectionUtils.exactlyOneElement(
                WorkflowApiVersion.findAllByWorkflow(workflowAnalysis, [sort: 'id', order: 'desc', max: 1]))
        workflowVersionAnalysis = CollectionUtils.exactlyOneElement(WorkflowVersion.findAllByApiVersion(wav, [sort: 'id', order: 'desc', max: 1]))
        workflowVersionAnalysis.supportedSeqTypes.add(seqType)
        workflowVersionAnalysis.allowedReferenceGenomes.add(referenceGenome)
        workflowVersionAnalysis.save(flush: true)
        log.info("Fetch analysis workflow version ${workflowVersionAnalysis}")

        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector([
                project        : project,
                seqType        : seqType,
                workflowVersion: workflowVersionAnalysis,
        ])
        log.info("Create selectedProjectSeqTypeWorkflowVersion ${workflowVersionSelector}")

        createProcessingOptionsForNotification()
    }

    ReferenceGenome createReferenceGenomeAndEntries() {
        speciesWithStrain = [findOrCreateHumanSpecies()] as Set

        ReferenceGenome referenceGenome = createReferenceGenome([
                path                    : referenceGenomeSpecificPath,
                fileNamePrefix          : referenceGenomeFileNamePrefix,
                cytosinePositionsIndex  : referenceGenomeCytosinePositionsIndex,
                chromosomeLengthFilePath: chromosomeLengthFilePath,
                chromosomeSuffix        : '',
                chromosomePrefix        : '',
                species                 : species,
                speciesWithStrain       : speciesWithStrain,
        ])
        ["21", "22"].each { String chromosomeName ->
            DomainFactory.createReferenceGenomeEntry(
                    referenceGenome: referenceGenome,
                    classification: ReferenceGenomeEntry.Classification.CHROMOSOME,
                    name: chromosomeName,
                    alias: chromosomeName,
            )
        }
        return referenceGenome
    }

    // The qa values are taken from the wgs alignment workflow with one lane
    final static Map QC_VALUES = [
            insertSizeMedian  : 406,
            insertSizeCV      : 23,
            properlyPaired    : 1919,
            pairedInSequencing: 2120,
    ].asImmutable()

    final Map createBamFileProperties() {
        return DomainFactory.randomBamFileProperties + [
                coverage            : COVERAGE,
                qcTrafficLightStatus: AbstractBamFile.QcTrafficLightStatus.QC_PASSED,
        ]
    }

    void setupRoddyBamFile() {
        SeqPlatformGroup seqPlatformGroup = createSeqPlatformGroup()
        MergingWorkPackage tumorMwp = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage(
                seqType: seqTypeToUse(),
                pipeline: DomainFactory.createPanCanPipeline(),
                referenceGenome: createReferenceGenomeAndEntries(),
                seqPlatformGroup: seqPlatformGroup,
        )
        MergingWorkPackage controlMwp = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage(
                seqType: tumorMwp.seqType,
                pipeline: tumorMwp.pipeline,
                referenceGenome: tumorMwp.referenceGenome,
                sample: createSample([
                        individual: tumorMwp.individual,
                ]),
                seqPlatformGroup: seqPlatformGroup,
        )

        bamFileTumor = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createRoddyBamFile([workPackage: tumorMwp] + createBamFileProperties(), RoddyBamFile)
        bamFileControl = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createRoddyBamFile(createBamFileProperties() + [
                workPackage: AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackageWithSameProperties(controlMwp),
                config     : bamFileTumor.config,
        ], RoddyBamFile)

        DomainFactory.createRoddyMergedBamQa(bamFileTumor, QC_VALUES)
        DomainFactory.createRoddyMergedBamQa(bamFileControl, QC_VALUES)

        commonBamFileSetup()
        createBedFileAndLibPrepKit()
        linkReferenceGenomeDirectoryToReference(referenceGenome)
    }

    void setupExternalBamFile() {
        ExternalMergingWorkPackage tumorMwp = ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage(
                seqType: seqTypeToUse(),
                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
                referenceGenome: createReferenceGenomeAndEntries(),
        )
        ExternalMergingWorkPackage controlMwp = ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage(
                seqType: tumorMwp.seqType,
                pipeline: tumorMwp.pipeline,
                referenceGenome: tumorMwp.referenceGenome,
                sample: createSample([
                        individual: tumorMwp.individual,
                ]),
        )
        /*
        Some versions of Roddy have trouble parsing the sample type from the bam file name when it contains '_'.
        This is checked for in the SampleType constraints, but only upon creation, not modification.

        Note that Indel is unaffected by this change, as it uses its own sample types, defined in its own subclass.
        This has to be done because Indel checks the sample types in the header of the bam file.
         */
        controlMwp.sampleType.name = 'control_test'
        controlMwp.sampleType.save(flush: true)

        bamFileTumor = ExternalBamFactoryInstance.INSTANCE.createBamFile([
                workPackage      : tumorMwp,
                maximumReadLength: 101,
        ] + createBamFileProperties())

        bamFileControl = ExternalBamFactoryInstance.INSTANCE.createBamFile(createBamFileProperties() + [
                workPackage      : controlMwp,
                maximumReadLength: 101,
        ])
        commonBamFileSetup()
        createBedFileAndLibPrepKit()
        linkReferenceGenomeDirectoryToReference(referenceGenome)
    }

    private void commonBamFileSetup() {
        individual = bamFileTumor.individual
        project = individual.project
        sampleTypeControl = bamFileControl.sampleType
        sampleTypeTumor = bamFileTumor.sampleType
        seqType = bamFileTumor.seqType
        referenceGenome = bamFileControl.referenceGenome

        individual.pid = PID
        individual.save(flush: true)

        [
                bamFileTumor,
                bamFileControl,
        ].each { AbstractBamFile bamFile ->
            WorkflowRun workflowRun = createWorkflowRun([
                    state: WorkflowRun.State.LEGACY,
            ])
            bamFile.workPackage.bamFileInProjectFolder = bamFile
            bamFile.workPackage.save(flush: true)
            bamFile.workflowArtefact = createWorkflowArtefact(
                    [
                            artefactType: ArtefactType.BAM,
                            state       : WorkflowArtefact.State.SUCCESS,
                            producedBy  : workflowRun,
                    ])
            bamFile.save(flush: true)
            filestoreService.attachWorkFolder(workflowRun, filestoreService.createWorkFolder(baseFolderInput))
        }

        RawSequenceFile.list().each {
            it.fastqImportInstance = fastqImportInstance
            it.save(flush: true)
        }

        createSampleTypeCategories()
        setupBamFilesInFileSystem()
    }

    void createSampleTypeCategories() {
        SnvDomainFactory.INSTANCE.createSampleTypePerProject(
                project: project,
                sampleType: sampleTypeTumor,
                category: SampleTypePerProject.Category.DISEASE,
        )

        SnvDomainFactory.INSTANCE.createSampleTypePerProject(
                project: project,
                sampleType: sampleTypeControl,
                category: SampleTypePerProject.Category.CONTROL,
        )

        samplePair = SnvDomainFactory.INSTANCE.createSamplePair(bamFileTumor.mergingWorkPackage, bamFileControl.mergingWorkPackage)
    }

    void setupBamFilesInFileSystem() {
        BamFileSet bamFileSet = this.bamFileSet

        Path diseaseBamFile = alignmentWorkFileServiceFactoryService.getService(bamFileTumor).getBamFile(bamFileTumor)
        Path diseaseBaiFile = alignmentWorkFileServiceFactoryService.getService(bamFileTumor).getBaiFile(bamFileTumor)
        Path controlBamFile = alignmentWorkFileServiceFactoryService.getService(bamFileControl).getBamFile(bamFileControl)
        Path controlBaiFile = alignmentWorkFileServiceFactoryService.getService(bamFileControl).getBaiFile(bamFileControl)
        [
                (bamFileSet.diseaseBamFile): diseaseBamFile,
                (bamFileSet.diseaseBaiFile): diseaseBaiFile,
                (bamFileSet.controlBamFile): controlBamFile,
                (bamFileSet.controlBaiFile): controlBaiFile,
        ].each { File target, Path link ->
            fileService.createLink(link, fileService.toPath(target, fileSystemService.remoteFileSystem))
        }

        Path diseaseBamFileLink = alignmentLinkFileServiceFactoryService.getService(bamFileTumor).getBamFile(bamFileTumor)
        Path diseaseBaiFileLink = alignmentLinkFileServiceFactoryService.getService(bamFileTumor).getBaiFile(bamFileTumor)
        Path controlBamFileLink = alignmentLinkFileServiceFactoryService.getService(bamFileControl).getBamFile(bamFileControl)
        Path controlBaiFileLink = alignmentLinkFileServiceFactoryService.getService(bamFileControl).getBaiFile(bamFileControl)
        [
                (diseaseBamFile): diseaseBamFileLink,
                (diseaseBaiFile): diseaseBaiFileLink,
                (controlBamFile): controlBamFileLink,
                (controlBaiFile): controlBaiFileLink,
        ].each { Path target, Path link ->
            fileService.createLink(link, target)
        }

        bamFileTumor.fileSize = Files.size(diseaseBamFileLink)
        bamFileTumor.save(flush: true)

        bamFileControl.fileSize = Files.size(controlBamFileLink)
        bamFileControl.save(flush: true)
    }

    /**
     * link the reference genome directory into the test structure
     */
    void linkReferenceGenomeDirectoryToReference(ReferenceGenome referenceGenome) {
        Path target = referenceDataDirectory.resolve("reference-genomes").resolve(referenceGenome.path)
        Path link = remoteFileSystem.getPath(referenceGenomeService.referenceGenomeDirectory(referenceGenome, false).absolutePath)
        fileService.createLink(link, target)
    }

    /**
     * Currently certain pipelines have reference genomes specified via a processing option.
     * This will be changed in the future. Refer to {@link WorkflowVersion.allowedReferenceGenomes}.
     * For now we just set these to the current reference genome until these workflows are transferred to the new system.
     */
    @Deprecated
    private void createProcessingOptionsForNotification() {
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME, value: "${referenceGenome.name}")
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME, value: "${referenceGenome.name}")
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME, value: "${referenceGenome.name}")
    }

    @Override
    File getBamFilePairBaseDirectory() {
        return new File(referenceDataDirectory.resolve('bamFiles').toString())
    }

    void createBedFileAndLibPrepKit() {
        LibraryPreparationKit kit = createLibraryPreparationKit(name: "Agilent5withoutUTRs")
        DomainFactory.createBedFile(
                fileName: "Agilent5withoutUTRs_plain.bed",
                libraryPreparationKit: kit,
                referenceGenome: referenceGenome,
        )
        bamFileTumor.containedSeqTracks*.libraryPreparationKit = kit
        bamFileTumor.containedSeqTracks*.save(flush: true)
        bamFileControl.containedSeqTracks*.libraryPreparationKit = kit
        bamFileControl.containedSeqTracks*.save(flush: true)
        bamFileTumor.workPackage.libraryPreparationKit = kit
        bamFileTumor.workPackage.save(flush: true)
        bamFileControl.workPackage.libraryPreparationKit = kit
        bamFileControl.workPackage.save(flush: true)
    }

    @Override
    Duration getRunningTimeout() {
        return Duration.ofHours(24)
    }
}
