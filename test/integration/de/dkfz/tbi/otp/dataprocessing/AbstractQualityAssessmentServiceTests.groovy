package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.dataprocessing.AbstractBamFileServiceTests.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.TestData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AbstractQualityAssessmentServiceTests {

    AbstractQualityAssessmentService abstractQualityAssessmentService

    final static REFERENCE_GENOME_LENGTH = 80
    final static REFERENCE_GENOME_LENGTH_WITH_N = 40

    TestData data = new TestData()

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        temporaryFolder.create()
        data.createObjects()

        data.referenceGenome.with {
            setLength REFERENCE_GENOME_LENGTH_WITH_N
            setLengthWithoutN REFERENCE_GENOME_LENGTH
        }
        assert data.referenceGenome.save([flush: true])
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

        abstractQualityAssessmentService.saveCoverageToProcessedMergedBamFile(qualityAssessmentMergedPass)

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
                qualityAssessmentMergedPass : QualityAssessmentMergedPass.build(processedMergedBamFile: roddyBamFile),
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

    RoddyBamFile setUpForParseRoddyBamFileQaStatistics() {
        File qaFile = temporaryFolder.newFile(RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME)
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        createReferenceGenomeEntriesAndQaFileOnFilesystem(roddyBamFile.referenceGenome, qaFile)
        roddyBamFile.metaClass.getTmpRoddyMergedQAJsonFile = { ->
            return qaFile
        }
        assert RoddyMergedBamQa.list().empty
        return roddyBamFile
    }

    @Test
    void testParseRoddyBamFileQaStatistics_allFine() {
        RoddyBamFile roddyBamFile = setUpForParseRoddyBamFileQaStatistics()
        abstractQualityAssessmentService.parseRoddyBamFileQaStatistics(roddyBamFile)
        assert TestCase.containSame(["8", "all", "7"], RoddyMergedBamQa.list()*.chromosome)
    }

    @Test
    void testParseRoddyBamFileQaStatistics_missingChromosome() {
        RoddyBamFile roddyBamFile = setUpForParseRoddyBamFileQaStatistics()
        ReferenceGenomeEntry.build(
                referenceGenome: roddyBamFile.referenceGenome,
                classification: Classification.CHROMOSOME,
                name: '9',
                alias: '9',
        )
        TestCase.shouldFailWithMessage(RuntimeException, /^Expected chromosomes .+, but found .+\.$/, {
            abstractQualityAssessmentService.parseRoddyBamFileQaStatistics(roddyBamFile)
        })
    }

    @Test
    void testParseRoddySingleLaneQaStatistics() {
        File qaFile = temporaryFolder.newFile(RoddyBamFile.QUALITY_CONTROL_JSON_FILE_NAME)
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        createReferenceGenomeEntriesAndQaFileOnFilesystem(roddyBamFile.referenceGenome, qaFile)
        SeqTrack seqTrack = exactlyOneElement(roddyBamFile.seqTracks)
        roddyBamFile.metaClass.getTmpRoddySingleLaneQAJsonFiles = { ->
            return [(seqTrack): qaFile]
        }
        assert RoddySingleLaneQa.list().empty
        abstractQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)
        assert TestCase.containSame(["8", "all", "7"], RoddySingleLaneQa.list()*.chromosome)
    }


    static void createReferenceGenomeEntriesAndQaFileOnFilesystem(ReferenceGenome referenceGenome, File qaFile) {
        ['7', '8'].each {
            ReferenceGenomeEntry.build(
                    referenceGenome: referenceGenome,
                    classification: Classification.CHROMOSOME,
                    name: it,
            )
        }

        // the values are from the documentation on the Wiki: https://wiki.local/NGS/OTP-Roddy+Interface#HTheQCData
        qaFile <<
"""
{
  "8": {
    "genomeWithoutNCoverageQcBases": 0.01,
    "referenceLength": 146364022,
    "chromosome": 8,
    "qcBasesMapped": 1866013
  },
  "all": {
    "pairedRead1": 209146,
    "pairedInSequencing": 421309,
    "withMateMappedToDifferentChr": 33635,
    "qcFailedReads": 0,
    "totalReadCounter": 421309,
    "totalMappedReadCounter": 420369,
    "genomeWithoutNCoverageQcBases": 0.01,
    "singletons": 1080,
    "withMateMappedToDifferentChrMaq": 6161,
    "insertSizeMedian": 399,
    "insertSizeSD": 93,
    "pairedRead2": 212163,
    "percentageMatesOnDifferentChr": 1.55,
    "chromosome": "all",
    "withItselfAndMateMapped": 419289,
    "qcBasesMapped": 35857865,
    "duplicates": 805,
    "insertSizeCV": 23,
    "referenceLength": 3095677412,
    "properlyPaired": 384766
  },
  "7": {
    "referenceLength": 159138663,
    "genomeWithoutNCoverageQcBases": 0.01,
    "qcBasesMapped": 1942176,
    "chromosome": 7
  }
}
"""
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
                processedMergedBamFile: processedMergedBamFile,
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
