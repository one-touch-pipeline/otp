package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*
import org.junit.rules.*
import org.springframework.beans.factory.annotation.*

class ExecutePanCanJobTests {

    @Autowired
    ExecutePanCanJob executePanCanJob

    LsdfFilesService lsdfFilesService

    RoddyBamFile roddyBamFile
    Realm dataProcessingRealm
    Realm dataManagementRealm

    TestConfigService configService

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        DomainFactory.createRoddyAlignableSeqTypes()

        configService = new TestConfigService([
                'otp.root.path': tmpDir.root.path,
                'otp.processing.root.path': tmpDir.root.path
        ])

        roddyBamFile = DomainFactory.createRoddyBamFile([
                md5sum: null,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])

        executePanCanJob.lsdfFilesService.configService = configService

        dataProcessingRealm = DomainFactory.createRealmDataProcessing([name: roddyBamFile.project.realmName])
        dataManagementRealm = DomainFactory.createRealmDataManagement([name: roddyBamFile.project.realmName])

        DomainFactory.createProcessingOptionBasePathReferenceGenome(new File(tmpDir.root, "reference_genomes").path)

        prepareDataFilesOnFileSystem(roddyBamFile)

        DomainFactory.createBedFile([referenceGenome: roddyBamFile.referenceGenome, libraryPreparationKit: roddyBamFile.mergingWorkPackage.libraryPreparationKit])
        CreateFileHelper.createFile(executePanCanJob.referenceGenomeService.fastaFilePath(roddyBamFile.referenceGenome, false))
        CreateFileHelper.createFile(executePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage, false))
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(BedFileService, executePanCanJob.bedFileService)
        configService.clean()
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
        executePanCanJob.bedFileService.metaClass.filePath = { BedFile bedFile ->
            return "BedFilePath"
        }

        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        roddyBamFile.mergingWorkPackage.seqType = exomeSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        List<String>  expectedCommand = [
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
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = HelperUtils.randomMd5sum
        roddyBamFile.fileSize = roddyBamFile.getWorkBaiFile().size()
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
        }.contains("LibraryLayout.valueOf(seqTrack.seqType.libraryLayout).mateCount == dataFiles.size()")
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


    @Test
    void testWorkflowSpecificValidation_RnaBamFile_AllFine() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([:], RnaRoddyBamFile)
        roddyBamFile.project.realmName = dataManagementRealm.name
        assert roddyBamFile.project.save(flush: true)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        executePanCanJob.workflowSpecificValidation(roddyBamFile)
    }

    @Test
    void testWorkflowSpecificValidation_RnaBamFile_ChimericFileDoesNotExist() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([:], RnaRoddyBamFile)
        roddyBamFile.project.realmName = dataManagementRealm.name
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
        return roddyBamFileToUse.seqTracks.collect {SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                lsdfFilesService.getFileViewByPidPath(dataFile) as File
            }
        }.flatten().join(';')
    }
}
