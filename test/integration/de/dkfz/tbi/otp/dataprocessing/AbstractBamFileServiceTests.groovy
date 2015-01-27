package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.InformationReliability;
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*

class AbstractBamFileServiceTests {

    AbstractBamFileService abstractBamFileService

    TestData testData = new TestData()
    SeqTrack seqTrack
    ExomeSeqTrack exomeSeqTrack
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

    @Before
    void setUp() {

        Project project = new Project(
                name: "project",
                dirName: "project-dir",
                realmName: "realmName"
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
                qualityCode: "software-tool-quality-code",
                type: SoftwareTool.Type.BASECALLING
                )
        assertNotNull(softwareTool.save([flush: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
                name: "seq-platform",
                model: "seq-platform-model"
                )
        assertNotNull(seqPlatform.save([flush: true]))

        Run run = new Run(
                name: "run",
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
                storageRealm: Run.StorageRealm.DKFZ
                )
        assertNotNull(run.save([flush: true]))

        Individual individual = new Individual(
                pid: "patient",
                mockPid: "mockPid",
                mockFullName: "mockFullName",
                type: Individual.Type.UNDEFINED,
                project: project
                )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                name: "sample-type"
                )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                individual: individual,
                sampleType: sampleType
                )
        assertNotNull(sample.save([flush: true]))

        SeqType wholeGenomeSeqType = new SeqType(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                libraryLayout:"library",
                dirName: "seq-type-dir-whole-genome"
                )
        assertNotNull(wholeGenomeSeqType.save([flush: true]))

        SeqType exomeSeqType = new SeqType(
                name:SeqTypeNames.EXOME.seqTypeName,
                libraryLayout:"library",
                dirName: "seq-type-dir-exome"
                )
        assertNotNull(exomeSeqType.save([flush: true]))

        ReferenceGenome referenceGenome = new ReferenceGenome([
            name                        : 'Arbitrary Reference Genome Name',
            path                        : '/nonexistent',
            fileNamePrefix              : 'somePrefix',
            length                      : ARBITRARY_GENOME_LENGTH_FOR_COVERAGE_WITH_N,
            lengthWithoutN              : ARBITRARY_GENOME_LENGTH_FOR_COVERAGE_WITHOUT_N,
            lengthRefChromosomes        : ARBITRARY_UNUSED_VALUE,
            lengthRefChromosomesWithoutN: ARBITRARY_UNUSED_VALUE,
        ])
        assert referenceGenome.save([flush: true])

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

        ExomeEnrichmentKit exomeEnrichmentKit = new ExomeEnrichmentKit(
                name: "exomeEnrichmentKit"
                )
        assertNotNull(exomeEnrichmentKit.save([flush: true]))

        BedFile bedFile = new BedFile(
                fileName: "bed_file",
                targetSize: ARBITRARY_TARGET_SIZE,
                mergedTargetSize: ARBITRARY_MERGED_TARGET_SIZE,
                referenceGenome: referenceGenome,
                exomeEnrichmentKit: exomeEnrichmentKit
                )
        assert bedFile.save([flush: true])

        seqTrack = new SeqTrack(
                laneId: "0",
                run: run,
                sample: sample,
                seqType: wholeGenomeSeqType,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool
                )
        assertNotNull(seqTrack.save([flush: true]))

        exomeSeqTrack = new ExomeSeqTrack(
                laneId: "1",
                run: run,
                sample: sample,
                seqType: exomeSeqType,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool,
                exomeEnrichmentKit: exomeEnrichmentKit,
                kitInfoReliability: InformationReliability.KNOWN
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

    @Test(expected = IllegalArgumentException)
    void testFindByMergingSetIsNull() {
        abstractBamFileService.findByMergingSet(null)
    }

    @Test
    void testFindByMergingSet() {
        List<AbstractBamFile> abstractBamFilesExp
        List<AbstractBamFile> abstractBamFilesAct

        assert abstractBamFileService.findByMergingSet(mergingSet).isEmpty()

        mergingSetAssignment = assignToMergingSet(mergingSet, processedBamFile)

        abstractBamFilesExp = []
        abstractBamFilesExp.add(processedBamFile)
        abstractBamFilesAct = abstractBamFileService.findByMergingSet(mergingSet)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedBamFile processedBamFile2 = createAndSaveProcessedBamFileAndQAObjects(seqTrack, "3")
        MergingSetAssignment mergingSetAssignment2 = assignToMergingSet(mergingSet, processedBamFile2)

        abstractBamFilesExp.add(processedBamFile2)
        abstractBamFilesAct = abstractBamFileService.findByMergingSet(mergingSet)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingSet)
        MergingSetAssignment mergingSetAssignment3 = assignToMergingSet(mergingSet, processedMergedBamFile)

        abstractBamFilesExp.add(processedMergedBamFile)
        abstractBamFilesAct = abstractBamFileService.findByMergingSet(mergingSet)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFindByProcessedMergedBamFileIsNull() {
        abstractBamFileService.findByProcessedMergedBamFile(null)
    }

    @Test
    void testFindByProcessedMergedBamFile() {
        List<AbstractBamFile> abstractBamFilesExp
        List<AbstractBamFile> abstractBamFilesAct

        assert abstractBamFileService.findByMergingSet(mergingSet).isEmpty()

        mergingSetAssignment = assignToMergingSet(mergingSet, processedBamFile)

        ProcessedMergedBamFile pmbf_WholeGenome = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)

        abstractBamFilesExp = []
        abstractBamFilesExp.add(processedBamFile)
        abstractBamFilesAct = abstractBamFileService.findByProcessedMergedBamFile(pmbf_WholeGenome)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedBamFile processedBamFile2 = createAndSaveProcessedBamFileAndQAObjects(seqTrack, "3")
        MergingSetAssignment mergingSetAssignment2 = assignToMergingSet(mergingSet, processedBamFile2)

        abstractBamFilesExp.add(processedBamFile2)
        abstractBamFilesAct = abstractBamFileService.findByProcessedMergedBamFile(pmbf_WholeGenome)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedMergedBamFile processedMergedBamFile2 = createProcessedMergedBamFile(mergingSet)
        MergingSetAssignment mergingSetAssignment3 = assignToMergingSet(mergingSet, processedMergedBamFile2)

        abstractBamFilesExp.add(processedMergedBamFile2)
        abstractBamFilesAct = abstractBamFileService.findByProcessedMergedBamFile(pmbf_WholeGenome)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)
    }


