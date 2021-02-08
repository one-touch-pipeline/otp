/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.jobs.fastqc

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ClusterJobSchedulerService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.FastqcUploadService
import de.dkfz.tbi.otp.utils.*

@Rollback
@Integration
class FastqcJobIntegrationTests {

    @Autowired
    ApplicationContext applicationContext

    FastqcJob fastqcJob

    SeqTrack seqTrack
    DataFile dataFile
    FastqImportInstance fastqImportInstance

    File testDirectory

    void setupData() {
        testDirectory = TestCase.createEmptyTestDirectory()

        seqTrack = DomainFactory.createSeqTrack()
        fastqImportInstance = DomainFactory.createFastqImportInstance()
        dataFile = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run, fastqImportInstance: fastqImportInstance, initialDirectory: "${testDirectory.path}/${seqTrack.run.name}"])

        fastqcJob = applicationContext.getBean('fastqcJob')
        fastqcJob.processingStep = DomainFactory.createAndSaveProcessingStep(FastqcJob.toString(), seqTrack)

        ProcessingOptionService processingOptionService = new ProcessingOptionService()
        processingOptionService.createOrUpdate(ProcessingOption.OptionName.COMMAND_FASTQC, "fastqc-0.10.1")

        WaitingFileUtils.metaClass.static.waitUntilExists = { File file -> true }
        LocalShellHelper.metaClass.static.executeAndAssertExitCodeAndErrorOutAndReturnStdout = { String cmd ->
            return "OK"
        }

        fastqcJob.lsdfFilesService.metaClass.ensureFileIsReadableAndNotEmpty = { File file ->
            return true
        }

        fastqcJob.lsdfFilesService.metaClass.ensureDirIsReadableAndNotEmpty = { File file ->
            return true
        }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ClusterJobSchedulerService, fastqcJob.clusterJobSchedulerService)
        TestCase.removeMetaClass(RemoteShellHelper, fastqcJob.remoteShellHelper)
        TestCase.removeMetaClass(LsdfFilesService, fastqcJob.lsdfFilesService)
        TestCase.removeMetaClass(FastqcUploadService, fastqcJob.fastqcUploadService)
        TestCase.removeMetaClass(FastqcJob, fastqcJob)
        WaitingFileUtils.metaClass = null
        LocalShellHelper.metaClass = null

        seqTrack = null
        dataFile = null
    }


    @Test
    void testMaybeSubmit_FastQcResultsNotAvailable_executesFastQcCommand() {
        setupData()
        fastqcJob.clusterJobSchedulerService.metaClass.executeJob = { Realm inputRealm, String inputCommand ->
            assert inputCommand.contains("fastqc-0.10.1")
            assert !inputCommand.contains("bzip2 --decompress --keep")
            assert !inputCommand.contains("rm -f")
            return 'pbsJobId'
        }

        fastqcJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm inputRealm, String command ->
            assert command.contains("umask 027; mkdir -p -m 2750")
            assert !command.contains("cp ")
            return new ProcessOutput('', '', 0)
        }

        fastqcJob.maybeSubmit()
    }

    @Test
    void testMaybeSubmit_FastQcResultsNotAvailableAndDatafileIsBzip_executesFastQcCommandForBzip() {
        setupData()
        dataFile.vbpFileName = dataFile.fileName = 'file.bz2'
        fastqcJob.clusterJobSchedulerService.metaClass.executeJob = { Realm inputRealm, String inputCommand ->
            assert inputCommand.contains("fastqc-0.10.1")
            assert inputCommand =~ "\\{ bzip2 --decompress ; } < .*/file.bz2 > .*/file"
            assert inputCommand.contains("rm -f")
            return 'pbsJobId'
        }

        fastqcJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm inputRealm, String command ->
            assert command.contains("umask 027; mkdir -p -m 2750")
            assert !command.contains("cp ")
            return new ProcessOutput('', '', 0)
        }

        fastqcJob.maybeSubmit()
    }

    @Test
    void testMaybeSubmit_FastQcResultsAvailable_executesCopyCommand() {
        setupData()
        long nReads = 100
        String sequenceLength = "90"

        File fastqcFile = CreateFileHelper.createFile(
                new File(fastqcJob.fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile).toString())
        )

        dataFile.nReads = nReads
        dataFile.sequenceLength = sequenceLength
        assert dataFile.save(flush: true)

        fastqcJob.clusterJobSchedulerService.metaClass.executeJob = { Realm inputRealm, String inputCommand ->
            assert false : "this method should not be reached"
        }

        fastqcJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm inputRealm, String command ->
            assert command.contains("umask 027; mkdir -p -m 2750") || command.contains("cp ")
            return new ProcessOutput('', '', 0)
        }

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }

        try {
            fastqcJob.maybeSubmit()
            assert seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED
        } finally {
            fastqcFile.delete()
            TestCase.cleanTestDirectory()
        }
    }


    @Test
    void testValidate_FastqcAreCreatedByClusterJob_shallBeUploadToDB() {
        setupData()
        long nReads = 100
        String sequenceLength = "90"

        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        dataFile.nReads = nReads
        dataFile.sequenceLength = sequenceLength
        assert dataFile.save(flush: true)

        assert fastqcProcessedFile.contentUploaded == false

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()

        assert fastqcProcessedFile.contentUploaded == true
        assert seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED
    }

    @Test
    void testValidate_totalSequencesIsSameForAllDataFiles_ShouldPassValidation() {
        setupData()
        long nReads = 100
        String sequenceLength = "90"

        dataFile.nReads = nReads
        dataFile.sequenceLength = sequenceLength
        dataFile.save(flush: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack,
                                                           project: seqTrack.project,
                                                           run: seqTrack.run,
                                                           fastqImportInstance: fastqImportInstance,
                                                           nReads: nReads,
                                                           sequenceLength: sequenceLength,
                                                           fileType: dataFile.fileType,])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()
    }

    @Test
    void testValidate_totalSequencesIsDifferentForAllDataFiles_ShouldFail() {
        setupData()
        long nReads1 = 100
        long nReads2 = 105
        String sequenceLength = "90"

        dataFile.nReads = nReads1
        dataFile.sequenceLength = sequenceLength
        dataFile.save(flush: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack,
                                                           project: seqTrack.project,
                                                           run: seqTrack.run,
                                                           fastqImportInstance: fastqImportInstance,
                                                           nReads: nReads2,
                                                           sequenceLength: sequenceLength,])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }

        TestCase.shouldFail(AssertionError) {
            fastqcJob.validate()
        }
    }

    @Test
    void testValidate_sequenceLengthIsIntegerValueAndIsSameForAllDataFiles_ShouldPassValidation() {
        setupData()
        String sequenceLength = "100"
        long nReads = 100

        dataFile.sequenceLength = sequenceLength
        dataFile.nReads = nReads
        dataFile.save(flush: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack,
                                                           project: seqTrack.project,
                                                           run: seqTrack.run,
                                                           fastqImportInstance: fastqImportInstance,
                                                           sequenceLength: sequenceLength,
                                                           nReads: nReads,
                                                           fileType: dataFile.fileType,])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()
    }

    @Test
    void testValidate_sequenceLengthIsRangeValueAndIsDifferentForAllDataFiles_ShouldPassValidation() {
        setupData()
        String sequenceLength1 = "0-50"
        String sequenceLength2 = "50-100"
        long nReads = 100

        dataFile.sequenceLength = sequenceLength1
        dataFile.nReads = nReads
        dataFile.save(flush: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack,
                                                           project: seqTrack.project,
                                                           run: seqTrack.run,
                                                           fastqImportInstance: fastqImportInstance,
                                                           sequenceLength: sequenceLength2,
                                                           nReads: nReads,
                                                           fileType: dataFile.fileType,])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()
    }

    @Test
    void testValidate_sequenceLengthIsDifferentForAllDataFiles_ShouldPassValidation() {
        setupData()
        String sequenceLength1 = "50"
        String sequenceLength2 = "100"
        long nReads = 100

        dataFile.sequenceLength = sequenceLength1
        dataFile.nReads = nReads
        dataFile.save(flush: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack,
                                                           project: seqTrack.project,
                                                           run: seqTrack.run,
                                                           fastqImportInstance: fastqImportInstance,
                                                           sequenceLength: sequenceLength2,
                                                           nReads: nReads,
                                                           fileType: dataFile.fileType,])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()
    }
}
