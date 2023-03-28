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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.After
import org.junit.Test

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import static org.junit.Assert.assertNotNull

@Rollback
@Integration
class AbstractBamFileServiceIntegrationTests {

    AbstractBamFileService abstractBamFileService

    TestData testData = new TestData()
    SeqTrack seqTrack
    SeqTrack exomeSeqTrack
    MergingSet mergingSet
    MergingSet exomeMergingSet
    ProcessedBamFile processedBamFile
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType
    ProcessedBamFile exomeProcessedBamFile
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqTypeForExome
    MergingSetAssignment mergingSetAssignment

    static final Long ARBITRARY_NUMBER_OF_READS = 42
    static final Long ARBITRARY_NUMBER_OF_READS_FOR_EXOME = 40
    static final Long ARBITRARY_GENOME_LENGTH_FOR_COVERAGE_WITHOUT_N = 4
    static final Double EXPECTED_COVERAGE_FOR_COVERAGE_WITHOUT_N_WHOLE_GENOME = 10.5
    static final Double EXPECTED_COVERAGE_FOR_COVERAGE_WITHOUT_N_EXOME = 5
    static final Long ARBITRARY_GENOME_LENGTH_FOR_COVERAGE_WITH_N = 5
    static final Double EXPECTED_COVERAGE_FOR_COVERAGE_WITH_N_WHOLE_GENOME = 8.4
    static final Long ARBITRARY_UNUSED_VALUE = 1
    static final Long ARBITRARY_TARGET_SIZE = 25
    static final Long ARBITRARY_MERGED_TARGET_SIZE = 8

    static final Map ARBITRARY_QA_VALUES = [
            qcBasesMapped: ARBITRARY_UNUSED_VALUE,
            totalReadCounter: ARBITRARY_UNUSED_VALUE,
            qcFailedReads: ARBITRARY_UNUSED_VALUE,
            duplicates: ARBITRARY_UNUSED_VALUE,
            totalMappedReadCounter: ARBITRARY_UNUSED_VALUE,
            pairedInSequencing: ARBITRARY_UNUSED_VALUE,
            pairedRead2: ARBITRARY_UNUSED_VALUE,
            pairedRead1: ARBITRARY_UNUSED_VALUE,
            properlyPaired: ARBITRARY_UNUSED_VALUE,
            withItselfAndMateMapped: ARBITRARY_UNUSED_VALUE,
            withMateMappedToDifferentChr: ARBITRARY_UNUSED_VALUE,
            withMateMappedToDifferentChrMaq: ARBITRARY_UNUSED_VALUE,
            singletons: ARBITRARY_UNUSED_VALUE,
            insertSizeMedian: ARBITRARY_UNUSED_VALUE,
            insertSizeSD: ARBITRARY_UNUSED_VALUE,
            referenceLength: ARBITRARY_UNUSED_VALUE,
    ].asImmutable()

    void setupData() {
        Project project = DomainFactory.createProject(
                name: "project",
                dirName: "project-dir",
                )
        assertNotNull(project.save([flush: true]))

        SeqCenter seqCenter = new SeqCenter(
                name: "seq-center",
                dirName: "seq-center-dir"
                )
        assertNotNull(seqCenter.save([flush: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                programName: "software-tool",
                programVersion: "software-tool-version",
                type: SoftwareTool.Type.BASECALLING
                )
        assertNotNull(softwareTool.save([flush: true]))

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()

        Run run = new Run(
                name: "run",
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
                )
        assertNotNull(run.save([flush: true]))

        Individual individual = new Individual(
                pid: "patient",
                type: Individual.Type.UNDEFINED,
                project: project
                )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                name: "sample-type",
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
                )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                individual: individual,
                sampleType: sampleType
                )
        assertNotNull(sample.save([flush: true]))

        DomainFactory.createAllAlignableSeqTypes()
        SeqType wholeGenomeSeqType = DomainFactory.createWholeGenomeSeqType()
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome([
            name                        : 'Arbitrary Reference Genome Name',
            path                        : 'nonexistent',
            fileNamePrefix              : 'somePrefix',
            length                      : ARBITRARY_GENOME_LENGTH_FOR_COVERAGE_WITH_N,
            lengthWithoutN              : ARBITRARY_GENOME_LENGTH_FOR_COVERAGE_WITHOUT_N,
            lengthRefChromosomes        : ARBITRARY_UNUSED_VALUE,
            lengthRefChromosomesWithoutN: ARBITRARY_UNUSED_VALUE,
        ])

        referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType([
            project        : project,
            seqType        : wholeGenomeSeqType,
            referenceGenome: referenceGenome,
        ])
        assert referenceGenomeProjectSeqType.save([flush: true])

        referenceGenomeProjectSeqTypeForExome = new ReferenceGenomeProjectSeqType([
            project        : project,
            seqType        : exomeSeqType,
            referenceGenome: referenceGenome,
        ])
        assert referenceGenomeProjectSeqTypeForExome.save([flush: true])

        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                name: "libraryPreparationKit",
                )
        assertNotNull(libraryPreparationKit.save([flush: true]))

