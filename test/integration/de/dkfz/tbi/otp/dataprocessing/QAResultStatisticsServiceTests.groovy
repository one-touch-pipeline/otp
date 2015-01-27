package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.filehandling.FileNames
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

/**
 * Integration tests for the {@link QAResultStatisticsService}.
 *
 *
 */
class QAResultStatisticsServiceTests {

    /*
     * NOTE: Since the name of the service class starts with two upper case letters, the name of
     *       the property also has to (according to the JavaBeans API Specification, section 8.8).
     *       Also, adding the type went my Eclipse go nuts. Using the def keyword works just fine.
     */
    def QAResultStatisticsService

    // Location of the statistics file on the processing side, will be copied
    final static PATH_TO_STAT_FILE = '/tmp/otp-unit-test/pmbfs/processing/projectDirName/results_per_pid/pid_1/merging//control/seqTypeName/seqTypeLibrary/DEFAULT/0/pass0/QualityAssessment/pass1'
    final static FINAL_PATH_FILE = '/tmp/otp-unit-test/pmfs/root/projectDirName/sequencing/seqTypeDirName/view-by-pid/pid_1/control/seqtypelibrary/merged-alignment/.tmp/QualityAssessment'

    TestData testData = new TestData()
    Project project
    Sample sample
    Run run
    Individual individual
    SeqTrack seqTrack
    ExomeSeqTrack exomeSeqTrack
    ExomeEnrichmentKit exomeEnrichmentKit
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
        Map paths = [
            rootPath: '/tmp/otp-unit-test/pmfs/root',
            processingRootPath: '/tmp/otp-unit-test/pmbfs/processing',
        ]

        Realm realm = DomainFactory.createRealmDataProcessingDKFZ(paths).save([flush: true])
        Realm realm1 = DomainFactory.createRealmDataManagementDKFZ(paths).save([flush: true])

