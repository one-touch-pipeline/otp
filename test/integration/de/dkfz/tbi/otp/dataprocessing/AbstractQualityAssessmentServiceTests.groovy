package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.TestData
import org.junit.*

import static org.junit.Assert.assertNotNull

class AbstractQualityAssessmentServiceTests {

    def AbstractQualityAssessmentService

    final static REFERENCE_GENOME_LENGTH = 80
    final static REFERENCE_GENOME_LENGTH_WITH_N = 40

    TestData data = new TestData()

    @Before
    void setUp() {
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

        OverallQualityAssessment overallQualityAssessment = new OverallQualityAssessment([
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

        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile([
                type                   : AbstractBamFile.BamType.SORTED,
                withdrawn              : false,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
                mergingPass            : mergingPass,
                alignmentPass          : processedBamFile.alignmentPass,
                numberOfMergedLanes    : 1,
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

        OverallQualityAssessmentMerged overallQualityAssessmentMerged = new OverallQualityAssessmentMerged([
                qualityAssessmentMergedPass: qualityAssessmentMergedPass,
                qcBasesMapped: qcBasesMapped,
        ])
        assert overallQualityAssessmentMerged.save([flush: true])

        return qualityAssessmentMergedPass
    }
}
