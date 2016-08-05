package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.dataprocessing.AbstractBamFileServiceTests.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.TestData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AbstractQualityAssessmentServiceTests {

    AbstractQualityAssessmentService abstractQualityAssessmentService

    final static REFERENCE_GENOME_LENGTH = 80
    final static REFERENCE_GENOME_LENGTH_WITH_N = 40
    final static long SOME_VALUE_1 = 1111111
    final static long SOME_VALUE_2 = 2222222
    final static long SOME_VALUE_3 = 3333333
    final static long SOME_VALUE_4 = 4444444

    TestData data = new TestData()

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        data.createObjects()

        data.referenceGenome.with {
            setLength REFERENCE_GENOME_LENGTH_WITH_N
            setLengthWithoutN REFERENCE_GENOME_LENGTH
        }
        assert data.referenceGenome.save([flush: true])
        DomainFactory.createPanCanAlignableSeqTypes()
    }

    @Test
    void test_saveCoverageToProcessedBamFile_WhenCoverageIsNull_ShouldCalculateAndSetCoverage() {

        final QC_BASES_MAPPED = 160
        final EXPECTED_COVERAGE = 2
        final EXPECTED_COVERAGE_WITH_N = 4

        def processedBamFile = createProcessedBamFile()
        def qualityAssessmentPass = createQualityAssessmentDataForProcessedBamFile(processedBamFile, QC_BASES_MAPPED)

        abstractQualityAssessmentService.saveCoverageToProcessedBamFile(qualityAssessmentPass)

        assert processedBamFile.coverage == EXPECTED_COVERAGE
        assert processedBamFile.coverageWithN == EXPECTED_COVERAGE_WITH_N
    }

    @Test
    void test_saveCoverageToProcessedMergedBamFile_WhenCoverageIsNull_ShouldCalculateAndSetCoverage() {

        final QC_BASES_MAPPED = 320
        final EXPECTED_COVERAGE = 4
        final EXPECTED_COVERAGE_WITH_N = 8

        def processedMergedBamFile = createProcessedMergedBamFile()
        def qualityAssessmentMergedPass = createQualityAssessmentDataForProcessedMergedBamFile(processedMergedBamFile, QC_BASES_MAPPED)

        abstractQualityAssessmentService.saveCoverageToAbstractMergedBamFile(qualityAssessmentMergedPass)

        assert processedMergedBamFile.coverage == EXPECTED_COVERAGE
        assert processedMergedBamFile.coverageWithN == EXPECTED_COVERAGE_WITH_N
    }

    @Test
    void test_saveCoverageToRoddyBamFile_ShouldCalculateAndSetCoverage() {
        final QC_BASES_MAPPED = 320
        final EXPECTED_COVERAGE = 4
        final EXPECTED_COVERAGE_WITH_N = 8

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        RoddyMergedBamQa mergedQa = new RoddyMergedBamQa(
                ARBITRARY_QA_VALUES + [
                qualityAssessmentMergedPass : QualityAssessmentMergedPass.build(abstractMergedBamFile: roddyBamFile),
                qcBasesMapped : QC_BASES_MAPPED,
                genomeWithoutNCoverageQcBases: EXPECTED_COVERAGE,
                chromosome: RoddyQualityAssessment.ALL,
                insertSizeCV: 123,
                percentageMatesOnDifferentChr: 0.123,
        ])
        assert mergedQa.save(flush: true)

        roddyBamFile.referenceGenome.length = REFERENCE_GENOME_LENGTH_WITH_N
        roddyBamFile.referenceGenome.lengthWithoutN = REFERENCE_GENOME_LENGTH
        assert roddyBamFile.referenceGenome.save(flush: true)

        abstractQualityAssessmentService.saveCoverageToRoddyBamFile(roddyBamFile)

        assert roddyBamFile.coverage == EXPECTED_COVERAGE
        assert roddyBamFile.coverageWithN == EXPECTED_COVERAGE_WITH_N
    }

    RoddyBamFile setUpForParseRoddyQaStatistics(SeqType seqType) {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile(
                workPackage: DomainFactory.createMergingWorkPackage(
                        seqType: seqType,
                        pipeline: DomainFactory.createPanCanPipeline(),
                )
        )
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddyBamFile.project.realmName])
        createReferenceGenomeEntries(roddyBamFile.referenceGenome)
        DomainFactory.createQaFileOnFileSystem(roddyBamFile.workMergedQAJsonFile, SOME_VALUE_1)
        DomainFactory.createQaFileOnFileSystem(roddyBamFile.workMergedQATargetExtractJsonFile, SOME_VALUE_2)
        DomainFactory.createQaFileOnFileSystem(exactlyOneElement(roddyBamFile.workSingleLaneQAJsonFiles.values()), SOME_VALUE_3)
        DomainFactory.createQaFileOnFileSystem(exactlyOneElement(roddyBamFile.workLibraryQAJsonFiles.values()), SOME_VALUE_4)
        assert RoddyMergedBamQa.list().empty
        assert RoddySingleLaneQa.list().empty
        assert RoddyLibraryQa.list().empty
        return roddyBamFile
    }

    private void testParseRoddyMergedBamQaStatistics_allFine(String mergedBamOrSingleLane, SeqType seqType, Long qcBasesMappedExpected, Long allBasesMappedExpected, Long onTargetMappedBasesExpected) {
        RoddyBamFile roddyBamFile = setUpForParseRoddyQaStatistics(seqType)

        abstractQualityAssessmentService."parseRoddy${mergedBamOrSingleLane}QaStatistics"(roddyBamFile)

        Collection<RoddyQualityAssessment> qas = RoddyQualityAssessment.list()
        assert TestCase.containSame(qas*.class*.simpleName.unique(), ["Roddy${mergedBamOrSingleLane}Qa"])
        assert TestCase.containSame(qas*.chromosome, ["8", "all", "7"])
        assert qas.find{ it.chromosome == '8' }.qcBasesMapped == qcBasesMappedExpected
        assert qas.find{ it.chromosome == '8' }.allBasesMapped == allBasesMappedExpected
        assert qas.find{ it.chromosome == '8' }.onTargetMappedBases == onTargetMappedBasesExpected
    }

    @Test
    void testParseRoddyMergedBamQaStatistics_WholeGenome_allFine() {
        testParseRoddyMergedBamQaStatistics_allFine("MergedBam", DomainFactory.createWholeGenomeSeqType(), SOME_VALUE_1, null, null)
    }

    @Test
    void testParseRoddyMergedBamQaStatistics_Exome_allFine() {
        testParseRoddyMergedBamQaStatistics_allFine("MergedBam", DomainFactory.createExomeSeqType(), null, SOME_VALUE_1, SOME_VALUE_2)
    }

    @Test
    void testParseRoddyQaStatistics_missingChromosome() {
        RoddyBamFile roddyBamFile = setUpForParseRoddyQaStatistics(DomainFactory.createWholeGenomeSeqType())
        ReferenceGenomeEntry.build(
                referenceGenome: roddyBamFile.referenceGenome,
                classification: Classification.CHROMOSOME,
                name: '9',
                alias: '9',
        )
        TestCase.shouldFailWithMessage(RuntimeException, /^Missed chromosomes: .+ \(expected: .*; found .*\).$/, {
            abstractQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)
        })
    }

    @Test
    void testParseRoddySingleLaneQaStatistics_WholeGenome_allFine() {
        testParseRoddyMergedBamQaStatistics_allFine("SingleLane", DomainFactory.createWholeGenomeSeqType(), SOME_VALUE_3, null, null)
    }

    @Test
    void testParseRoddySingleLaneQaStatistics_Exome_allFine() {
        testParseRoddyMergedBamQaStatistics_allFine("SingleLane", DomainFactory.createExomeSeqType(), null, SOME_VALUE_3, null)
    }

    @Test
    void testParseRoddyLibraryQaStatistics_WGBS_allFine() {
        testParseRoddyMergedBamQaStatistics_allFine("Library", DomainFactory.createWholeGenomeBisulfiteSeqType(), SOME_VALUE_4, null, null)
    }

    @Test
    void testAssertListContainsAllChromosomeNamesInReferenceGenome_listAreSame_shouldBeFine() {
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3']
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        abstractQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)
    }

    @Test
    void testAssertListContainsAllChromosomeNamesInReferenceGenome_OtpListIsSubsetOfRoddyList_shouldBeFine() {
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3', '4', '5']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3']
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        abstractQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)
    }

    @Test
    void testAssertListContainsAllChromosomeNamesInReferenceGenome_OtpListIsBiggerThenRoddyList_shouldFail() {
        List<String> chromosomeNamesFromRoddyJson = [RoddyQualityAssessment.ALL, '1', '2', '3']
        List<String> chromosomeNamesFromOtp = ['1', '2', '3', '4', '5']
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeEntries(referenceGenome, chromosomeNamesFromOtp)

        TestCase.shouldFailWithMessage(RuntimeException, /^Missed chromosomes: .+ \(expected: .*; found .*\).$/, {
            abstractQualityAssessmentService.assertListContainsAllChromosomeNamesInReferenceGenome(chromosomeNamesFromRoddyJson, referenceGenome)
        })
    }


    static void createReferenceGenomeEntries(ReferenceGenome referenceGenome) {
        ['7', '8'].each {
            ReferenceGenomeEntry.build(
                    referenceGenome: referenceGenome,
                    classification: Classification.CHROMOSOME,
                    name: it,
            )
        }
    }


    private ProcessedBamFile createProcessedBamFile() {

        AlignmentPass alignmentPass = data.createAlignmentPass()
        alignmentPass.save([flush: true])

        ProcessedBamFile processedBamFile = new ProcessedBamFile([
                type                   : AbstractBamFile.BamType.SORTED,
                withdrawn              : false,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
                alignmentPass          : alignmentPass,
        ])
        assert processedBamFile.save([flush: true])

        return processedBamFile
    }

    private static QualityAssessmentPass createQualityAssessmentDataForProcessedBamFile(ProcessedBamFile processedBamFile, Long qcBasesMapped) {

        assert processedBamFile: 'processedBamFile must not be null'

        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass([
                processedBamFile: processedBamFile,
                // Explicitly set up null, so we can check the result
                coverage: null,
                coverageWithN: null,
        ])
        assert qualityAssessmentPass.save([flush: true])

        OverallQualityAssessment overallQualityAssessment = new OverallQualityAssessment(
                ARBITRARY_QA_VALUES + [
                qualityAssessmentPass: qualityAssessmentPass,
                qcBasesMapped: qcBasesMapped,
        ])
        assert overallQualityAssessment.save([flush: true])

        return qualityAssessmentPass
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile() {

        MergingWorkPackage mergingWorkPackage = data.findOrSaveMergingWorkPackage(data.seqTrack, data.referenceGenome)
        mergingWorkPackage.save([flush: true])

        MergingSet mergingSet = data.createMergingSet([mergingWorkPackage: mergingWorkPackage])
        mergingSet.save([flush: true])

        MergingPass mergingPass = data.createMergingPass([mergingSet: mergingSet])
        assert mergingPass.save([flush: true])

        ProcessedBamFile processedBamFile = createProcessedBamFile()

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment([
                mergingSet: mergingSet,
                bamFile: processedBamFile,
        ])
        assert mergingSetAssignment.save([flush: true])

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                withdrawn              : false,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
        ])
        assert processedMergedBamFile.save([flush: true])

        return processedMergedBamFile
    }

    private static QualityAssessmentMergedPass createQualityAssessmentDataForProcessedMergedBamFile(ProcessedMergedBamFile processedMergedBamFile, Long qcBasesMapped) {

        assert processedMergedBamFile: 'processedMergedBamFile must not be null'

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
                abstractMergedBamFile: processedMergedBamFile,
                // Explicitly set up null, so we can check the result
                coverage: null,
                coverageWithN: null,
        ])
        assert qualityAssessmentMergedPass.save([flush: true])

        OverallQualityAssessmentMerged overallQualityAssessmentMerged = new OverallQualityAssessmentMerged(
                ARBITRARY_QA_VALUES + [
                qualityAssessmentMergedPass: qualityAssessmentMergedPass,
                qcBasesMapped: qcBasesMapped,
        ])
        assert overallQualityAssessmentMerged.save([flush: true])

        return qualityAssessmentMergedPass
    }
}
