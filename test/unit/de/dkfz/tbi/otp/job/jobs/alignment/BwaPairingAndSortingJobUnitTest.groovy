package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.dataprocessing.ProcessedSaiFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedSaiFileService
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.TestData
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test

@TestMixin(GrailsUnitTestMixin)
@Build([AlignmentPass, ProcessedSaiFile, ReferenceGenome])
class BwaPairingAndSortingJobUnitTest {

    static final String FILE_MATE1 = "file_R1_abc.fastq.gz"
    static final String FILE_MATE2 = "file_R2_abc.fastq.gz"
    static final String SAI_FILE1 = "${FILE_MATE1}.sai"
    static final String SAI_FILE2 = "${FILE_MATE2}.sai"


    BwaPairingAndSortingJob bwaPairingAndSortingJob

    AlignmentPass alignmentPass

    ProcessedSaiFile processedSaiFile1

    ProcessedSaiFile processedSaiFile2

    @Before
    void setUp() {
        bwaPairingAndSortingJob = new BwaPairingAndSortingJob()
        bwaPairingAndSortingJob.processedSaiFileService = new ProcessedSaiFileService()
        bwaPairingAndSortingJob.processedSaiFileService.metaClass.getFilePath = { ProcessedSaiFile saiFile ->
            return "${saiFile.dataFile.fileName}.sai"
        }
        bwaPairingAndSortingJob.lsdfFilesService = new LsdfFilesService()
        bwaPairingAndSortingJob.lsdfFilesService.metaClass.getFileViewByPidPath = { DataFile file, Sequence sequence = null ->
            file.fileName
        }

        alignmentPass = TestData.createAndSaveAlignmentPass()
        DataFile dataFile1 = DomainFactory.createDataFile(fileName: FILE_MATE1, vbpFileName: FILE_MATE1, mateNumber: 1)
        DataFile dataFile2 = DomainFactory.createDataFile(fileName: FILE_MATE2, vbpFileName: FILE_MATE2, mateNumber: 2)
        processedSaiFile1 = ProcessedSaiFile.build(alignmentPass: alignmentPass, dataFile: dataFile1)
        processedSaiFile2 = ProcessedSaiFile.build(alignmentPass: alignmentPass, dataFile: dataFile2)
    }

    @After
    void tearDown() {
        alignmentPass = null
        processedSaiFile1 = null
        processedSaiFile2 = null
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = null
        TestCase.removeMetaClass(LsdfFilesService, bwaPairingAndSortingJob.lsdfFilesService)
        TestCase.removeMetaClass(ProcessedSaiFileService, bwaPairingAndSortingJob.processedSaiFileService)
        bwaPairingAndSortingJob = null
    }


    @Test
    void testCreateSequenceAndSaiFiles_correctOrder() {
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = { AlignmentPass pass, boolean exists ->
            return [processedSaiFile1, processedSaiFile2]
        }

        String ret = bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass)
        String[] files = ret.split(" ")
        assert SAI_FILE1 == files[0]
        assert SAI_FILE2 == files[1]
        assert FILE_MATE1 == files[2]
        assert FILE_MATE2 == files[3]
    }

    @Test
    void testCreateSequenceAndSaiFiles_wrongOrder() {
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = { AlignmentPass pass, boolean exists ->
            return [processedSaiFile2, processedSaiFile1]
        }

        String ret = bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass)
        String[] files = ret.split(" ")
        assert SAI_FILE1 == files[0]
        assert SAI_FILE2 == files[1]
        assert FILE_MATE1 == files[2]
        assert FILE_MATE2 == files[3]
    }

    @Test
    void testCreateSequenceAndSaiFiles_tooFew() {
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = { AlignmentPass pass, boolean exists ->
            return [processedSaiFile1]
        }

        shouldFail(ProcessingException) {
            String ret = bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass)
        }
    }

    @Test
    void testCreateSequenceAndSaiFiles_tooMany() {
        ProcessedSaiFile processedSaiFile3 = ProcessedSaiFile.build(alignmentPass: alignmentPass, dataFile: DomainFactory.createDataFile())
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = { AlignmentPass pass, boolean exists ->
            return [processedSaiFile1, processedSaiFile2, processedSaiFile3]
        }

        shouldFail(ProcessingException) {
            bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass)
        }
    }

    @Test
    void testCreateSequenceAndSaiFiles_illegalFileName() {
        processedSaiFile2.dataFile.fileName = "file_R3_abc.fastq.gz"
        shouldFail RuntimeException, { bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass) }
    }

    @Test
    void testCreateSequenceAndSaiFiles_illegalVbpFileName() {
        processedSaiFile2.dataFile.vbpFileName = "file_R3_abc.fastq.gz"
        shouldFail RuntimeException, { bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass) }
    }
}
