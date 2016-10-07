package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.InformationReliability;
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome

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
    ].asImmutable()

    @Before
    void setUp() {

        Project project = DomainFactory.createProject(
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
                type: SoftwareTool.Type.BASECALLING
                )
        assertNotNull(softwareTool.save([flush: true]))

        SeqPlatform seqPlatform = SeqPlatform.build()

        Run run = new Run(
                name: "run",
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
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
                name: "sample-type",
                specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
                )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                individual: individual,
                sampleType: sampleType
                )
        assertNotNull(sample.save([flush: true]))

        DomainFactory.createPanCanAlignableSeqTypes()
        SeqType wholeGenomeSeqType = DomainFactory.createWholeGenomeSeqType()
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()

        ReferenceGenome referenceGenome = new ReferenceGenome([
            name                        : 'Arbitrary Reference Genome Name',
            path                        : 'nonexistent',
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

        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                name: "libraryPreparationKit",
                shortDisplayName: "libraryPreparationKit",
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
                libraryPreparationKit: libraryPreparationKit,
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
    void testFindByProcessedMergedBamFileIsNull() {
        abstractBamFileService.findByProcessedMergedBamFile(null)
    }

    @Test
    void testFindByProcessedMergedBamFile() {
        List<AbstractBamFile> abstractBamFilesExp
        List<AbstractBamFile> abstractBamFilesAct

        assert mergingSet.bamFiles.isEmpty()

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
        processedBamFile.mergingWorkPackage.referenceGenome = null
        abstractBamFileService.calculateCoverageWithoutN(processedBamFile)
    }

    @Test(expected = AssertionError)
    void test_calculateCoverageWithoutN_WhenBamFileIsProcessedBamFile_SeqTypeExome_AndReferenceGenomeIsNull() {
        changeStateOfBamFileToHavingPassedQC(exomeProcessedBamFile)
        exomeProcessedBamFile.mergingWorkPackage.referenceGenome = null
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
        QualityAssessmentMergedPass qualityAssessmentMergedPass = QualityAssessmentMergedPass.findByAbstractMergedBamFile(processedMergedBamFile)
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
        processedBamFile.mergingWorkPackage.referenceGenome = null
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

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass1, [
                    fileExists: true,
                    numberOfMergedLanes: 1,
                    type: BamType.MDUP,
                ])
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
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
            status       : State.NEEDS_PROCESSING,
        ])
        assert processedMergedBamFile.save([flush: true])

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
        bamFile.with { setQualityAssessmentStatus AbstractBamFile.QaProcessingStatus.FINISHED
        }
        assert bamFile.save([flush: true])
    }
}
