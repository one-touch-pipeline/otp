/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.analysis.pair

import de.dkfz.tbi.otp.WorkflowTestCase
import de.dkfz.tbi.otp.analysis.pair.bamfiles.BamFileSet
import de.dkfz.tbi.otp.analysis.pair.bamfiles.SeqTypeAndInputBamFiles

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.time.Duration

abstract class AbstractBamFilePairAnalysisWorkflowTests extends WorkflowTestCase implements SeqTypeAndInputBamFiles, IsRoddy {

    static final Double COVERAGE = 30.0

    static final String PID = 'stds' // name have to be the same as in the reference data for OTP snv

    AbstractBamFile bamFileControl
    AbstractBamFile bamFileTumor
    ConfigPerProjectAndSeqType config
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    SamplePair samplePair
    SampleType sampleTypeControl
    SampleType sampleTypeTumor
    SeqType seqType

    @Override
    abstract ConfigPerProjectAndSeqType createConfig()

    @Override
    abstract ReferenceGenome createReferenceGenome()

    // The qa values are taken from the wgs alignment workflow with one lane
    final static Map QC_VALUES = [
            insertSizeMedian  : 406,
            insertSizeCV      : 23,
            properlyPaired    : 1919,
            pairedInSequencing: 2120,
    ]

    final Map createBamFileProperties() {
        return DomainFactory.randomBamFileProperties + [
                coverage            : COVERAGE,
                qcTrafficLightStatus: AbstractBamFile.QcTrafficLightStatus.QC_PASSED,
        ]
    }

    void setupRoddyBamFile() {
        MergingWorkPackage tumorMwp = DomainFactory.createMergingWorkPackage(
                seqType: seqTypeToUse(),
                pipeline: DomainFactory.createPanCanPipeline(),
                referenceGenome: createReferenceGenome()
        )
        MergingWorkPackage controlMwp = DomainFactory.createMergingWorkPackage(
                seqType: tumorMwp.seqType,
                pipeline: tumorMwp.pipeline,
                referenceGenome: tumorMwp.referenceGenome,
                sample: DomainFactory.createSample([
                        individual: tumorMwp.individual,
                ]),
        )

        bamFileTumor = DomainFactory.createRoddyBamFile([workPackage: tumorMwp] + createBamFileProperties())
        bamFileControl = DomainFactory.createRoddyBamFile(createBamFileProperties() + [
                workPackage: DomainFactory.createMergingWorkPackage(controlMwp),
                config     : bamFileTumor.config,
        ])

        DomainFactory.createRoddyMergedBamQa(bamFileTumor, QC_VALUES)
        DomainFactory.createRoddyMergedBamQa(bamFileControl, QC_VALUES)

        commonBamFileSetup()
        createBedFileAndLibPrepKit()
    }

    void setupExternalBamFile() {
        ExternalMergingWorkPackage tumorMwp = DomainFactory.createExternalMergingWorkPackage(
                seqType: seqTypeToUse(),
                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
                referenceGenome: createReferenceGenome()
        )
        ExternalMergingWorkPackage controlMwp = DomainFactory.createExternalMergingWorkPackage(
                seqType: tumorMwp.seqType,
                pipeline: tumorMwp.pipeline,
                referenceGenome: tumorMwp.referenceGenome,
                sample: DomainFactory.createSample([
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

        bamFileTumor = DomainFactory.createExternallyProcessedBamFile([
                workPackage      : tumorMwp,
                insertSizeFile   : 'tumor_insertsize_plot.png_qcValues.txt',
                maximumReadLength: 101,
        ] + createBamFileProperties())

        bamFileControl = DomainFactory.createExternallyProcessedBamFile(createBamFileProperties() + [
                workPackage      : controlMwp,
                insertSizeFile   : 'control_insertsize_plot.png_qcValues.txt',
                maximumReadLength: 101,
        ])
        commonBamFileSetup()
        createBedFileAndLibPrepKit()
    }

    private void commonBamFileSetup() {
        individual = bamFileTumor.individual
        project = individual.project
        sampleTypeControl = bamFileControl.sampleType
        sampleTypeTumor = bamFileTumor.sampleType
        seqType = bamFileTumor.seqType
        referenceGenome = bamFileControl.referenceGenome

        project.realm = realm
        assert project.save(flush: true)

        individual.pid = PID
        assert individual.save(flush: true)

        bamFileTumor.workPackage.bamFileInProjectFolder = bamFileTumor
        assert bamFileTumor.workPackage.save(flush: true)

        bamFileControl.workPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.workPackage.save(flush: true)

        createSampleTypeCategories()
        createThresholds()
        setupBamFilesInFileSystem()
    }

    void createSampleTypeCategories() {
        DomainFactory.createSampleTypePerProject(
                project: project,
                sampleType: sampleTypeTumor,
                category: SampleTypePerProject.Category.DISEASE,
        )

        DomainFactory.createSampleTypePerProject(
                project: project,
                sampleType: sampleTypeControl,
                category: SampleTypePerProject.Category.CONTROL,
        )

        samplePair = DomainFactory.createSamplePair(bamFileTumor.mergingWorkPackage, bamFileControl.mergingWorkPackage)
    }

    void createThresholds() {
        DomainFactory.createProcessingThresholds(
                project: project,
                seqType: seqType,
                sampleType: sampleTypeTumor,
                coverage: COVERAGE,
                numberOfLanes: null,
        )

        DomainFactory.createProcessingThresholds(
                project: project,
                seqType: seqType,
                sampleType: sampleTypeControl,
                coverage: COVERAGE,
                numberOfLanes: null,
        )
    }

    void setupBamFilesInFileSystem() {
        BamFileSet bamFileSet = this.bamFileSet

        File diseaseBamFile = bamFileTumor.pathForFurtherProcessing
        File diseaseBaiFile = new File(diseaseBamFile.parentFile, bamFileTumor.baiFileName)
        File controlBamFile = bamFileControl.pathForFurtherProcessing
        File controlBaiFile = new File(controlBamFile.parentFile, bamFileControl.baiFileName)

        linkFileUtils.createAndValidateLinks([
                (bamFileSet.diseaseBamFile): diseaseBamFile,
                (bamFileSet.diseaseBaiFile): diseaseBaiFile,
                (bamFileSet.controlBamFile): controlBamFile,
                (bamFileSet.controlBaiFile): controlBaiFile,
        ], realm)

        bamFileTumor.fileSize = bamFileSet.diseaseBamFile.size()
        assert bamFileTumor.save(flush: true)

        bamFileControl.fileSize = bamFileSet.controlBamFile.size()
        assert bamFileControl.save(flush: true)
    }

    abstract File getWorkflowData()

    @Override
    File getBamFilePairBaseDirectory() {
        return new File(inputRootDirectory, 'bamFiles')
    }

    void createBedFileAndLibPrepKit() {
        LibraryPreparationKit kit = DomainFactory.createLibraryPreparationKit(name: "Agilent5withoutUTRs")
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
    Duration getTimeout() {
        return Duration.ofHours(5)
    }
}