        project = new Project(
                        name: "projectName",
                        dirName: "projectDirName",
                        realmName: 'DKFZ',
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

        seqType = new SeqType(
                        name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                        libraryLayout: "seqTypeLibrary",
                        dirName: "seqTypeDirName"
                        )
        assertNotNull(seqType.save([flush: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
                        name: "Illumina",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true]))

        SeqCenter seqCenter = new SeqCenter(
                        name: "seqCenterName",
                        dirName: "seqCenterDirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        run = new Run(
                        name: "runName",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: Run.StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "softwareToolName",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true]))

        exomeEnrichmentKit = new ExomeEnrichmentKit(
                        name: "exomeEnrichmentKit"
                        )
        assertNotNull(exomeEnrichmentKit.save([flush: true]))

        exomeSeqTrack = new ExomeSeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        exomeEnrichmentKit: exomeEnrichmentKit,
                        kitInfoReliability: InformationReliability.KNOWN
                        )
        assertNotNull(exomeSeqTrack.save([flush: true]))

        alignmentPass = testData.createAlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack,
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

        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
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

        processedMergedBamFile = new ProcessedMergedBamFile(
                        identifier: 20,
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: BamType.MDUP,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass(
                        identifier: 1,
                        description: "text2",
                        processedMergedBamFile: processedMergedBamFile
                        )
        assertNotNull(qualityAssessmentMergedPass.save([flush: true]))

        referenceGenome = new ReferenceGenome(
                        name: "referenceGenome",
                        path: "pathToReferenceGenome",
                        fileNamePrefix: "referenceGenomePrefix",
                        length: 3210000,
                        lengthWithoutN: 2910000,
                        lengthRefChromosomes: 800,
                        lengthRefChromosomesWithoutN: 750
                        )
        assertNotNull(referenceGenome.save([flush: true]))

        BedFile bedFile = new BedFile(
                        fileName: "bedFile",
                        targetSize: 80,
                        mergedTargetSize: 60,
                        referenceGenome: referenceGenome,
                        exomeEnrichmentKit: exomeEnrichmentKit
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
        exomeEnrichmentKit = null
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
        QAResultStatisticsService.formatToTwoDecimals(null)
    }

    @Test
    void testFormatToTwoDecimals() {
        assertEquals "-1.00", QAResultStatisticsService.formatToTwoDecimals(-1)
        assertEquals "0.00", QAResultStatisticsService.formatToTwoDecimals(0)
        assertEquals "2.00", QAResultStatisticsService.formatToTwoDecimals(2)
        assertEquals "3.14", QAResultStatisticsService.formatToTwoDecimals(3.14159)
        assertEquals "123.46", QAResultStatisticsService.formatToTwoDecimals(123456789 / 1e6)
    }


    @Test(expected = IllegalArgumentException)
    void testPrepareFetchingMergedBamFileResultsWhenArgumentIsNull() {
        QAResultStatisticsService.prepareFetchingMergedBamFileResults(null)
    }

    @Test
    void testPrepareFetchingMergedBamFileResultsWholeGenome() {
        Map actual = QAResultStatisticsService.prepareFetchingMergedBamFileResults(processedMergedBamFile)
        Map expect = [
            (QAResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_X): chromosomeQualityAssessmentMergedChrX,
            (QAResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y): chromosomeQualityAssessmentMergedChrY,
            (QAResultStatisticsService.OVERALL_QUALITY_ASSESSMENT): overallQualityAssessmentMerged,
            (QAResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_X): referenceGenomeEntryChrX,
            (QAResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_Y): referenceGenomeEntryChrY,
            (QAResultStatisticsService.REFERENCE_GENOME): referenceGenome,
            (QAResultStatisticsService.INDIVIDUAL): individual,
            (QAResultStatisticsService.SAMPLE): sample,
            (QAResultStatisticsService.LANE): 'all_merged',
            (QAResultStatisticsService.RUN): 'all_merged',
            (QAResultStatisticsService.SEQTYPE): seqType,
            (QAResultStatisticsService.EXOME_ENRICHMENT_KIT): null
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
        Map actual = QAResultStatisticsService.prepareFetchingMergedBamFileResults(processedMergedBamFile)
        Map expect = [
            (QAResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_X): chromosomeQualityAssessmentMergedChrX,
            (QAResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y): chromosomeQualityAssessmentMergedChrY,
            (QAResultStatisticsService.OVERALL_QUALITY_ASSESSMENT): overallQualityAssessmentMerged,
            (QAResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_X): referenceGenomeEntryChrX,
            (QAResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_Y): referenceGenomeEntryChrY,
            (QAResultStatisticsService.REFERENCE_GENOME): referenceGenome,
            (QAResultStatisticsService.INDIVIDUAL): individual,
            (QAResultStatisticsService.SAMPLE): sample,
            (QAResultStatisticsService.LANE): 'all_merged',
            (QAResultStatisticsService.RUN): 'all_merged',
            (QAResultStatisticsService.SEQTYPE): seqType,
            (QAResultStatisticsService.EXOME_ENRICHMENT_KIT): exomeEnrichmentKit
        ]
        // assertEquals will not DTRT here, we have to use the equals() method
        assertTrue expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testFetchResultsSmallWhenArgumentIsNull() {
        QAResultStatisticsService.fetchResultsSmall(null)
    }

    @Test
    void testFetchResultsSmallWholeGenome() {
        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.save([flush: true])
        List<Map> actual = QAResultStatisticsService.fetchResultsSmall(processedMergedBamFile)
        List<Map> expect = [
            // The two maps are (almost) identical. If different values are used, they need
            // to be changed accordingly.
            [
                // Map for ProcessedMergedBamFile
                (QAResultStatisticsService.REFERENCE_GENOME_LENGTH_WITH_N): '3.21',
                (QAResultStatisticsService.REFERENCE_GENOME_LENGTH_WITHOUT_N): '2.91',
                (QAResultStatisticsService.PID): 'pid_1',
                (QAResultStatisticsService.MOCK_FULL_NAME): 'mockFullName_1',
                (QAResultStatisticsService.SAMPLE_TYPE): 'control',
                (QAResultStatisticsService.RUN_ID): 'all_merged',
                (QAResultStatisticsService.LANE): 'all_merged',
                (QAResultStatisticsService.COVERAGE_WITHOUT_N): '0.00',
                (QAResultStatisticsService.COVERAGE_WITH_N): '0.00',
                (QAResultStatisticsService.COVERAGE_WITHOUT_N_CHR_X): '0.16',
                (QAResultStatisticsService.COVERAGE_WITHOUT_N_CHR_Y): '0.16',
                (QAResultStatisticsService.QC_BASES_MAPPED): 8,
                (QAResultStatisticsService.TOTAL_READ_COUNT): 55,
                (QAResultStatisticsService.MAPPED_READ_COUNT): 19,
                (QAResultStatisticsService.PERCENTAGE_MAPPED_READS): '34.55',
                (QAResultStatisticsService.PROPERLY_PAIRED): '52.27',
                (QAResultStatisticsService.SINGLETONS): '49.09',
                (QAResultStatisticsService.DUPLICATES): '32.73',
                (QAResultStatisticsService.INSERT_SIZE_SD): '29.00',
                (QAResultStatisticsService.INSERT_SIZE_MEDIAN): '30.00',
                (QAResultStatisticsService.INSERT_SIZE_MEAN): '28.00',
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
        List<Map> actual = QAResultStatisticsService.fetchResultsSmall(processedMergedBamFile)
        List<Map> expect = [
            // The two maps are (almost) identical. If different values are used, they need
            // to be changed accordingly.
            [
                // Map for ProcessedMergedBamFile
                (QAResultStatisticsService.REFERENCE_GENOME_LENGTH_WITH_N): '3.21',
                (QAResultStatisticsService.REFERENCE_GENOME_LENGTH_WITHOUT_N): '2.91',
                (QAResultStatisticsService.PID): 'pid_1',
                (QAResultStatisticsService.MOCK_FULL_NAME): 'mockFullName_1',
                (QAResultStatisticsService.SAMPLE_TYPE): 'control',
                (QAResultStatisticsService.RUN_ID): 'all_merged',
                (QAResultStatisticsService.LANE): 'all_merged',
                (QAResultStatisticsService.COVERAGE_WITHOUT_N): '0.00',
                (QAResultStatisticsService.COVERAGE_WITH_N): '0.00',
                (QAResultStatisticsService.COVERAGE_WITHOUT_N_CHR_X): '0.16',
                (QAResultStatisticsService.COVERAGE_WITHOUT_N_CHR_Y): '0.16',
                (QAResultStatisticsService.QC_BASES_MAPPED): 8,
                (QAResultStatisticsService.TOTAL_READ_COUNT): 55,
                (QAResultStatisticsService.MAPPED_READ_COUNT): 19,
                (QAResultStatisticsService.PERCENTAGE_MAPPED_READS): '34.55',
                (QAResultStatisticsService.PROPERLY_PAIRED): '52.27',
                (QAResultStatisticsService.SINGLETONS): '49.09',
                (QAResultStatisticsService.DUPLICATES): '32.73',
                (QAResultStatisticsService.INSERT_SIZE_SD): '29.00',
                (QAResultStatisticsService.INSERT_SIZE_MEDIAN): '30.00',
                (QAResultStatisticsService.INSERT_SIZE_MEAN): '28.00',
                (QAResultStatisticsService.TARGET_COVERAGE): '0.83',
                (QAResultStatisticsService.ON_TARGET_RATE): '75.76',
            ],
        ]
        assertTrue expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testFetchResultsExtendedWhenArgumentIsNull() {
        QAResultStatisticsService.fetchResultsExtended(null)
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
                (QAResultStatisticsService.DUPLICATES_READ_1): 1,
                (QAResultStatisticsService.DUPLICATES_READ_2): 2,
                (QAResultStatisticsService.PE_READS_MAPPED_ON_DIFF_CHR): 33,
                (QAResultStatisticsService.INCORRECT_PE_ORIENTATION): 32,
                (QAResultStatisticsService.INCORRECT_PROPER_PAIR): 3,
                (QAResultStatisticsService.PERCENTAGE_QC_BASES_MAPPED_WITHOUT_N): '8/2910000',
                (QAResultStatisticsService.PERCENTAGE_QC_BASES_MAPPED_WITH_N): '8/3210000',
                (QAResultStatisticsService.NOT_MAPPED_READ_1): 13,
                (QAResultStatisticsService.NOT_MAPPED_READ_2): 14,
                (QAResultStatisticsService.MAPPED_SHORT_READ_1): 11,
                (QAResultStatisticsService.MAPPED_SHORT_READ_2): 12,
                (QAResultStatisticsService.MAPPED_LOW_QUALITY_READ_1): 9,
                (QAResultStatisticsService.MAPPED_LOW_QUALITY_READ_2): 10,
                (QAResultStatisticsService.MAPPED_QUALITY_LONG_READ_1): 6,
                (QAResultStatisticsService.MAPPED_QUALITY_LONG_READ_2): 7,
            ],
        ]
        List<Map> actual = QAResultStatisticsService.fetchResultsExtended(processedMergedBamFile)
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
                (QAResultStatisticsService.DUPLICATES_READ_1): 1,
                (QAResultStatisticsService.DUPLICATES_READ_2): 2,
                (QAResultStatisticsService.PE_READS_MAPPED_ON_DIFF_CHR): 33,
                (QAResultStatisticsService.INCORRECT_PE_ORIENTATION): 32,
                (QAResultStatisticsService.INCORRECT_PROPER_PAIR): 3,
                (QAResultStatisticsService.PERCENTAGE_QC_BASES_MAPPED_WITHOUT_N): '8/2910000',
                (QAResultStatisticsService.PERCENTAGE_QC_BASES_MAPPED_WITH_N): '8/3210000',
                (QAResultStatisticsService.NOT_MAPPED_READ_1): 13,
                (QAResultStatisticsService.NOT_MAPPED_READ_2): 14,
                (QAResultStatisticsService.MAPPED_SHORT_READ_1): 11,
                (QAResultStatisticsService.MAPPED_SHORT_READ_2): 12,
                (QAResultStatisticsService.MAPPED_LOW_QUALITY_READ_1): 9,
                (QAResultStatisticsService.MAPPED_LOW_QUALITY_READ_2): 10,
                (QAResultStatisticsService.MAPPED_QUALITY_LONG_READ_1): 6,
                (QAResultStatisticsService.MAPPED_QUALITY_LONG_READ_2): 7,
                (QAResultStatisticsService.ALL_MAPPED_BASES): 66,
                (QAResultStatisticsService.TARGET_MAPPED_BASES): 50,
            ],
        ]
        List<Map> actual = QAResultStatisticsService.fetchResultsExtended(processedMergedBamFile)
        assertTrue EXPECT == actual
    }

    @Test(expected = IllegalArgumentException)
    void testCreateOutputLineWhenArgumentValuesIsNull() {
        QAResultStatisticsService.createOutputLine(null, ["first", "second"])
    }

    @Test(expected = IllegalArgumentException)
    void testCreateOutputLineWhenArgumentSortOrderIsNull() {
        QAResultStatisticsService.createOutputLine(["one":"1", "two":"2"], null)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateOutputLineWhenValuesIsEmpty() {
        QAResultStatisticsService.createOutputLine([:], ["first", "second"])
    }

    @Test(expected = IllegalArgumentException)
    void testCreateOutputLineWhenSortOrderIsEmpty() {
        QAResultStatisticsService.createOutputLine(["one":"1", "two":"2"], [])
    }

    @Test
    void testCreateOutputLine() {
        def sortOrder = ["first", "second", "third"]
        def values = [ "second": "2", "third": "3", "first": "1" ]
        assertEquals "1\t2\t3\n", QAResultStatisticsService.createOutputLine(values, sortOrder)
    }

    @Test(expected = IllegalArgumentException)
    void testStatisticsFileWhenArgumentIsNull() {
        QAResultStatisticsService.statisticsFile(null)
    }

    @Test
    void testStatisticsFile() {
        Map actual = QAResultStatisticsService.statisticsFile(processedMergedBamFile)
        Map expect = [
            'small': "${FINAL_PATH_FILE}/${FileNames.QA_RESULT_OVERVIEW}",
            'extended': "${FINAL_PATH_FILE}/${FileNames.QA_RESULT_OVERVIEW_EXTENDED}",
        ]
        assertTrue expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testDefineOutputWhenArgumentIsNull() {
        QAResultStatisticsService.defineOutput(null)
    }

    @Test
    void testDefineOutputWholeGenome() {
        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.save([flush: true])
        Map result = QAResultStatisticsService.defineOutput(processedMergedBamFile)
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
            "#duplicates Read1",
            "#duplicates Read2",
            "%PE reads mapped on diff chromosomes",
            "%incorrect PE orientation",
            "incorrect proper pair",
            "QC bases/ total bases w/o N",
            "QC bases/ total bases w N",
            "mapq=0 read1",
            "mapq=0 read2",
            "mapq>0,readlength<minlength read1",
            "mapq>0,readlength<minlength read2",
            "mapq>0,BaseQualityMedian<basequalCutoff read1",
            "mapq>0,BaseQualityMedian<basequalCutoff read2",
            "mapq>0,BaseQualityMedian>=basequalCutoff read1",
            "mapq>0,BaseQualityMedian>=basequalCutoff read2",
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
        Map result = QAResultStatisticsService.defineOutput(processedMergedBamFile)
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
            "#duplicates Read1",
            "#duplicates Read2",
            "%PE reads mapped on diff chromosomes",
            "%incorrect PE orientation",
            "incorrect proper pair",
            "QC bases/ total bases w/o N",
            "QC bases/ total bases w N",
            "mapq=0 read1",
            "mapq=0 read2",
            "mapq>0,readlength<minlength read1",
            "mapq>0,readlength<minlength read2",
            "mapq>0,BaseQualityMedian<basequalCutoff read1",
            "mapq>0,BaseQualityMedian<basequalCutoff read2",
            "mapq>0,BaseQualityMedian>=basequalCutoff read1",
            "mapq>0,BaseQualityMedian>=basequalCutoff read2",
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
