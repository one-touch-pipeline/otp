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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.*

@Rollback
@Integration
class ExecutePanCanJobIntegrationTests implements RoddyRnaFactory {

    ExecutePanCanJob executePanCanJob

    RoddyBamFile roddyBamFile

    LsdfFilesService lsdfFilesService
    TestConfigService configService
    ReferenceGenomeService referenceGenomeService
    FileSystemService fileSystemService
    BedFileService bedFileService

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    void setupData() {
        executePanCanJob = new ExecutePanCanJob(
                lsdfFilesService: lsdfFilesService,
                referenceGenomeService: referenceGenomeService,
                fileSystemService: fileSystemService,
                bedFileService: bedFileService,
        )

        DomainFactory.createRoddyAlignableSeqTypes()

        configService.addOtpProperties(tmpDir.root.toPath())

        roddyBamFile = DomainFactory.createRoddyBamFile([
                md5sum                      : null,
                fileOperationStatus         : AbstractMergedBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        SessionUtils.metaClass.static.withNewSession = { Closure c -> c() }

        DomainFactory.createProcessingOptionBasePathReferenceGenome(new File(tmpDir.root, "reference_genomes").path)

        prepareDataFilesOnFileSystem(roddyBamFile)

        DomainFactory.createBedFile([referenceGenome: roddyBamFile.referenceGenome, libraryPreparationKit: roddyBamFile.mergingWorkPackage.libraryPreparationKit])
        CreateFileHelper.createFile(executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome, false))
        CreateFileHelper.createFile(executePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage, false))
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(BedFileService, executePanCanJob.bedFileService)
        TestCase.removeMetaClass(SessionUtils)
        configService.clean()
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_BamFileIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCValues(null)
        }.contains("roddyBamFile must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_BaseBamFileNotCorrect_ShouldFail() {
        setupData()
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        roddyBamFile.fileSize = roddyBamFile.finalBamFile.size()
        assert roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        prepareDataFilesOnFileSystem(roddyBamFile2)

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile2)
        }.contains(roddyBamFile.workBamFile.path)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_ExomeSeqType_AllFine() {
        setupData()
        executePanCanJob.bedFileService.metaClass.filePath = { BedFile bedFile ->
            return "BedFilePath"
        }

        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        roddyBamFile.mergingWorkPackage.seqType = exomeSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        List<String> expectedCommand = [
                "INDEX_PREFIX:${executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "GENOME_FA:${executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "CHROM_SIZES_FILE:${executePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage).absolutePath}",
                "possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:",
                "runFingerprinting:false",
                "TARGET_REGIONS_FILE:BedFilePath",
                "TARGETSIZE:1",
                "fastq_list:${fastqFilesAsString(roddyBamFile)}",
        ]

        List<String> actualCommand = executePanCanJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        assert expectedCommand == actualCommand
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_WholeGenomeSeqType_NoBaseBamFile_WithFingerPrinting_AllFine() {
        setupData()
        ReferenceGenome referenceGenome = roddyBamFile.referenceGenome
        referenceGenome.fingerPrintingFileName = "fingerprintingFile"
        assert referenceGenome.save(flush: true)

        File fingerPrintingFile = executePanCanJob.referenceGenomeService.fingerPrintingFile(roddyBamFile.referenceGenome, false)
        CreateFileHelper.createFile(fingerPrintingFile)

        List<String> expectedCommand = [
                "INDEX_PREFIX:${executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "GENOME_FA:${executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "CHROM_SIZES_FILE:${executePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage).absolutePath}",
                "possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:",
                "runFingerprinting:true",
                "fingerprintingSitesFile:${fingerPrintingFile}",
                "fastq_list:${fastqFilesAsString(roddyBamFile)}",
        ]

        List<String> actualCommand = executePanCanJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        assert expectedCommand == actualCommand
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_WholeGenomeSeqType_NoBaseBamFile_AllFine() {
        setupData()

        List<String> expectedCommand = [
                "INDEX_PREFIX:${executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "GENOME_FA:${executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "CHROM_SIZES_FILE:${executePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage).absolutePath}",
                "possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:",
                "runFingerprinting:false",
                "fastq_list:${fastqFilesAsString(roddyBamFile)}",
        ]

        List<String> actualCommand = executePanCanJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        assert expectedCommand == actualCommand
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_WholeGenomeSeqType_WithBaseBamFile_AllFine() {
        setupData()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = HelperUtils.randomMd5sum
        roddyBamFile.fileSize = roddyBamFile.workBaiFile.size()
        assert roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        prepareDataFilesOnFileSystem(roddyBamFile2)

        List<String> expectedCommand = [
                "INDEX_PREFIX:${executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile2.referenceGenome).absolutePath}",
                "GENOME_FA:${executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome).absolutePath}",
                "CHROM_SIZES_FILE:${executePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile2.mergingWorkPackage).absolutePath}",
                "possibleControlSampleNamePrefixes:${roddyBamFile2.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:",
                "runFingerprinting:false",
                "fastq_list:${fastqFilesAsString(roddyBamFile2)}",
                "bam:${roddyBamFile.workBamFile.path}",
        ]

        List<String> actualCommand = executePanCanJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile2)

        assert expectedCommand == actualCommand
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_MustAlwaysReturnAnEmptyString() {
        setupData()
        assert "" == executePanCanJob.prepareAndReturnWorkflowSpecificParameter(null)
        assert "" == executePanCanJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)
    }

    @Test
    void testGetFilesToMerge_BamFileIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.getFilesToMerge(null)
        }.contains("roddyBamFile must not be null")
    }

    @Test
    void testGetFilesToMerge_WrongCountOfDataFileForSeqTrack_ShouldFail() {
        setupData()
        DataFile dataFile = DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks as List).first()
        dataFile.delete(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.getFilesToMerge(roddyBamFile)
        }.contains("seqTrack.seqType.libraryLayout.mateCount == dataFiles.size()")
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @Test
    void testGetFilesToMerge_DataFilesAreNotOnFileSystem_ShouldFail() {
        setupData()

        DataFile dataFile1 = DataFile.findBySeqTrackInListAndMateNumber(roddyBamFile.seqTracks as List, 1)
        File file1 = new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFile1))
        assert file1.delete()

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.getFilesToMerge(roddyBamFile)
        }.contains(file1.path)
    }

    @Test
    void testGetFilesToMerge_DataFileHasWrongFileSizeInDatabase_ShouldFail() {
        setupData()
        DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks as List).each {
            it.fileSize = 12345
            assert it.save(flush: true)
        }

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.getFilesToMerge(roddyBamFile)
        }.contains("1234")
    }

    @Test
    void testGetFilesToMerge_AllFine() {
        setupData()
        List<File> expectedDataFileList = DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks as List).sort {
            it.fileName
        }.collect { new File(lsdfFilesService.getFileViewByPidPath(it)) }
        List<File> actualDataFileList = executePanCanJob.getFilesToMerge(roddyBamFile)

        assert expectedDataFileList == actualDataFileList
    }

    @Test
    void testWorkflowSpecificValidation_workMergedQATargetExtractJsonFileDoesNotExist_ShouldFail() {
        setupData()
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        DomainFactory.changeSeqType(roddyBamFile, exomeSeqType)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.workflowSpecificValidation(roddyBamFile)
        }.contains(roddyBamFile.workMergedQATargetExtractJsonFile.path)
    }

    @Test
    void testWorkflowSpecificValidation_AllFine() {
        setupData()
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        DomainFactory.changeSeqType(roddyBamFile, exomeSeqType)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        assert CreateFileHelper.createFile(roddyBamFile.workMergedQATargetExtractJsonFile)

        executePanCanJob.workflowSpecificValidation(roddyBamFile)
    }

    @Test
    void testWorkflowSpecificValidation_RnaBamFile_AllFine() {
        setupData()
        RoddyBamFile roddyBamFile = RoddyRnaFactory.super.createBamFile()
        assert roddyBamFile.project.save(flush: true)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        executePanCanJob.workflowSpecificValidation(roddyBamFile)
    }

    @Test
    void testWorkflowSpecificValidation_RnaBamFile_ChimericFileDoesNotExist() {
        setupData()
        RoddyBamFile roddyBamFile = RoddyRnaFactory.super.createBamFile()
        assert roddyBamFile.project.save(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.workflowSpecificValidation(roddyBamFile)
        }.contains(roddyBamFile.correspondingWorkChimericBamFile.path)
    }

    private void prepareDataFilesOnFileSystem(RoddyBamFile bamFile) {
        assert 1 == bamFile.seqTracks.size()
        SeqTrack seqTrack = bamFile.seqTracks.iterator()[0]
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack, [sort: 'mateNumber'])
        assert 2 == dataFiles.size()
        File dataFile1File = new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[0]))
        createFileAndAddFileSize(dataFile1File, dataFiles[0])
        File dataFile2File = new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[1]))
        createFileAndAddFileSize(dataFile2File, dataFiles[1])
    }

    private void createFileAndAddFileSize(File file, DataFile dataFile) {
        CreateFileHelper.createFile(file)
        dataFile.fileSize = file.length()
        assert dataFile.save(flush: true)
    }

    private String fastqFilesAsString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.seqTracks.collectMany { SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                lsdfFilesService.getFileViewByPidPath(dataFile) as File
            }
        }.join(';')
    }
}
