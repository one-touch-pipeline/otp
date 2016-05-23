package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.HelperUtils
import grails.plugin.springsecurity.acl.AclUtilService
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired

class ExecutePanCanJobTests {

    @Autowired
    ExecutePanCanJob executePanCanJob

    LsdfFilesService lsdfFilesService

    RoddyBamFile roddyBamFile

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        DomainFactory.createAlignableSeqTypes()

        roddyBamFile = DomainFactory.createRoddyBamFile([
                md5sum: null,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        DomainFactory.createRealmDataProcessing(tmpDir.root, [name: roddyBamFile.project.realmName])
        DomainFactory.createRealmDataManagement(tmpDir.root, [name: roddyBamFile.project.realmName])

        prepareDataFilesOnFileSystem(roddyBamFile)

        DomainFactory.createBedFile([referenceGenome: roddyBamFile.referenceGenome, libraryPreparationKit: roddyBamFile.mergingWorkPackage.libraryPreparationKit])
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(BedFileService, executePanCanJob.bedFileService)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_BamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCValues(null)
        }.contains("roddyBamFile must not be null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_BaseBamFileNotCorrect_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        roddyBamFile.fileSize = roddyBamFile.getFinalBamFile().size()
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
        executePanCanJob.bedFileService.metaClass.filePath = { Realm realm, BedFile bedFile ->
            return "BedFilePath"
        }

        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        roddyBamFile.mergingWorkPackage.seqType = exomeSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        String expectedCommand =  """\
,fastq_list:${fastqFilesAsString(roddyBamFile)},\
TARGET_REGIONS_FILE:BedFilePath,\
TARGETSIZE:1,\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}\
"""
        String actualCommand = executePanCanJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        assert expectedCommand == actualCommand
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_WholeGenomeSeqType_NoBaseBamFile_AllFine() {

        String expectedCommand = """\
,fastq_list:${fastqFilesAsString(roddyBamFile)},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}\
"""
        String actualCommand = executePanCanJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile)

        assert expectedCommand == actualCommand
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCValues_WholeGenomeSeqType_WithBaseBamFile_AllFine() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        roddyBamFile.fileSize = roddyBamFile.getWorkBaiFile().size()
        assert roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        prepareDataFilesOnFileSystem(roddyBamFile2)

        String expectedCommand = """\
,fastq_list:${fastqFilesAsString(roddyBamFile2)},\
bam:${roddyBamFile.workBamFile.path},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}\
"""

        String actualCommand = executePanCanJob.prepareAndReturnWorkflowSpecificCValues(roddyBamFile2)

        assert expectedCommand == actualCommand
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificParameter_MustAlwaysReturnAnEmptyString() {
        assert "" == executePanCanJob.prepareAndReturnWorkflowSpecificParameter(null)
        assert "" == executePanCanJob.prepareAndReturnWorkflowSpecificParameter(roddyBamFile)
    }


    @Test
    void testGetFilesToMerge_BamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.getFilesToMerge(null)
        }.contains("roddyBamFile must not be null")
    }


    @Test
    void testGetFilesToMerge_WrongCountOfDataFileForSeqTrack_ShouldFail() {
        DataFile dataFile = DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks).first()
        dataFile.delete(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.getFilesToMerge(roddyBamFile)
        }.contains("2 == dataFiles.size()")
    }


    @Test
    void testGetFilesToMerge_DataFilesAreNotOnFileSystem_ShouldFail() {

        DataFile dataFile1 = DataFile.findBySeqTrackInListAndMateNumber(roddyBamFile.seqTracks, 1)
        File file1 = new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFile1))
        assert file1.delete()

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.getFilesToMerge(roddyBamFile)
        }.contains(file1.path)
    }


    @Test
    void testGetFilesToMerge_DataFileHasWrongFileSizeInDatabase_ShouldFail() {
        DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks).each {
            it.fileSize = 12345
            assert it.save(flush: true)
        }

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.getFilesToMerge(roddyBamFile)
        }.contains("1234")
    }


    @Test
    void testGetFilesToMerge_AllFine() {
        List<File> expectedDataFileList = DataFile.findAllBySeqTrackInList(roddyBamFile.seqTracks).sort { it.fileName }.collect { new File(lsdfFilesService.getFileViewByPidPath(it)) }
        List<File> actualDataFileList = executePanCanJob.getFilesToMerge(roddyBamFile)

        assert expectedDataFileList == actualDataFileList
    }


    @Test
    void testWorkflowSpecificValidation_workMergedQATargetExtractJsonFileDoesNotExist_ShouldFail() {
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        DomainFactory.changeSeqType(roddyBamFile, exomeSeqType)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.workflowSpecificValidation(roddyBamFile)
        }.contains(roddyBamFile.workMergedQATargetExtractJsonFile.path)

    }


    @Test
    void testWorkflowSpecificValidation_AllFine() {
        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        DomainFactory.changeSeqType(roddyBamFile, exomeSeqType)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        assert CreateFileHelper.createFile(roddyBamFile.getWorkMergedQATargetExtractJsonFile())

        executePanCanJob.workflowSpecificValidation(roddyBamFile)
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
        return roddyBamFileToUse.seqTracks.collect {SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                lsdfFilesService.getFileViewByPidPath(dataFile) as File
            }
        }.flatten().join(';')
    }
}
