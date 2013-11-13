package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
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

    Project project
    Sample sample
    Run run
    Individual individual
    SeqTrack seqTrack
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
        Realm realm = new Realm()
        realm.cluster = Realm.Cluster.DKFZ
        realm.rootPath = "/tmp/otp-unit-test/pmfs/root"
        realm.processingRootPath = "/tmp/otp-unit-test/pmbfs/processing"
        realm.programsRootPath = ""
        realm.webHost = ""
        realm.host = ""
        realm.port = 8080
        realm.unixUser = ""
        realm.timeout = 1000
        realm.pbsOptions = ""
        realm.name = "realmName"
        realm.operationType = Realm.OperationType.DATA_PROCESSING
        realm.env = Environment.getCurrent().getName()
        realm.save([flush: true])

        Realm realm1 = new Realm()
        realm1.cluster = Realm.Cluster.DKFZ
        realm1.rootPath = "/tmp/otp-unit-test/pmfs/root"
        realm1.processingRootPath = "/tmp/otp-unit-test/pmbfs/processing"
        realm1.programsRootPath = ""
        realm1.webHost = ""
        realm1.host = ""
        realm1.port = 8080
        realm1.unixUser = ""
        realm1.timeout = 1000
        realm1.pbsOptions = ""
        realm1.name = "realmName"
        realm1.operationType = Realm.OperationType.DATA_MANAGEMENT
        realm1.env = Environment.getCurrent().getName()
        realm1.save([flush: true])


        project = new Project(
                        name: "projectName",
                        dirName: "projectDirName",
                        realmName: "realmName"
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

        SeqType seqType = new SeqType(
                        name: "seqTypeName",
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

        AlignmentPass alignmentPass = new AlignmentPass(
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

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
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
    void testPrepareFetchingSingleLaneResultsWhenArgumentIsNull() {
        QAResultStatisticsService.prepareFetchingSingleLaneResults(null)
    }

    @Test
    void testPrepareFetchingSingleLaneResults() {
        Map actual = QAResultStatisticsService.prepareFetchingSingleLaneResults(processedBamFile)
        Map expect = [
            (QAResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_X): chromosomeQualityAssessmentChrX,
            (QAResultStatisticsService.CHROMOSOME_QUALITY_ASSESSMENT_CHR_Y): chromosomeQualityAssessmentChrY,
            (QAResultStatisticsService.OVERALL_QUALITY_ASSESSMENT): overallQualityAssessment,
            (QAResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_X): referenceGenomeEntryChrX,
            (QAResultStatisticsService.REFERENCE_GENOME_ENTRY_CHR_Y): referenceGenomeEntryChrY,
            (QAResultStatisticsService.REFERENCE_GENOME): referenceGenome,
            (QAResultStatisticsService.INDIVIDUAL): individual,
            (QAResultStatisticsService.SAMPLE): sample,
            (QAResultStatisticsService.RUN): 'runName',
            (QAResultStatisticsService.LANE): processedBamFile.alignmentPass.seqTrack.laneId
        ]
        // assertEquals will not DTRT here, we have to use the equals() method
        assertTrue expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testPrepareFetchingMergedBamFileResultsWhenArgumentIsNull() {
        QAResultStatisticsService.prepareFetchingMergedBamFileResults(null)
    }

    @Test
    void testPrepareFetchingMergedBamFileResults() {
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
        ]
        // assertEquals will not DTRT here, we have to use the equals() method
        assertTrue expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testFetchResultsSmallWhenArgumentIsNull() {
        QAResultStatisticsService.fetchResultsSmall(null)
    }

    @Test
    void testFetchResultsSmall() {
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
            [
                // Map for ProcessedBamFile
                (QAResultStatisticsService.REFERENCE_GENOME_LENGTH_WITH_N): '3.21',
                (QAResultStatisticsService.REFERENCE_GENOME_LENGTH_WITHOUT_N): '2.91',
                (QAResultStatisticsService.PID): 'pid_1',
                (QAResultStatisticsService.MOCK_FULL_NAME): 'mockFullName_1',
                (QAResultStatisticsService.SAMPLE_TYPE): 'control',
                (QAResultStatisticsService.RUN_ID): 'runName',
                (QAResultStatisticsService.LANE): 'laneId',
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

    @Test(expected = IllegalArgumentException)
    void testFetchResultsExtendedWhenArgumentIsNull() {
        QAResultStatisticsService.fetchResultsExtended(null)
    }

    @Test
    void testFetchResultsExtended() {
        List<Map> actual = QAResultStatisticsService.fetchResultsExtended(processedMergedBamFile)
        List<Map> expect = [
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
            [
                // Map for ProcessedBamFile
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
        assertTrue expect == actual
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

    // FIXME: Should we also test (in "createOutputLine") if keys of values is a superset of elements in sortOrder?

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
            'small': "${FINAL_PATH_FILE}/QAResultOverview.tsv",
            'extended': "${FINAL_PATH_FILE}/QAResultOverviewExtended.tsv",
        ]
        assertTrue expect == actual
    }

    @Test(expected = IllegalArgumentException)
    void testDefineOutputWhenArgumentIsNull() {
        QAResultStatisticsService.defineOutput(null)
    }

    @Test
    void testDefineOutput() {
        Map result = QAResultStatisticsService.defineOutput(processedMergedBamFile)
        String actSmall = result["small"]
        String actExtended = result["extended"]
        String expSmall = """\
pid\tmock full name\tsample type\trun id\tlane\tCoverage w/o N (2.91Mbp)\tCoverage wN (3.21Mbp)\tChrX Coverage w/o N\tChrY Coverage w/o N\t#QC bases mapped\t#total read count (flagstat)\t#mapped read count (flagstat)\t%mapped reads (flagstat)\t%properly_paired (flagstat)\t%singletons (flagstat)\t%duplicates (picard)\tStandard Deviation PE_insertsize\tMedian PE_insertsize\tMean PE_insertsize
pid_1\tmockFullName_1\tcontrol\tall_merged\tall_merged\t0.00\t0.00\t0.16\t0.16\t8\t55\t19\t34.55\t52.27\t49.09\t32.73\t29.00\t30.00\t28.00
pid_1\tmockFullName_1\tcontrol\trunName\tlaneId\t0.00\t0.00\t0.16\t0.16\t8\t55\t19\t34.55\t52.27\t49.09\t32.73\t29.00\t30.00\t28.00
"""
        String expExtended = """\
pid\tmock full name\tsample type\trun id\tlane\tCoverage w/o N (2.91Mbp)\tCoverage wN (3.21Mbp)\tChrX Coverage w/o N\tChrY Coverage w/o N\t#QC bases mapped\t#total read count (flagstat)\t#mapped read count (flagstat)\t%mapped reads (flagstat)\t%properly_paired (flagstat)\t%singletons (flagstat)\t%duplicates (picard)\tStandard Deviation PE_insertsize\tMedian PE_insertsize\tMean PE_insertsize\t#duplicates Read1\t#duplicates Read2\t%PE reads mapped on diff chromosomes\t%incorrect PE orientation\tincorrect proper pair\tQC bases/ total bases w/o N\tQC bases/ total bases w N\tmapq=0 read1\tmapq=0 read2\tmapq>0,readlength<minlength read1\tmapq>0,readlength<minlength read2\tmapq>0,BaseQualityMedian<basequalCutoff read1\tmapq>0,BaseQualityMedian<basequalCutoff read2\tmapq>0,BaseQualityMedian>=basequalCutoff read1\tmapq>0,BaseQualityMedian>=basequalCutoff read2
pid_1\tmockFullName_1\tcontrol\tall_merged\tall_merged\t0.00\t0.00\t0.16\t0.16\t8\t55\t19\t34.55\t52.27\t49.09\t32.73\t29.00\t30.00\t28.00\t1\t2\t33.0\t32.0\t3\t8/2910000\t8/3210000\t13\t14\t11\t12\t9\t10\t6\t7
pid_1\tmockFullName_1\tcontrol\trunName\tlaneId\t0.00\t0.00\t0.16\t0.16\t8\t55\t19\t34.55\t52.27\t49.09\t32.73\t29.00\t30.00\t28.00\t1\t2\t33.0\t32.0\t3\t8/2910000\t8/3210000\t13\t14\t11\t12\t9\t10\t6\t7
"""
        assertEquals expSmall, actSmall
        assertEquals expExtended, actExtended
    }

    private void setProperties(AbstractQualityAssessment abstractQualityAssessment) {
        // TODO: more realistic values might be nice, but need changes to the rest of the test
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
        ].each { key, value ->
            abstractQualityAssessment."${key}" = value
        }
    }
}
