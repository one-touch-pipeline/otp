package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.test.mixin.domain.DomainClassUnitTestMixin
import static org.junit.Assert.*
import org.junit.*
import grails.util.Environment
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.FastqcBasicStatistics
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class QualityAssessmentPassServiceTests extends AbstractIntegrationTest {

    QualityAssessmentPassService qualityAssessmentPassService
    SeqTrackService SeqTrackService

    QualityAssessmentPass qualityAssessmentPass
    ProcessedBamFile processedBamFile
    Project project
    Realm realm
    SeqTrack seqTrack

    @Before
    void setUp() {
        project = new Project(
                        name: "TheProject",
                        dirName: "project-dir",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true]))

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

        SeqType seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        SeqPlatform seqPlatform = new SeqPlatform()
        seqPlatform.name = "instrumentPlatform"
        seqPlatform.model = "instrumentModel"

        assertNotNull(seqPlatform.save(flush: true))

        SeqPlatformModelIdentifier seqPlatformModelIdentifier = new SeqPlatformModelIdentifier()
        seqPlatformModelIdentifier.name = "Illumina"
        seqPlatformModelIdentifier.seqPlatform = seqPlatform
        assertNotNull(seqPlatformModelIdentifier.save(flush: true))

        SeqCenter seqCenter = new SeqCenter(
                        name: "TheSequencingCenter",
                        dirName: "TheSequencingCenter"
                        )
        assertNotNull(seqCenter.save(flush: true))

        final String SOFTWARE_TOOL_NAME = "CASAVA"
        final String SOFTWARE_TOOL_VERSION = "1.8.2"
        SoftwareTool softwareTool = new SoftwareTool()
        softwareTool.programName = SOFTWARE_TOOL_NAME
        softwareTool.programVersion = SOFTWARE_TOOL_VERSION
        softwareTool.type = SoftwareTool.Type.BASECALLING
        assertNotNull(softwareTool.save(flush: true))

        SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier()
        softwareToolIdentifier.name = "${SOFTWARE_TOOL_NAME}-${SOFTWARE_TOOL_VERSION}"
        softwareToolIdentifier.softwareTool = softwareTool
        assertNotNull(softwareToolIdentifier.save(flush: true))

        Run run = new Run()
        run.name = "1234_A00123_0012_ABCDEFGHIJ"
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        assertNotNull(run.save([flush: true, failOnError: true]))

        seqTrack = new SeqTrack(
            laneId: 0,
            run: run,
            sample: sample,
            seqType: seqType,
            seqPlatform: seqPlatform,
            pipelineVersion: softwareTool)
        assertNotNull(seqTrack.save([flush: true]))

        AlignmentPass alignmentPass = new AlignmentPass(
            identifier: 0,
            seqTrack: seqTrack,
            description: "firstPass")
        assertNotNull(alignmentPass.save([flush: true]))

        processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        fileExists: true,
                        type: BamType.SORTED,
                        qualityAssessmentStatus: QaProcessingStatus.UNKNOWN,
                        fileOperationStatus: FileOperationStatus.DECLARED,
                        md5sum: null,
                        withdrawn: false,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedBamFile.save([flush: true]))

        qualityAssessmentPass = new QualityAssessmentPass(
                        processedBamFile: processedBamFile,
                        identifier: 0,
                        description: 'QualtiyAssessmentPassDescription'
                        )
        assertNotNull(qualityAssessmentPass.save([flush: true]))
    }

    @After
    void tearDown() {
        qualityAssessmentPass = null
        project = null
        processedBamFile = null
        realm = null
        seqTrack = null
    }

    @Test(expected = IllegalArgumentException)
    void testPassStartedNullInput() {
        qualityAssessmentPassService.passStarted(null)
    }

    @Test
    void testPassStarted() {
        assertEquals(QaProcessingStatus.UNKNOWN, processedBamFile.qualityAssessmentStatus)
        qualityAssessmentPassService.passStarted(qualityAssessmentPass)
        assertEquals(QaProcessingStatus.IN_PROGRESS, processedBamFile.qualityAssessmentStatus)
    }

    @Test
    void testNotStarted() {
        assertEquals(QaProcessingStatus.UNKNOWN, processedBamFile.qualityAssessmentStatus)
        qualityAssessmentPassService.notStarted(processedBamFile)
        assertEquals(QaProcessingStatus.NOT_STARTED, processedBamFile.qualityAssessmentStatus)
    }

    @Test(expected = IllegalArgumentException)
    void testPassFinishedNullInput() {
        qualityAssessmentPassService.passFinished(null)
    }

    @Test
    void testPassFinished() {
        assertEquals(QaProcessingStatus.UNKNOWN, processedBamFile.qualityAssessmentStatus)
        assertEquals(FileOperationStatus.DECLARED, processedBamFile.fileOperationStatus)
        qualityAssessmentPassService.passFinished(qualityAssessmentPass)
        assertEquals(QaProcessingStatus.FINISHED, processedBamFile.qualityAssessmentStatus)
    }

    @Test(expected = IllegalArgumentException)
    void testUpdateStateNull() {
        qualityAssessmentPassService.update(qualityAssessmentPass, null)
    }

    @Test(expected = IllegalArgumentException)
    void testUpdatePassNull() {
        qualityAssessmentPassService.update(null, QaProcessingStatus.UNKNOWN)
    }

    @Test
    void testUpdate() {
        assertEquals(QaProcessingStatus.UNKNOWN, processedBamFile.qualityAssessmentStatus)
        qualityAssessmentPassService.update(qualityAssessmentPass, QaProcessingStatus.NOT_STARTED)
        assertEquals(QaProcessingStatus.NOT_STARTED, processedBamFile.qualityAssessmentStatus)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmForDataProcessingPassNull() {
        qualityAssessmentPassService.realmForDataProcessing(null)
    }

    @Test
    void testRealmForDataProcessing() {
        realm = DomainFactory.createRealmDataProcessingDKFZ().save([flush:true])
        assertEquals(realm, qualityAssessmentPassService.realmForDataProcessing(qualityAssessmentPass))
    }

    @Test(expected = IllegalArgumentException)
    void testProjectPassNull() {
        qualityAssessmentPassService.project(null)
    }

    @Test
    void testProject() {
        assertEquals(project, qualityAssessmentPassService.project(qualityAssessmentPass))
    }

    @Test(expected = IllegalArgumentException)
    void testAllQualityAssessmentPassesBamFileNull() {
        qualityAssessmentPassService.allQualityAssessmentPasses(null)
    }

    @Test
    void testAllQualityAssessmentPasses() {
        assertEquals([qualityAssessmentPass], qualityAssessmentPassService.allQualityAssessmentPasses(processedBamFile))
        QualityAssessmentPass qualityAssessmentPass1 = new QualityAssessmentPass(
                        processedBamFile: processedBamFile,
                        identifier: 1,
                        description: 'QualtiyAssessmentPassDescription'
                        )
        assertNotNull(qualityAssessmentPass1.save([flush: true]))
        assertEquals([qualityAssessmentPass1, qualityAssessmentPass], qualityAssessmentPassService.allQualityAssessmentPasses(processedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testLatestQualityAssessmentPassFileNull() {
        qualityAssessmentPassService.latestQualityAssessmentPass(null)
    }

    @Test
    void testLatestQualityAssessmentPass() {
        assertEquals(qualityAssessmentPass, qualityAssessmentPassService.latestQualityAssessmentPass(processedBamFile))
        QualityAssessmentPass qualityAssessmentPass1 = new QualityAssessmentPass(
                        processedBamFile: processedBamFile,
                        identifier: 1,
                        description: 'QualtiyAssessmentPassDescription'
                        )
        assertNotNull(qualityAssessmentPass1.save([flush: true]))
        assertEquals(qualityAssessmentPass1, qualityAssessmentPassService.latestQualityAssessmentPass(processedBamFile))
    }

    @Test
    void testCreatePassQualityAssessmentStatusWrong() {
        assertNull(qualityAssessmentPassService.createPass())
    }

    @Test
    void testCreatePassBamTypeWrong() {
        processedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        processedBamFile.type = BamType.MDUP
        assertNull(qualityAssessmentPassService.createPass())
        processedBamFile.type = BamType.RMDUP
        assertNull(qualityAssessmentPassService.createPass())
    }

    @Test
    void testCreatePassWithdrawn() {
        processedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        processedBamFile.withdrawn = true
        assertNull(qualityAssessmentPassService.createPass())
    }

    @Test
    void testCreatePassWithoutFastqcFinished() {
        processedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        assertEquals(1, QualityAssessmentPass.list().size())
        assertNull(qualityAssessmentPassService.createPass())
    }

    @Test
    void testCreatePassWithFastqcFinished() {
        seqTrackService.setFastqcFinished(seqTrack)
        processedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        assertEquals(1, QualityAssessmentPass.list().size())

        FileType fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = "/sequence/"
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        DataFile dataFile = new DataFile(
            fileType: fileType,
            seqTrack: seqTrack)
        assertNotNull(dataFile.save(flush: true))

       FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile(
            contentUploaded: true,
            dataFile: dataFile)
        assertNotNull(fastqcProcessedFile.save(flush: true))

        FastqcBasicStatistics fastqcBasicStats = new FastqcBasicStatistics(
            fileType: "fileType",
            encoding: "encoding",
            fastqcProcessedFile: fastqcProcessedFile)
        assertNotNull(fastqcBasicStats.save(flush: true))

        assertNotNull(qualityAssessmentPassService.createPass())
    }

    @Test
    void testCreatePassWithTwoCandidates() {
        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: processedBamFile.alignmentPass,
                        fileExists: true,
                        type: BamType.SORTED,
                        qualityAssessmentStatus: QaProcessingStatus.NOT_STARTED,
                        fileOperationStatus: FileOperationStatus.DECLARED,
                        md5sum: null,
                        withdrawn: false,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedBamFile2.save([flush: true]))

        QualityAssessmentPass qualityAssessmentPass2 = new QualityAssessmentPass(
                        processedBamFile: processedBamFile2,
                        identifier: 0,
                        description: 'QualtiyAssessmentPassDescription'
                        )
        assertNotNull(qualityAssessmentPass2.save([flush: true]))

        seqTrackService.setFastqcFinished(seqTrack)
        processedBamFile.qualityAssessmentStatus = QaProcessingStatus.NOT_STARTED
        assertEquals(2, QualityAssessmentPass.list().size())

        FileType fileType = new FileType(
                        type: FileType.Type.SEQUENCE,
                        subType: "fastq",
                        vbpPath: "/sequence/",
                        signature: ".fastq"
                        )
        assertNotNull(fileType.save(flush: true))

        DataFile dataFile = new DataFile(
                        fileType: fileType,
                        seqTrack: seqTrack)
        assertNotNull(dataFile.save(flush: true))

        FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile(
                        contentUploaded: true,
                        dataFile: dataFile)
        assertNotNull(fastqcProcessedFile.save(flush: true))

        FastqcBasicStatistics fastqcBasicStats = new FastqcBasicStatistics(
                        fileType: "fileType",
                        encoding: "encoding",
                        fastqcProcessedFile: fastqcProcessedFile)
        assertNotNull(fastqcBasicStats.save(flush: true))

        assertNotNull(qualityAssessmentPassService.createPass())
    }

    private void setUpTestForAssertNumberOfReadsIsTheSameAsCalculatedWithFastqc(long fastqcReadCount, long bamReadCount) {
        final String FILE_TYPE = "fileType"
        final String FILE_ENCODING = "encoding"

        FileType fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = "/sequence/"
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        DataFile dataFile = new DataFile(
            fileType: fileType,
            seqTrack: seqTrack)
        assertNotNull(dataFile.save(flush: true))

       FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile(
            contentUploaded: true,
            dataFile: dataFile)
        assertNotNull(fastqcProcessedFile.save(flush: true))

        FastqcBasicStatistics fastqcBasicStats = new FastqcBasicStatistics(
            fileType: FILE_TYPE,
            encoding: FILE_ENCODING,
            totalSequences: (fastqcReadCount / 2),
            fastqcProcessedFile: fastqcProcessedFile)
        assertNotNull(fastqcBasicStats.save(flush: true))

        dataFile = new DataFile(
            fileType: fileType,
            seqTrack: seqTrack)
        assertNotNull(dataFile.save(flush: true))

        fastqcProcessedFile = new FastqcProcessedFile(
            contentUploaded: true,
            dataFile: dataFile)
        assertNotNull(fastqcProcessedFile.save(flush: true))

        fastqcBasicStats = new FastqcBasicStatistics(
            fileType: FILE_TYPE,
            encoding: FILE_ENCODING,
            totalSequences: (fastqcReadCount / 2),
            fastqcProcessedFile: fastqcProcessedFile)
        assertNotNull(fastqcBasicStats.save(flush: true))

        AbstractQualityAssessment qualityAssessmentStatistics = new OverallQualityAssessment(
            totalReadCounter: bamReadCount,
            qualityAssessmentPass: qualityAssessmentPass
        )
        assertNotNull(qualityAssessmentStatistics.save(flush: true))
    }

    @Test
    void TestAssertNumberOfReadsIsTheSameAsCalculatedWithFastqc() {
        setUpTestForAssertNumberOfReadsIsTheSameAsCalculatedWithFastqc(1000, 1000)
        qualityAssessmentPassService.assertNumberOfReadsIsTheSameAsCalculatedWithFastqc(qualityAssessmentPass)
    }

    @Test
    void TestAssertNumberOfReadsIsTheSameAsCalculatedWithFastqcNotTheSame() {
        setUpTestForAssertNumberOfReadsIsTheSameAsCalculatedWithFastqc(1000, 1002)
        shouldFail(QualityAssessmentException) {
            qualityAssessmentPassService.assertNumberOfReadsIsTheSameAsCalculatedWithFastqc(qualityAssessmentPass)
        }
    }
}
