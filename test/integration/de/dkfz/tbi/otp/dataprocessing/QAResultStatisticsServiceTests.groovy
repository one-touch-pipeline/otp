package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.filehandling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import org.junit.*
import org.springframework.beans.factory.annotation.*

import static org.junit.Assert.*

/**
 * Integration tests for the {@link QAResultStatisticsService}.
 */
class QAResultStatisticsServiceTests {

    @Autowired
    QAResultStatisticsService qaResultStatisticsService

    TestData testData = new TestData()
    Project project
    Sample sample
    Run run
    Individual individual
    SeqTrack seqTrack
    ExomeSeqTrack exomeSeqTrack
    LibraryPreparationKit libraryPreparationKit
    SeqType seqType
    AlignmentPass alignmentPass
    ProcessedBamFile processedBamFile
    ProcessedMergedBamFile processedMergedBamFile
    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntryChrX
    ReferenceGenomeEntry referenceGenomeEntryChrY
    ChromosomeQualityAssessment chromosomeQualityAssessmentChrX
    ChromosomeQualityAssessment chromosomeQualityAssessmentChrY
    OverallQualityAssessment overallQualityAssessment
    ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedChrX
    ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedChrY
    OverallQualityAssessmentMerged overallQualityAssessmentMerged

    @Before
    void setUp() {
        project = DomainFactory.createProject(
                        name: "projectName",
                        dirName: "projectDirName",
                        )
        assertNotNull(project.save([flush: true]))

        individual = new Individual(
                        pid: "pid_1",
                        mockPid: "mockPid_1",
                        mockFullName: "mockFullName_1",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "control"
                        )
        assertNotNull(sampleType.save([flush: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        seqType = DomainFactory.createWholeGenomeSeqType()

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup()

        SeqCenter seqCenter = new SeqCenter(
                        name: "seqCenterName",
                        dirName: "seqCenterDirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        run = new Run(
                        name: "runName",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        )
        assertNotNull(run.save([flush: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "softwareToolName",
                        programVersion: "version",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        libraryPreparationKit = new LibraryPreparationKit(
                name: "libraryPreparationKit",
                shortDisplayName: "libraryPreparationKit",
        )
        assertNotNull(libraryPreparationKit.save([flush: true]))

        seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        libraryPreparationKit: libraryPreparationKit,
                        kitInfoReliability: InformationReliability.KNOWN,
                        )
        assertNotNull(seqTrack.save([flush: true]))

        exomeSeqTrack = new ExomeSeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        libraryPreparationKit: libraryPreparationKit,
                        kitInfoReliability: InformationReliability.KNOWN,
                        )
        assertNotNull(exomeSeqTrack.save([flush: true]))

        referenceGenome = DomainFactory.createReferenceGenome([
                name: "referenceGenome",
                path: "pathToReferenceGenome",
                fileNamePrefix: "referenceGenomePrefix",
                length: 3210000,
                lengthWithoutN: 2910000,
                lengthRefChromosomes: 800,
                lengthRefChromosomesWithoutN: 750,
        ])


        alignmentPass = DomainFactory.createAlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack,
                        referenceGenome: referenceGenome,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true]))

        processedBamFile = new ProcessedBamFile(
                        identifier: 10,
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: AbstractBamFile.State.NEEDS_PROCESSING,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED
                        )
        assertNotNull(processedBamFile.save([flush: true]))

        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass(
                        identifier: 1,
                        description: "text",
                        processedBamFile: processedBamFile
                        )
        assertNotNull(qualityAssessmentPass.save([flush: true]))

        chromosomeQualityAssessmentChrX = new ChromosomeQualityAssessment(
                        chromosomeName: "chrX",
                        qualityAssessmentPass: qualityAssessmentPass
                        )
        setProperties(chromosomeQualityAssessmentChrX)
        assertNotNull(chromosomeQualityAssessmentChrX.save([flush: true]))

        chromosomeQualityAssessmentChrY = new ChromosomeQualityAssessment(
                        chromosomeName: "chrY",
                        qualityAssessmentPass: qualityAssessmentPass
                        )
        setProperties(chromosomeQualityAssessmentChrY)
        assertNotNull(chromosomeQualityAssessmentChrY.save([flush: true]))

        overallQualityAssessment = new OverallQualityAssessment(
                        qualityAssessmentPass: qualityAssessmentPass
                        )
        setProperties(overallQualityAssessment)
        assertNotNull(overallQualityAssessment.save([flush: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: processedBamFile.mergingWorkPackage,
                        status: MergingSet.State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))

        processedMergedBamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, [
                        fileExists: true,
                        type: BamType.MDUP,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED,
                        ])

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass(
                        identifier: 1,
                        description: "text2",
                        abstractMergedBamFile: processedMergedBamFile
                        )
        assertNotNull(qualityAssessmentMergedPass.save([flush: true]))

        BedFile bedFile = new BedFile(
                        fileName: "bedFile",
                        targetSize: 80,
                        mergedTargetSize: 60,
                        referenceGenome: referenceGenome,
                        libraryPreparationKit: libraryPreparationKit
                        )
        assertNotNull(bedFile.save([flush: true]))

        referenceGenomeEntryChrX = new ReferenceGenomeEntry(
                        name: "chrX",
                        alias: "X",
                        length: 80,
                        lengthWithoutN: 50,
                        classification: Classification.CHROMOSOME,
                        referenceGenome: referenceGenome
                        )
        assertNotNull(referenceGenomeEntryChrX.save([flush: true]))

        referenceGenomeEntryChrY = new ReferenceGenomeEntry(
                        name: "chrY",
                        alias: "Y",
                        length: 80,
                        lengthWithoutN: 50,
                        classification: Classification.CHROMOSOME,
                        referenceGenome: referenceGenome
                        )
        assertNotNull(referenceGenomeEntryChrY.save([flush: true]))

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType(
                        project: project,
                        seqType: seqType,
                        referenceGenome: referenceGenome
                        )
        assertNotNull(referenceGenomeProjectSeqType.save([flush: true]))

        chromosomeQualityAssessmentMergedChrX = new ChromosomeQualityAssessmentMerged(
                        chromosomeName: "chrX",
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass
                        )
        setProperties(chromosomeQualityAssessmentMergedChrX)
        assertNotNull(chromosomeQualityAssessmentMergedChrX.save([flush: true]))

        chromosomeQualityAssessmentMergedChrY = new ChromosomeQualityAssessmentMerged(
                        chromosomeName: "chrY",
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass
                        )
        setProperties(chromosomeQualityAssessmentMergedChrY)
        assertNotNull(chromosomeQualityAssessmentMergedChrY.save([flush: true]))

        overallQualityAssessmentMerged = new OverallQualityAssessmentMerged(
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass
                        )
        setProperties(overallQualityAssessmentMerged)
        assertNotNull(overallQualityAssessmentMerged.save([flush: true]))
    }

    @After
    void tearDown() {
        project = null
        sample = null
        run = null
        individual = null
        seqTrack = null
        seqType = null
        exomeSeqTrack = null
        libraryPreparationKit = null
        alignmentPass = null
        processedBamFile = null
        processedMergedBamFile = null
        referenceGenome = null
        referenceGenomeEntryChrX = null
        referenceGenomeEntryChrY = null
        chromosomeQualityAssessmentChrX = null
        chromosomeQualityAssessmentChrY = null
        overallQualityAssessment = null
        chromosomeQualityAssessmentMergedChrX = null
        chromosomeQualityAssessmentMergedChrY = null
        overallQualityAssessmentMerged = null
    }

    @Test(expected = IllegalArgumentException)
    void testFormatToTwoDecimalsWhenArgumentIsNull() {
        qaResultStatisticsService.formatToTwoDecimals(null)
    }

    @Test
    void testFormatToTwoDecimals() {
        assertEquals "-1.00", qaResultStatisticsService.formatToTwoDecimals(-1)
        assertEquals "0.00", qaResultStatisticsService.formatToTwoDecimals(0)
        assertEquals "2.00", qaResultStatisticsService.formatToTwoDecimals(2)
        assertEquals "3.14", qaResultStatisticsService.formatToTwoDecimals(3.14159)
        assertEquals "123.46", qaResultStatisticsService.formatToTwoDecimals(123456789 / 1e6)
    }


    @Test(expected = IllegalArgumentException)
    void testPrepareFetchingMergedBamFileResultsWhenArgumentIsNull() {
        qaResultStatisticsService.prepareFetchingMergedBamFileResults(null)
    }

    @Test
    void testPrepareFetchingMergedBamFileResultsWholeGenome() {
        Map actual = qaResultStatisticsService.prepareFetchingMergedBamFileResults(processedMergedBamFile)
        Map expect = [
            (qaResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_X): chromosomeQualityAssessmentMergedChrX,
            (qaResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y): chromosomeQualityAssessmentMergedChrY,
            (qaResultStatisticsService.OVERALL_QUALITY_ASSESSMENT)         : overallQualityAssessmentMerged,
            (qaResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_X)       : referenceGenomeEntryChrX,
            (qaResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_Y)       : referenceGenomeEntryChrY,
            (qaResultStatisticsService.REFERENCE_GENOME)                   : referenceGenome,
            (qaResultStatisticsService.INDIVIDUAL)                         : individual,
            (qaResultStatisticsService.SAMPLE)                             : sample,
            (qaResultStatisticsService.LANE)                               : 'all_merged',
            (qaResultStatisticsService.RUN)                                : 'all_merged',
            (qaResultStatisticsService.SEQTYPE)                            : seqType,
            (qaResultStatisticsService.LIBRARY_PREPARATION_KIT)            : null,
        ]
        // assertEquals will not DTRT here, we have to use the equals() method
        assertTrue expect == actual
    }

    @Test
    void testPrepareFetchingMergedBamFileResultsExome() {
        alignmentPass.seqTrack = exomeSeqTrack
        alignmentPass.save([flush: true])
        seqType.name = SeqTypeNames.EXOME.seqTypeName
        seqType.save([flush: true])
        Map actual = qaResultStatisticsService.prepareFetchingMergedBamFileResults(processedMergedBamFile)
        Map expect = [
            (qaResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_X): chromosomeQualityAssessmentMergedChrX,
            (qaResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y): chromosomeQualityAssessmentMergedChrY,
            (qaResultStatisticsService.OVERALL_QUALITY_ASSESSMENT)         : overallQualityAssessmentMerged,
            (qaResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_X)       : referenceGenomeEntryChrX,
            (qaResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_Y)       : referenceGenomeEntryChrY,
            (qaResultStatisticsService.REFERENCE_GENOME)                   : referenceGenome,
            (qaResultStatisticsService.INDIVIDUAL)                         : individual,
            (qaResultStatisticsService.SAMPLE)                             : sample,
            (qaResultStatisticsService.LANE)                               : 'all_merged',
            (qaResultStatisticsService.RUN)                                : 'all_merged',
            (qaResultStatisticsService.SEQTYPE)                            : seqType,
            (qaResultStatisticsService.LIBRARY_PREPARATION_KIT)            : libraryPreparationKit,
        ]
        // assertEquals will not DTRT here, we have to use the equals() method
        assertTrue expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testFetchResultsSmallWhenArgumentIsNull() {
        qaResultStatisticsService.fetchResultsSmall(null)
    }

    @Test
    void testFetchResultsSmallWholeGenome() {
        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.save([flush: true])
        List<Map> actual = qaResultStatisticsService.fetchResultsSmall(processedMergedBamFile)
        List<Map> expect = [
            // The two maps are (almost) identical. If different values are used, they need
            // to be changed accordingly.
            [
                // Map for ProcessedMergedBamFile
                (qaResultStatisticsService.REFERENCE_GENOME_LENGTH_WITH_N)   : '3.21',
                (qaResultStatisticsService.REFERENCE_GENOME_LENGTH_WITHOUT_N): '2.91',
                (qaResultStatisticsService.PID)                              : 'pid_1',
                (qaResultStatisticsService.MOCK_FULL_NAME)                   : 'mockFullName_1',
                (qaResultStatisticsService.SAMPLE_TYPE)                      : 'control',
                (qaResultStatisticsService.RUN_ID)                           : 'all_merged',
                (qaResultStatisticsService.LANE)                             : 'all_merged',
                (qaResultStatisticsService.COVERAGE_WITHOUT_N)               : '0.00',
                (qaResultStatisticsService.COVERAGE_WITH_N)                  : '0.00',
                (qaResultStatisticsService.COVERAGE_WITHOUT_N_CHR_X)         : '0.16',
                (qaResultStatisticsService.COVERAGE_WITHOUT_N_CHR_Y)         : '0.16',
                (qaResultStatisticsService.QC_BASES_MAPPED)                  : 8,
                (qaResultStatisticsService.TOTAL_READ_COUNT)                 : 55,
                (qaResultStatisticsService.MAPPED_READ_COUNT)                : 19,
                (qaResultStatisticsService.PERCENTAGE_MAPPED_READS)          : '34.55',
                (qaResultStatisticsService.PROPERLY_PAIRED)                  : '52.27',
                (qaResultStatisticsService.SINGLETONS)                       : '49.09',
                (qaResultStatisticsService.DUPLICATES)                       : '32.73',
                (qaResultStatisticsService.INSERT_SIZE_SD)                   : '29.00',
                (qaResultStatisticsService.INSERT_SIZE_MEDIAN)               : '30.00',
                (qaResultStatisticsService.INSERT_SIZE_MEAN)                 : '28.00',
            ],
        ]
        assertTrue expect == actual
    }

    @Test
    void testFetchResultsSmallExome() {
        seqType.name = SeqTypeNames.EXOME.seqTypeName
        seqType.save([flush: true])
        alignmentPass.seqTrack = exomeSeqTrack
        alignmentPass.save([flush: true])
        List<Map> actual = qaResultStatisticsService.fetchResultsSmall(processedMergedBamFile)
        List<Map> expect = [
            // The two maps are (almost) identical. If different values are used, they need
            // to be changed accordingly.
            [
                // Map for ProcessedMergedBamFile
                (qaResultStatisticsService.REFERENCE_GENOME_LENGTH_WITH_N)   : '3.21',
                (qaResultStatisticsService.REFERENCE_GENOME_LENGTH_WITHOUT_N): '2.91',
                (qaResultStatisticsService.PID)                              : 'pid_1',
                (qaResultStatisticsService.MOCK_FULL_NAME)                   : 'mockFullName_1',
                (qaResultStatisticsService.SAMPLE_TYPE)                      : 'control',
                (qaResultStatisticsService.RUN_ID)                           : 'all_merged',
                (qaResultStatisticsService.LANE)                             : 'all_merged',
                (qaResultStatisticsService.COVERAGE_WITHOUT_N)               : '0.00',
                (qaResultStatisticsService.COVERAGE_WITH_N)                  : '0.00',
                (qaResultStatisticsService.COVERAGE_WITHOUT_N_CHR_X)         : '0.16',
                (qaResultStatisticsService.COVERAGE_WITHOUT_N_CHR_Y)         : '0.16',
                (qaResultStatisticsService.QC_BASES_MAPPED)                  : 8,
                (qaResultStatisticsService.TOTAL_READ_COUNT)                 : 55,
                (qaResultStatisticsService.MAPPED_READ_COUNT)                : 19,
                (qaResultStatisticsService.PERCENTAGE_MAPPED_READS)          : '34.55',
                (qaResultStatisticsService.PROPERLY_PAIRED)                  : '52.27',
                (qaResultStatisticsService.SINGLETONS)                       : '49.09',
                (qaResultStatisticsService.DUPLICATES)                       : '32.73',
                (qaResultStatisticsService.INSERT_SIZE_SD)                   : '29.00',
                (qaResultStatisticsService.INSERT_SIZE_MEDIAN)               : '30.00',
                (qaResultStatisticsService.INSERT_SIZE_MEAN)                 : '28.00',
                (qaResultStatisticsService.TARGET_COVERAGE)                  : '0.83',
                (qaResultStatisticsService.ON_TARGET_RATE)                   : '75.76',
            ],
        ]
        assertTrue expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testFetchResultsExtendedWhenArgumentIsNull() {
        qaResultStatisticsService.fetchResultsExtended(null)
    }

    @Test
    void testFetchResultsExtendedWholeGenome() {
        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.save([flush: true])
        final List<Map> EXPECT = [
            // The two maps are (almost) identical. If different values are used, they need
            // to be changed accordingly. They also have a large overlap with the maps in
            // testFetchResultsSmall().
            [
                // Map for ProcessedMergedBamFile
                (qaResultStatisticsService.DUPLICATES_MATE_1)                   : 1,
                (qaResultStatisticsService.DUPLICATES_MATE_2)                   : 2,
                (qaResultStatisticsService.PE_READS_MAPPED_ON_DIFF_CHR)         : 33,
                (qaResultStatisticsService.INCORRECT_PE_ORIENTATION)            : 32,
                (qaResultStatisticsService.INCORRECT_PROPER_PAIR)               : 3,
                (qaResultStatisticsService.PERCENTAGE_QC_BASES_MAPPED_WITHOUT_N): '8/2910000',
                (qaResultStatisticsService.PERCENTAGE_QC_BASES_MAPPED_WITH_N)   : '8/3210000',
                (qaResultStatisticsService.NOT_MAPPED_MATE_1)                   : 13,
                (qaResultStatisticsService.NOT_MAPPED_MATE_2)                   : 14,
                (qaResultStatisticsService.MAPPED_SHORT_MATE_1)                 : 11,
                (qaResultStatisticsService.MAPPED_SHORT_MATE_2)                 : 12,
                (qaResultStatisticsService.MAPPED_LOW_QUALITY_MATE_1)           : 9,
                (qaResultStatisticsService.MAPPED_LOW_QUALITY_MATE_2)           : 10,
                (qaResultStatisticsService.MAPPED_QUALITY_LONG_MATE_1)          : 6,
                (qaResultStatisticsService.MAPPED_QUALITY_LONG_MATE_2)          : 7,
            ],
        ]
        List<Map> actual = qaResultStatisticsService.fetchResultsExtended(processedMergedBamFile)
        assertTrue EXPECT == actual
    }

    @Test
    void testFetchResultsExtendedExome() {
        seqType.name = SeqTypeNames.EXOME.seqTypeName
        seqType.save([flush: true])
        alignmentPass.seqTrack = exomeSeqTrack
        alignmentPass.save([flush: true])
        final List<Map> EXPECT = [
            // The two maps are (almost) identical. If different values are used, they need
            // to be changed accordingly. They also have a large overlap with the maps in
            // testFetchResultsSmall().
            [
                // Map for ProcessedMergedBamFile
                (qaResultStatisticsService.DUPLICATES_MATE_1)                   : 1,
                (qaResultStatisticsService.DUPLICATES_MATE_2)                   : 2,
                (qaResultStatisticsService.PE_READS_MAPPED_ON_DIFF_CHR)         : 33,
                (qaResultStatisticsService.INCORRECT_PE_ORIENTATION)            : 32,
                (qaResultStatisticsService.INCORRECT_PROPER_PAIR)               : 3,
                (qaResultStatisticsService.PERCENTAGE_QC_BASES_MAPPED_WITHOUT_N): '8/2910000',
                (qaResultStatisticsService.PERCENTAGE_QC_BASES_MAPPED_WITH_N)   : '8/3210000',
                (qaResultStatisticsService.NOT_MAPPED_MATE_1)                   : 13,
                (qaResultStatisticsService.NOT_MAPPED_MATE_2)                   : 14,
                (qaResultStatisticsService.MAPPED_SHORT_MATE_1)                 : 11,
                (qaResultStatisticsService.MAPPED_SHORT_MATE_2)                 : 12,
                (qaResultStatisticsService.MAPPED_LOW_QUALITY_MATE_1)           : 9,
                (qaResultStatisticsService.MAPPED_LOW_QUALITY_MATE_2)           : 10,
                (qaResultStatisticsService.MAPPED_QUALITY_LONG_MATE_1)          : 6,
                (qaResultStatisticsService.MAPPED_QUALITY_LONG_MATE_2)          : 7,
                (qaResultStatisticsService.ALL_MAPPED_BASES)                    : 66,
                (qaResultStatisticsService.TARGET_MAPPED_BASES)                 : 50,
            ],
        ]
        List<Map> actual = qaResultStatisticsService.fetchResultsExtended(processedMergedBamFile)
        assertTrue EXPECT == actual
    }

    @Test(expected = IllegalArgumentException)
    void testCreateOutputLineWhenArgumentValuesIsNull() {
        qaResultStatisticsService.createOutputLine(null, ["first", "second"])
    }

    @Test(expected = IllegalArgumentException)
    void testCreateOutputLineWhenArgumentSortOrderIsNull() {
        qaResultStatisticsService.createOutputLine(["one":"1", "two":"2"], null)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateOutputLineWhenValuesIsEmpty() {
        qaResultStatisticsService.createOutputLine([:], ["first", "second"])
    }

    @Test(expected = IllegalArgumentException)
    void testCreateOutputLineWhenSortOrderIsEmpty() {
        qaResultStatisticsService.createOutputLine(["one":"1", "two":"2"], [])
    }

    @Test
    void testCreateOutputLine() {
        def sortOrder = ["first", "second", "third"]
        def values = [ "second": "2", "third": "3", "first": "1" ]
        assertEquals "1\t2\t3\n", qaResultStatisticsService.createOutputLine(values, sortOrder)
    }

    @Test(expected = IllegalArgumentException)
    void testStatisticsFileWhenArgumentIsNull() {
        qaResultStatisticsService.statisticsFile(null)
    }

    @Test
    void testStatisticsFile() {
        TestConfigService configService = new TestConfigService()

        Map actual = qaResultStatisticsService.statisticsFile(processedMergedBamFile)

        // Location of the statistics file on the processing side, will be copied
        final FINAL_PATH_FILE = configService.getRootPath().path + "/projectDirName/sequencing/${seqType.dirName}/view-by-pid/pid_1/control/${seqType.libraryLayoutDirName}/merged-alignment/.tmp/QualityAssessment"

        Map expect = [
            'small': "${FINAL_PATH_FILE}/${FileNames.QA_RESULT_OVERVIEW}",
            'extended': "${FINAL_PATH_FILE}/${FileNames.QA_RESULT_OVERVIEW_EXTENDED}",
        ]
        assert expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testDefineOutputWhenArgumentIsNull() {
        qaResultStatisticsService.defineOutput(null)
    }

    @Test
    void testDefineOutputWholeGenome() {
        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.save([flush: true])
        Map result = qaResultStatisticsService.defineOutput(processedMergedBamFile)
        String actSmall = result["small"]
        String actExtended = result["extended"]
        List<String> expSmallHeader = [
            "pid",
            "mock full name",
            "sample type",
            "run id",
            "lane",
            "Coverage w/o N (2.91Mbp)",
            "Coverage wN (3.21Mbp)",
            "ChrX Coverage w/o N",
            "ChrY Coverage w/o N",
            "#QC bases mapped",
            "#mapped read count (flagstat)",
            "%mapped reads (flagstat)",
            "#total read count (flagstat)",
            "%properly_paired (flagstat)",
            "%singletons (flagstat)",
            "%duplicates (picard)",
            "Standard Deviation PE_insertsize",
            "Median PE_insertsize",
            "Mean PE_insertsize",
        ]
        List<String> expSmallMergedBamFile = [
            "pid_1",
            "mockFullName_1",
            "control",
            "all_merged",
            "all_merged",
            "0.00",
            "0.00",
            "0.16",
            "0.16",
            "8",
            "19",
            "34.55",
            "55",
            "52.27",
            "49.09",
            "32.73",
            "29.00",
            "30.00",
            "28.00",
        ]

        String expSmall = expSmallHeader.join("\t") + "\n" + expSmallMergedBamFile.join("\t") + "\n"

        List<String> expExtendedHeader = [
            "pid",
            "mock full name",
            "sample type",
            "run id",
            "lane",
            "Coverage w/o N (2.91Mbp)",
            "Coverage wN (3.21Mbp)",
            "ChrX Coverage w/o N",
            "ChrY Coverage w/o N",
            "#QC bases mapped",
            "#mapped read count (flagstat)",
            "%mapped reads (flagstat)",
            "#total read count (flagstat)",
            "%properly_paired (flagstat)",
            "%singletons (flagstat)",
            "%duplicates (picard)",
            "Standard Deviation PE_insertsize",
            "Median PE_insertsize",
            "Mean PE_insertsize",
            "#duplicates mate1",
            "#duplicates mate2",
            "%PE reads mapped on diff chromosomes",
            "%incorrect PE orientation",
            "incorrect proper pair",
            "QC bases/ total bases w/o N",
            "QC bases/ total bases w N",
            "mapq=0 mate1",
            "mapq=0 mate2",
            "mapq>0,readlength<minlength mate1",
            "mapq>0,readlength<minlength mate2",
            "mapq>0,BaseQualityMedian<basequalCutoff mate1",
            "mapq>0,BaseQualityMedian<basequalCutoff mate2",
            "mapq>0,BaseQualityMedian>=basequalCutoff mate1",
            "mapq>0,BaseQualityMedian>=basequalCutoff mate2",
        ]

        List<String> expExtendedMergedBamFile = [
            "pid_1",
            "mockFullName_1",
            "control",
            "all_merged",
            "all_merged",
            "0.00",
            "0.00",
            "0.16",
            "0.16",
            "8",
            "19",
            "34.55",
            "55",
            "52.27",
            "49.09",
            "32.73",
            "29.00",
            "30.00",
            "28.00",
            "1",
            "2",
            "33.0",
            "32.0",
            "3",
            "8/2910000",
            "8/3210000",
            "13",
            "14",
            "11",
            "12",
            "9",
            "10",
            "6",
            "7",
        ]
        String expExtended = expExtendedHeader.join("\t") + "\n" + expExtendedMergedBamFile.join("\t") + "\n"

        assertEquals expSmall, actSmall
        assertEquals expExtended, actExtended
    }

    @Test
    void testDefineOutputExome() {
        seqType.name = SeqTypeNames.EXOME.seqTypeName
        seqType.save([flush: true])
        alignmentPass.seqTrack = exomeSeqTrack
        alignmentPass.save([flush: true])
        Map result = qaResultStatisticsService.defineOutput(processedMergedBamFile)
        String actSmall = result["small"]
        String actExtended = result["extended"]
        List<String> expSmallHeader = [
            "pid",
            "mock full name",
            "sample type",
            "run id",
            "lane",
            "%onTarget",
            "target Coverage",
            "#total read count (flagstat)",
            "%properly_paired (flagstat)",
            "%singletons (flagstat)",
            "%duplicates (picard)",
            "Standard Deviation PE_insertsize",
            "Median PE_insertsize",
            "Mean PE_insertsize",
        ]
        List<String> expSmallMergedBamFile = [
            "pid_1",
            "mockFullName_1",
            "control",
            "all_merged",
            "all_merged",
            "75.76",
            "0.83",
            "55",
            "52.27",
            "49.09",
            "32.73",
            "29.00",
            "30.00",
            "28.00",
        ]
        String expSmall = expSmallHeader.join("\t") + "\n" +expSmallMergedBamFile.join("\t") + "\n"

        List<String> expExtendedHeader = [
            "pid",
            "mock full name",
            "sample type",
            "run id",
            "lane",
            "%onTarget",
            "target Coverage",
            "#total read count (flagstat)",
            "%properly_paired (flagstat)",
            "%singletons (flagstat)",
            "%duplicates (picard)",
            "Standard Deviation PE_insertsize",
            "Median PE_insertsize",
            "Mean PE_insertsize",
            "target mapped bases",
            "all mapped bases",
            "#duplicates mate1",
            "#duplicates mate2",
            "%PE reads mapped on diff chromosomes",
            "%incorrect PE orientation",
            "incorrect proper pair",
            "QC bases/ total bases w/o N",
            "QC bases/ total bases w N",
            "mapq=0 mate1",
            "mapq=0 mate2",
            "mapq>0,readlength<minlength mate1",
            "mapq>0,readlength<minlength mate2",
            "mapq>0,BaseQualityMedian<basequalCutoff mate1",
            "mapq>0,BaseQualityMedian<basequalCutoff mate2",
            "mapq>0,BaseQualityMedian>=basequalCutoff mate1",
            "mapq>0,BaseQualityMedian>=basequalCutoff mate2",
        ]
        List<String> expExpectedMergedBamFile = [
            "pid_1",
            "mockFullName_1",
            "control",
            "all_merged",
            "all_merged",
            "75.76",
            "0.83",
            "55",
            "52.27",
            "49.09",
            "32.73",
            "29.00",
            "30.00",
            "28.00",
            "50",
            "66",
            "1",
            "2",
            "33.0",
            "32.0",
            "3",
            "8/2910000",
            "8/3210000",
            "13",
            "14",
            "11",
            "12",
            "9",
            "10",
            "6",
            "7",
        ]

        String expExtended = expExtendedHeader.join("\t") + "\n" + expExpectedMergedBamFile.join("\t") + "\n"
        assertEquals expSmall, actSmall
        assertEquals expExtended, actExtended
    }

    private void setProperties(AbstractQualityAssessment abstractQualityAssessment) {
        [
            referenceLength: 1000,
            duplicateR1: 1,
            duplicateR2: 2,
            properPairStrandConflict: 3,
            referenceAgreement: 4,
            referenceAgreementStrandConflict: 5,
            mappedQualityLongR1: 6,
            mappedQualityLongR2: 7,
            qcBasesMapped: 8,
            mappedLowQualityR1: 9,
            mappedLowQualityR2: 10,
            mappedShortR1: 11,
            mappedShortR2: 12,
            notMappedR1: 13,
            notMappedR2: 14,
            endReadAberration: 15,
            totalReadCounter: 55,
            qcFailedReads: 17,
            duplicates: 18,
            totalMappedReadCounter: 19,
            pairedInSequencing: 44,
            pairedRead2: 21,
            pairedRead1: 22,
            properlyPaired: 23,
            withItselfAndMateMapped: 24,
            withMateMappedToDifferentChr: 25,
            withMateMappedToDifferentChrMaq: 26,
            singletons: 27,
            insertSizeMean: 28d,
            insertSizeSD: 29d,
            insertSizeMedian: 30d,
            insertSizeRMS: 31d,
            percentIncorrectPEorientation: 32,
            percentReadPairsMapToDiffChrom: 33,
            allBasesMapped: 66,
            onTargetMappedBases: 50,
        ].each { key, value ->
            abstractQualityAssessment."${key}" = value
        }
    }
}