    @Test
    void testAssignedToMergingSet() {
        assertEquals(processedBamFile.status, AbstractBamFile.State.NEEDS_PROCESSING)
        abstractBamFileService.assignedToMergingSet(processedBamFile)
        assertEquals(processedBamFile.status, AbstractBamFile.State.PROCESSED)
    }

    // Test calculateCoverageWithoutN() for ProcessedBamFiles

    @Test(expected = AssertionError)
    void test_calculateCoverageWithoutN_WhenBamFileIsNull() {
        abstractBamFileService.calculateCoverageWithoutN(null)
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedBamFile_SeqTypeWholeGenome() {
        changeStateOfBamFileToHavingPassedQC(processedBamFile)
        assert abstractBamFileService.calculateCoverageWithoutN(processedBamFile) == EXPECTED_COVERAGE_FOR_COVERAGE_WITHOUT_N_WHOLE_GENOME
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedBamFile_SeqTypeExome() {
        changeStateOfBamFileToHavingPassedQC(exomeProcessedBamFile)
        assert abstractBamFileService.calculateCoverageWithoutN(exomeProcessedBamFile) == EXPECTED_COVERAGE_FOR_COVERAGE_WITHOUT_N_EXOME
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedBamFile_Exome_onTragetBasesMappedNotFilled() {
        changeStateOfBamFileToHavingPassedQC(exomeProcessedBamFile)
        assert abstractBamFileService.calculateCoverageWithoutN(exomeProcessedBamFile) == EXPECTED_COVERAGE_FOR_COVERAGE_WITHOUT_N_EXOME
        QualityAssessmentPass qualityAssessmentPass = QualityAssessmentPass.findByProcessedBamFile(exomeProcessedBamFile)
        OverallQualityAssessment overallQualityAssessment = OverallQualityAssessment.findByQualityAssessmentPass(qualityAssessmentPass)
        overallQualityAssessment.onTargetMappedBases = null
        overallQualityAssessment.save()
        assert abstractBamFileService.calculateCoverageWithoutN(exomeProcessedBamFile) == null
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedBamFile_SeqTypeWholeGenome_AndReferenceGenomeIsNull() {
        changeStateOfBamFileToHavingPassedQC(processedBamFile)
        processedBamFile.alignmentPass.referenceGenome = null
        abstractBamFileService.calculateCoverageWithoutN(processedBamFile)
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedBamFile_SeqTypeExome_AndReferenceGenomeIsNull() {
        changeStateOfBamFileToHavingPassedQC(exomeProcessedBamFile)
        exomeProcessedBamFile.alignmentPass.referenceGenome = null
        abstractBamFileService.calculateCoverageWithoutN(exomeProcessedBamFile)
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedBamFile_SeqTypeWholeGenome_AndFileIsNotQualityAssessed() {
        assert !processedBamFile.isQualityAssessed()
        abstractBamFileService.calculateCoverageWithoutN(processedBamFile)
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedBamFile_SeqTypeExome_AndFileIsNotQualityAssessed() {
        assert !exomeProcessedBamFile.isQualityAssessed()
        abstractBamFileService.calculateCoverageWithoutN(exomeProcessedBamFile)
    }

    // Test calculateCoverageWithoutN() for ProcessedMergedBamFiles
    //   -> Technically not needed, as they are AbstractBamFiles too.

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedMergedBamFile_WholeGenome() {
        assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        assert abstractBamFileService.calculateCoverageWithoutN(processedMergedBamFile) == EXPECTED_COVERAGE_FOR_COVERAGE_WITHOUT_N_WHOLE_GENOME
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedMergedBamFile_Exome() {
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        assert abstractBamFileService.calculateCoverageWithoutN(processedMergedBamFile) == EXPECTED_COVERAGE_FOR_COVERAGE_WITHOUT_N_EXOME
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedMergedBamFile_Exome_onTragetBasesMappedNotFilled() {
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        QualityAssessmentMergedPass qualityAssessmentMergedPass = QualityAssessmentMergedPass.findByProcessedMergedBamFile(processedMergedBamFile)
        OverallQualityAssessmentMerged overallQualityAssessmentMerged = OverallQualityAssessmentMerged.findByQualityAssessmentMergedPass(qualityAssessmentMergedPass)
        overallQualityAssessmentMerged.onTargetMappedBases = null
        overallQualityAssessmentMerged.save()
        assert abstractBamFileService.calculateCoverageWithoutN(processedMergedBamFile) == null
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedMergedBamFile_WholeGenome_AndReferenceGenomeIsNull() {
        assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        processedMergedBamFile.mergingWorkPackage.referenceGenome = null
        abstractBamFileService.calculateCoverageWithoutN(processedMergedBamFile)
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedMergedBamFile_Exome_AndReferenceGenomeIsNull() {
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        assert referenceGenomeProjectSeqTypeForExome.delete([flush: true])
        abstractBamFileService.calculateCoverageWithoutN(processedMergedBamFile)
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedMergedBamFile_WholeGenome_AndFileIsNotQualityAssessed() {
        assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        assert !processedMergedBamFile.isQualityAssessed()
        abstractBamFileService.calculateCoverageWithoutN(processedMergedBamFile)
    }

    @Test
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedMergedBamFile_Exome_AndFileIsNotQualityAssessed() {
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        assert !processedMergedBamFile.isQualityAssessed()
        abstractBamFileService.calculateCoverageWithoutN(processedMergedBamFile)
    }

    // Test calculateCoverageWithN() for ProcessedBamFiles

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsNull() {
        abstractBamFileService.calculateCoverageWithN(null)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_WholeGenome() {
        changeStateOfBamFileToHavingPassedQC(processedBamFile)
        assert abstractBamFileService.calculateCoverageWithN(processedBamFile) == EXPECTED_COVERAGE_FOR_COVERAGE_WITH_N_WHOLE_GENOME
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_Exome() {
        changeStateOfBamFileToHavingPassedQC(exomeProcessedBamFile)
        assert abstractBamFileService.calculateCoverageWithN(exomeProcessedBamFile) == null
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_WholeGenome_AndReferenceGenomeIsNull() {
        changeStateOfBamFileToHavingPassedQC(processedBamFile)
        processedBamFile.alignmentPass.referenceGenome = null
        abstractBamFileService.calculateCoverageWithN(processedBamFile)
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_Exome_AndReferenceGenomeIsNull() {
        changeStateOfBamFileToHavingPassedQC(exomeProcessedBamFile)
        assert referenceGenomeProjectSeqTypeForExome.delete([flush: true])
        abstractBamFileService.calculateCoverageWithN(exomeProcessedBamFile)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_WholeGenome_AndFileIsNotQualityAssessed() {
        assert !processedBamFile.isQualityAssessed()
        abstractBamFileService.calculateCoverageWithN(processedBamFile)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedBamFile_Exome_AndFileIsNotQualityAssessed() {
        assert !exomeProcessedBamFile.isQualityAssessed()
        assert abstractBamFileService.calculateCoverageWithN(exomeProcessedBamFile) == null
    }

    // Test calculateCoverageWithN() for ProcessedMergedBamFiles
    //   -> Technically not needed, as they are AbstractBamFiles too.

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_WholeGenome() {
        mergingSetAssignment = assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile pmbf_WholeGenome = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        changeStateOfBamFileToHavingPassedQC(pmbf_WholeGenome)
        assert abstractBamFileService.calculateCoverageWithN(pmbf_WholeGenome) == EXPECTED_COVERAGE_FOR_COVERAGE_WITH_N_WHOLE_GENOME
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_Exome() {
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        assert abstractBamFileService.calculateCoverageWithN(processedMergedBamFile) == null
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_WholeGenome_AndReferenceGenomeIsNull() {
        assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        processedMergedBamFile.mergingWorkPackage.referenceGenome = null
        abstractBamFileService.calculateCoverageWithN(processedMergedBamFile)
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_Exome_AndReferenceGenomeIsNull() {
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        changeStateOfBamFileToHavingPassedQC(processedMergedBamFile)
        assert referenceGenomeProjectSeqTypeForExome.delete([flush: true])
        abstractBamFileService.calculateCoverageWithN(processedMergedBamFile)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_WholeGenome_AndFileIsNotQualityAssessed() {
        assignToMergingSet(mergingSet, processedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(mergingSet)
        assert !processedMergedBamFile.isQualityAssessed()
        abstractBamFileService.calculateCoverageWithN(processedMergedBamFile)
    }

    @Test
    void test_calculateCoverageWithN_WhenBamFileIsProcessedMergedBamFile_Exome_AndFileIsNotQualityAssessed() {
        assignToMergingSet(exomeMergingSet, exomeProcessedBamFile)
        ProcessedMergedBamFile processedMergedBamFile = createAndSaveProcessedMergedBamFileAndDependentObjects(exomeMergingSet)
        assert !processedMergedBamFile.isQualityAssessed()
        assert abstractBamFileService.calculateCoverageWithN(processedMergedBamFile) == null
    }


    public ProcessedMergedBamFile createProcessedMergedBamFile(MergingSet mergingSet) {
        MergingPass mergingPass1 = new MergingPass(
                identifier: 1,
                mergingSet: mergingSet
        )
        assertNotNull(mergingPass1.save([flush: true]))

        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass1,
                        fileExists: true,
                        type: BamType.MDUP
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))
        return processedMergedBamFile
    }

    private ProcessedMergedBamFile createAndSaveProcessedMergedBamFileAndDependentObjects(MergingSet mergingSet) {
        MergingPass mergingPass = new MergingPass(
                identifier: 0,
                mergingSet: mergingSet
                )
        assertNotNull(mergingPass.save([flush: true]))

        // Do not create as QC'ed in order to test assertions if no QC data exists. Tests explicitly change it if needed.
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile([
            mergingPass  : mergingPass,
            type         : BamType.SORTED,
            status       : State.NEEDS_PROCESSING,
        ])
        assert processedMergedBamFile.save([flush: true])

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
            processedMergedBamFile: processedMergedBamFile,
        ])
        assert qualityAssessmentMergedPass.save([flush: true])

        OverallQualityAssessmentMerged overallQualityAssessmentMerged = new OverallQualityAssessmentMerged([
            qualityAssessmentMergedPass: qualityAssessmentMergedPass,
            qcBasesMapped              : ARBITRARY_NUMBER_OF_READS,
            onTargetMappedBases        : ARBITRARY_NUMBER_OF_READS_FOR_EXOME
        ])
        assert overallQualityAssessmentMerged.save([flush: true])

        return processedMergedBamFile
    }

    private ProcessedBamFile createAndSaveProcessedBamFileAndQAObjects(SeqTrack seqTrack, String identifier) {
        AlignmentPass alignmentPass = testData.createAlignmentPass(
                referenceGenome: seqTrack.configuredReferenceGenome,
                identifier: identifier,
                seqTrack: seqTrack,
                description: "test"
                )
        assertNotNull(alignmentPass.save([flush: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                alignmentPass: alignmentPass,
                type: BamType.SORTED,
                status: State.NEEDS_PROCESSING
                )
        assertNotNull(processedBamFile.save([flush: true]))

        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass([
            processedBamFile: processedBamFile,
        ])
        assert qualityAssessmentPass.save([flush: true])

        OverallQualityAssessment overallQualityAssessment = new OverallQualityAssessment([
            qualityAssessmentPass: qualityAssessmentPass,
            qcBasesMapped: ARBITRARY_NUMBER_OF_READS,
            onTargetMappedBases: ARBITRARY_NUMBER_OF_READS_FOR_EXOME
        ])
        assert overallQualityAssessment.save([flush: true])

        return processedBamFile
    }

    private MergingSet createMergingSetAndDependentObjects(SeqTrack seqTrack) {
        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage(
                referenceGenome: seqTrack.configuredReferenceGenome,
                sample: seqTrack.sample,
                seqType: seqTrack.seqType
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
        bamFile.with { setQualityAssessmentStatus AbstractBamFile.QaProcessingStatus.FINISHED
        }
        assert bamFile.save([flush: true])
    }
}