        BedFile bedFile = new BedFile(
                fileName: "bed_file",
                targetSize: ARBITRARY_TARGET_SIZE,
                mergedTargetSize: ARBITRARY_MERGED_TARGET_SIZE,
                referenceGenome: referenceGenome,
                libraryPreparationKit: libraryPreparationKit
                )
        assert bedFile.save([flush: true])

        seqTrack = new SeqTrack(
                laneId: "0",
                run: run,
                sample: sample,
                seqType: wholeGenomeSeqType,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool,
                sampleIdentifier: "sampleIdentifier",
                )
        assertNotNull(seqTrack.save([flush: true]))

        exomeSeqTrack = new SeqTrack(
                laneId: "1",
                run: run,
                sample: sample,
                seqType: exomeSeqType,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool,
                libraryPreparationKit: libraryPreparationKit,
                kitInfoReliability: InformationReliability.KNOWN,
                sampleIdentifier: "sampleIdentifier",
                )
        assertNotNull(exomeSeqTrack.save([flush: true]))

        processedBamFile = createAndSaveProcessedBamFileAndQAObjects(seqTrack, "1")

        exomeProcessedBamFile = createAndSaveProcessedBamFileAndQAObjects(exomeSeqTrack, "2")

        mergingSet = createMergingSetAndDependentObjects(seqTrack)

