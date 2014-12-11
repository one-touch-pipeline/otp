package de.dkfz.tbi.otp.job.jobs.alignment

import org.junit.Test

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.dataprocessing.ProcessedSaiFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedSaiFileService
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService


@Build([AlignmentPass, ProcessedSaiFile])
class BwaPairingAndSortingJobUnitTest {

    static final String FILE_READ1 = "file_R1_abc.fastq.gz"
    static final String FILE_READ2 = "file_R2_abc.fastq.gz"
    static final String SAI_FILE1 = "${FILE_READ1}.sai"
    static final String SAI_FILE2 = "${FILE_READ2}.sai"



    BwaPairingAndSortingJob bwaPairingAndSortingJob

    AlignmentPass alignmentPass

    ProcessedSaiFile processedSaiFile1

    ProcessedSaiFile processedSaiFile2



    void setUp() {
        bwaPairingAndSortingJob = new BwaPairingAndSortingJob()
        bwaPairingAndSortingJob.processedSaiFileService = [
            getFilePath: {ProcessedSaiFile saiFile ->
                return "${saiFile.dataFile.fileName}.sai"
            }
            ] as ProcessedSaiFileService
        bwaPairingAndSortingJob.lsdfFilesService = [
            getFileViewByPidPath: {DataFile file, Sequence sequence = null ->
                file.fileName
            }
            ] as LsdfFilesService

        alignmentPass = AlignmentPass.build()
        DataFile dataFile1 = DataFile.build(fileName: FILE_READ1, vbpFileName: FILE_READ1, readNumber: 1)
        DataFile dataFile2 = DataFile.build(fileName: FILE_READ2, vbpFileName: FILE_READ2, readNumber: 2)
        processedSaiFile1 = ProcessedSaiFile.build(alignmentPass: alignmentPass, dataFile: dataFile1)
        processedSaiFile2 = ProcessedSaiFile.build(alignmentPass: alignmentPass, dataFile: dataFile2)
    }



    void testCreateSequenceAndSaiFiles_correctOrder() {
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = {AlignmentPass pass, boolean exists ->
            return [processedSaiFile1, processedSaiFile2]
        }

        String ret = bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass)
        String[] files = ret.split(" ")
        assert SAI_FILE1 == files[0]
        assert SAI_FILE2 == files[1]
        assert FILE_READ1 == files[2]
        assert FILE_READ2 == files[3]
    }

    void testCreateSequenceAndSaiFiles_wrongOrder() {
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = {AlignmentPass pass, boolean exists ->
            return [processedSaiFile2, processedSaiFile1]
        }

        String ret = bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass)
        String[] files = ret.split(" ")
        assert SAI_FILE1 == files[0]
        assert SAI_FILE2 == files[1]
        assert FILE_READ1 == files[2]
        assert FILE_READ2 == files[3]
    }

    void testCreateSequenceAndSaiFiles_tooFew() {
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = {AlignmentPass pass, boolean exists ->
            return [processedSaiFile1]
        }

        shouldFail(ProcessingException) {
            String ret = bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass)
        }
    }

    void testCreateSequenceAndSaiFiles_tooMany() {
        ProcessedSaiFile processedSaiFile3 = ProcessedSaiFile.build(alignmentPass: alignmentPass)
        ProcessedSaiFile.metaClass.static.findAllByAlignmentPassAndFileExists = {AlignmentPass pass, boolean exists ->
            return [processedSaiFile1, processedSaiFile2, processedSaiFile3]
        }

        shouldFail(ProcessingException) {
            String ret = bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass)
        }
    }

    @Test
    void testCreateSequenceAndSaiFiles_illegalFileName() {
        processedSaiFile2.dataFile.fileName = "file_R3_abc.fastq.gz"
        shouldFail { bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass) }
    }

    @Test
    void testCreateSequenceAndSaiFiles_illegalVbpFileName() {
        processedSaiFile2.dataFile.vbpFileName = "file_R3_abc.fastq.gz"
        shouldFail { bwaPairingAndSortingJob.createSequenceAndSaiFiles(alignmentPass) }
    }
}