        exomeMergingSet = createMergingSetAndDependentObjects(exomeSeqTrack)
    }

    @After
    void tearDown() {
        exomeSeqTrack = null
        seqTrack = null
        mergingSet = null
        exomeMergingSet = null
        referenceGenomeProjectSeqType = null
        exomeProcessedBamFile = null
        referenceGenomeProjectSeqTypeForExome = null
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsNull() {
        setupData()
        abstractBamFileService.calculateCoverageWithN(null)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_WholeGenome() {
        setupData()
        changeStateOfBamFileToHavingPassedQC(processedBamFile)
        assert abstractBamFileService.calculateCoverageWithN(processedBamFile) == EXPECTED_COVERAGE_FOR_COVERAGE_WITH_N_WHOLE_GENOME
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_Exome() {
        setupData()
        changeStateOfBamFileToHavingPassedQC(exomeProcessedBamFile)
        assert abstractBamFileService.calculateCoverageWithN(exomeProcessedBamFile) == null
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_WholeGenome_AndReferenceGenomeIsNull() {
        setupData()
        changeStateOfBamFileToHavingPassedQC(processedBamFile)
        processedBamFile.mergingWorkPackage.referenceGenome = null
        abstractBamFileService.calculateCoverageWithN(processedBamFile)
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_Exome_AndReferenceGenomeIsNull() {
        setupData()
        changeStateOfBamFileToHavingPassedQC(exomeProcessedBamFile)
        assert referenceGenomeProjectSeqTypeForExome.delete([flush: true])
        abstractBamFileService.calculateCoverageWithN(exomeProcessedBamFile)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_WholeGenome_AndFileIsNotQualityAssessed() {
        setupData()
        assert !processedBamFile.isQualityAssessed()
        abstractBamFileService.calculateCoverageWithN(processedBamFile)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_Exome_AndFileIsNotQualityAssessed() {
        setupData()
        assert !exomeProcessedBamFile.isQualityAssessed()
        assert abstractBamFileService.calculateCoverageWithN(exomeProcessedBamFile) == null
    }

    // Test calculateCoverageWithN() for ProcessedMergedBamFiles
    //   -> Technically not needed, as they are AbstractBamFiles too.

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_WholeGenome() {
        setupData()
        mergingSetAssignment = assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile pmbf_WholeGenome = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        changeStateOfBamFileToHavingPassedQC(pmbf_WholeGenome)
        assert abstractBamFileService.calculateCoverageWithN(pmbf_WholeGenome) == EXPECTED_COVERAGE_FOR_COVERAGE_WITH_N_WHOLE_GENOME
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_Exome() {
        setupData()
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        assert abstractBamFileService.calculateCoverageWithN(processedMergedBamFile) == null
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_WholeGenome_AndReferenceGenomeIsNull() {
        setupData()
        assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        processedMergedBamFile.mergingWorkPackage.referenceGenome = null
        abstractBamFileService.calculateCoverageWithN(processedMergedBamFile)
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_Exome_AndReferenceGenomeIsNull() {
        setupData()
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        assert referenceGenomeProjectSeqTypeForExome.delete([flush: true])
        abstractBamFileService.calculateCoverageWithN(processedMergedBamFile)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_WholeGenome_AndFileIsNotQualityAssessed() {
        setupData()
        assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        assert !processedMergedBamFile.isQualityAssessed()
        abstractBamFileService.calculateCoverageWithN(processedMergedBamFile)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_Exome_AndFileIsNotQualityAssessed() {
        setupData()
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        assert !processedMergedBamFile.isQualityAssessed()
        assert abstractBamFileService.calculateCoverageWithN(processedMergedBamFile) == null
    }

    ProcessedMergedBamFile createProcessedMergedBamFile(MergingSet mergingSet) {
        MergingPass mergingPass1 = new MergingPass(
                identifier: 1,
                mergingSet: mergingSet
        )
        assertNotNull(mergingPass1.save([flush: true]))

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass1, [
                    fileExists: true,
                    numberOfMergedLanes: 1,
                    type: BamType.MDUP,
                ])
        return processedMergedBamFile
    }

    private ProcessedMergedBamFile createAndSaveProcessedMergedBamFileAndDependentObjects(MergingSet mergingSet) {
        MergingPass mergingPass = new MergingPass(
                identifier: 0,
                mergingSet: mergingSet
                )
        assertNotNull(mergingPass.save([flush: true]))

        // Do not create as QC'ed in order to test assertions if no QC data exists. Tests explicitly change it if needed.
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, [
            status       : State.NEEDS_PROCESSING,
        ])

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
            abstractMergedBamFile: processedMergedBamFile,
        ])
        assert qualityAssessmentMergedPass.save([flush: true])

        OverallQualityAssessmentMerged overallQualityAssessmentMerged = new OverallQualityAssessmentMerged(
            ARBITRARY_QA_VALUES + [
            qualityAssessmentMergedPass: qualityAssessmentMergedPass,
            qcBasesMapped              : ARBITRARY_NUMBER_OF_READS,
            onTargetMappedBases        : ARBITRARY_NUMBER_OF_READS_FOR_EXOME,
        ])
        assert overallQualityAssessmentMerged.save([flush: true])

        return processedMergedBamFile
    }

    private ProcessedBamFile createAndSaveProcessedBamFileAndQAObjects(SeqTrack seqTrack, String identifier) {
        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass(
                referenceGenome: seqTrack.configuredReferenceGenome,
                identifier: identifier,
                seqTrack: seqTrack,
                description: "test"
                )
        assertNotNull(alignmentPass.save([flush: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                alignmentPass: alignmentPass,
                type: BamType.SORTED,
                status: State.NEEDS_PROCESSING,
                numberOfMergedLanes: 1,
                )
        assertNotNull(processedBamFile.save([flush: true]))

        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass([
            processedBamFile: processedBamFile,
        ])
        assert qualityAssessmentPass.save([flush: true])

        OverallQualityAssessment overallQualityAssessment = new OverallQualityAssessment(
            ARBITRARY_QA_VALUES + [
            qualityAssessmentPass: qualityAssessmentPass,
            qcBasesMapped: ARBITRARY_NUMBER_OF_READS,
            onTargetMappedBases: ARBITRARY_NUMBER_OF_READS_FOR_EXOME,
        ])
        assert overallQualityAssessment.save([flush: true])

        return processedBamFile
    }

    private MergingSet createMergingSetAndDependentObjects(SeqTrack seqTrack) {
        MergingWorkPackage mergingWorkPackage = DomainFactory.findOrSaveMergingWorkPackage(
                seqTrack,
                seqTrack.configuredReferenceGenome,
                )
        assertNotNull(mergingWorkPackage.save([flush: true]))

        MergingSet mergingSet1 = new MergingSet(
                identifier: 0,
                mergingWorkPackage: mergingWorkPackage,
                status: MergingSet.State.NEEDS_PROCESSING
                )
        assertNotNull(mergingSet1.save([flush: true]))
        return mergingSet1
    }

    private MergingSetAssignment assignToMergingSet(MergingSet ms, AbstractBamFile bamFile) {
        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                mergingSet: ms,
                bamFile: bamFile,
                )
        assert mergingSetAssignment.save([flush: true])

        return mergingSetAssignment
    }

    private static void changeStateOfBamFileToHavingPassedQC(AbstractBamFile bamFile) {
        bamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
        assert bamFile.save([flush: true])
    }
}
