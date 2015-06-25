package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired


class ExecutePanCanJobTests {

    @Autowired
    ExecutePanCanJob executePanCanJob

    LsdfFilesService lsdfFilesService

    Realm dataProcessing
    Realm dataManagement
    RoddyBamFile roddyBamFile
    SeqTrack seqTrack

    File referenceGenomeDir
    File referenceGenomeFile
    File configFile
    File roddyPath
    File roddyCommand
    String roddyVersion
    File roddyBaseConfigsPath
    File roddyApplicationIni
    File chromosomeSizeFiles
    File processingRootPath
    File dataFile1File
    File dataFile2File

    @Before
    void setUp() {
        DomainFactory.createAlignableSeqTypes()

        roddyBamFile = DomainFactory.createRoddyBamFile([md5sum: null, fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED])
        dataProcessing = Realm.build(name: roddyBamFile.project.realmName, operationType: Realm.OperationType.DATA_PROCESSING)
        dataManagement = Realm.build(name: roddyBamFile.project.realmName, operationType: Realm.OperationType.DATA_MANAGEMENT)

        processingRootPath = dataProcessing.processingRootPath as File

        DomainFactory.createRoddyProcessingOptions(processingRootPath)

        roddyPath = ProcessingOption.findByName("roddyPath").value as File
        roddyCommand = new File(roddyPath, 'roddy.sh')
        roddyVersion = ProcessingOption.findByName("roddyVersion").value
        roddyBaseConfigsPath = ProcessingOption.findByName("roddyBaseConfigsPath").value as File
        assert roddyBaseConfigsPath.mkdirs()
        roddyApplicationIni = ProcessingOption.findByName("roddyApplicationIni").value as File
        roddyApplicationIni.text = "Some Text"

        seqTrack = roddyBamFile.seqTracks.iterator()[0]
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack, [sort: 'readNumber'])
        assert 2 == dataFiles.size()
        dataFile1File = new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[0]))
        createFileAndAddFileSize(dataFile1File, dataFiles[0])
        dataFile2File = new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[1]))
        createFileAndAddFileSize(dataFile2File, dataFiles[1])

        referenceGenomeDir = new File("${processingRootPath}/reference_genomes/${roddyBamFile.referenceGenome.path}")
        assert referenceGenomeDir.mkdirs()
        referenceGenomeFile = new File(referenceGenomeDir, "${roddyBamFile.referenceGenome.fileNamePrefix}.fa")
        CreateFileHelper.createFile(referenceGenomeFile)
        configFile = new File(roddyBamFile.config.configFilePath)
        CreateFileHelper.createFile(configFile)

        chromosomeSizeFiles = executePanCanJob.referenceGenomeService.pathToChromosomeSizeFilesPerReference(roddyBamFile.project, roddyBamFile.referenceGenome) as File
        assert chromosomeSizeFiles.mkdirs()

        roddyBamFile.workPackage.metaClass.findMergeableSeqTracks = { -> SeqTrack.list() }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecutePanCanJob, executePanCanJob)
        TestCase.removeMetaClass(ReferenceGenomeService, executePanCanJob.referenceGenomeService)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, executePanCanJob.executeRoddyCommandService)
        TestCase.removeMetaClass(LsdfFilesService, executePanCanJob.lsdfFilesService)

        assert new File(dataManagement.rootPath).deleteDir()
        assert new File(dataManagement.processingRootPath).deleteDir()
        assert roddyBaseConfigsPath.deleteDir()

        processingRootPath = null
        roddyPath = null
        roddyCommand = null
        roddyVersion = null
        roddyBaseConfigsPath = null
        roddyApplicationIni = null
        chromosomeSizeFiles = null
        dataProcessing = null
        dataManagement = null
        seqTrack = null
        roddyBamFile = null
        configFile = null
        dataFile1File = null
        dataFile2File = null
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RoddyBamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(null, dataManagement)
        }.contains("roddyResult must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RealmIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, null)
        }.contains("realm must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_PathToReferenceGenomeIsNull_ShouldFail() {
        executePanCanJob.referenceGenomeService.metaClass.fastaFilePath = { Project project, ReferenceGenome referenceGenome ->
            return null
        }

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains("Path to the reference genome file is null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_PathToChromosomeSizeFilesIsNull_ShouldFail() {
        executePanCanJob.referenceGenomeService.metaClass.pathToChromosomeSizeFilesPerReference = { Project project, ReferenceGenome referenceGenome ->
            return null
        }

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains("Path to the chromosome size files is null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsButIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"

        assert roddyBamFile.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        SeqTrack seqTrack = roddyBamFile2.seqTracks.iterator()[0]
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        assert 2 == dataFiles.size()
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[0])), dataFiles[0])
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[1])), dataFiles[1])

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile2, dataManagement)
        }.contains(roddyBamFile.finalBamFile.path)

    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_DataFilesAreNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        DataFile.findAllBySeqTrack(seqTrack).each {
            assert new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(it)).delete()
        }
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(dataFile1File.path)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ConfigFileIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        assert configFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(roddyBamFile.config.configFilePath)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ReferenceGenomeIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        assert referenceGenomeFile.delete()
        assert TestCase.shouldFail(RuntimeException) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(referenceGenomeFile.path)

    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ChromosomeSizeDirDoesNotExist_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }
        assert chromosomeSizeFiles.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(chromosomeSizeFiles.path)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_NoBaseBamFileExists() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        String viewByPid = roddyBamFile.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path
        String fastqFilesAsString = roddyBamFile.seqTracks.collect {SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                lsdfFilesService.getFileViewByPidPath(dataFile) as File
            }
        }.flatten().join(';')

        String expectedCmd =  """\
cd /tmp \
&& sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.externalScriptVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.externalScriptVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPid},${roddyBamFile.tmpRoddyDirectory} \
--cvalues="fastq_list:${fastqFilesAsString},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile.path},\
CHROM_SIZES_FILE:${chromosomeSizeFiles},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}"\
"""

        String actualCmd = executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsAlready() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = DomainFactory.DEFAULT_MD5_SUM
        assert roddyBamFile.save(flush: true)

        CreateFileHelper.createFile(roddyBamFile.finalBamFile)
        roddyBamFile.fileSize = roddyBamFile.finalBamFile.length()
        assert roddyBamFile.save(flush: true)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        String viewByPid = roddyBamFile2.individual.getViewByPidPathBase(roddyBamFile2.seqType).absoluteDataManagementPath.path
        String fastqFilesAsString = roddyBamFile2.seqTracks.collect {SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                lsdfFilesService.getFileViewByPidPath(dataFile) as File
            }
        }.flatten().join(';')

        SeqTrack seqTrack2 = roddyBamFile2.seqTracks.iterator()[0]
        List<DataFile> dataFiles2 = DataFile.findAllBySeqTrack(seqTrack2)
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles2[0])), dataFiles2[0])
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles2[1])), dataFiles2[1])

        String expectedCmd = """\
cd /tmp \
&& sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile2.workflow.name}_${roddyBamFile2.config.externalScriptVersion}.config@WGS \
${roddyBamFile2.individual.pid} \
--useconfig=${roddyApplicationIni} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile2.config.externalScriptVersion} \
--configurationDirectories=${new File(roddyBamFile2.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPid},${roddyBamFile2.tmpRoddyDirectory} \
--cvalues="fastq_list:${fastqFilesAsString},\
bam:${roddyBamFile.finalBamFile},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile},\
CHROM_SIZES_FILE:${chromosomeSizeFiles},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}"\
"""

        String actualCmd =  executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile2, dataManagement)

        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_WhenReadNumberOrderIsWrong_ShouldBeOk() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        String viewByPid = roddyBamFile.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path
        String fastqFilesAsString = roddyBamFile.seqTracks.collect {SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                lsdfFilesService.getFileViewByPidPath(dataFile) as File
            }
        }.flatten().join(';')

        String expectedCmd =  """\
cd /tmp && \
sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.externalScriptVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.externalScriptVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPid},${roddyBamFile.tmpRoddyDirectory} \
--cvalues="fastq_list:${fastqFilesAsString},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile},\
CHROM_SIZES_FILE:${chromosomeSizeFiles},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}"\
"""

        List<DataFile> dataFiles = []

        roddyBamFile.seqTracks.each { seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).sort {it.readNumber}.each { DataFile dataFile ->
                dataFiles << dataFile
            }
        }

        dataFiles.first().fileName = "DataFileFileName_R2.gz"
        dataFiles.last().fileName = "DataFileFileName_R1.gz"
        dataFiles.first().vbpFileName = "DataFileFileName_R2.gz"
        dataFiles.last().vbpFileName = "DataFileFileName_R1.gz"
        dataFiles.first().readNumber = 2
        dataFiles.last().readNumber = 1
        assert dataFiles.first().save(flush: true)
        assert dataFiles.last().save(flush: true)

        assert roddyBamFile.save(flush: true)

        String actualCmd = executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }


    @Test
    void testValidate_RoddyBamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(null)
        }.contains("Input roddyResultObject must not be null")
    }

    @Test
    void testValidate_BamDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyBamFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyBamFile.path)
    }

    @Test
    void testValidate_BaiDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyBaiFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyBaiFile.path)
    }

    @Test
    void testValidate_Md5sumDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyMd5sumFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyMd5sumFile.path)
    }

    @Test
    void testValidate_MergedQaDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyQADirectory.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyQADirectory.path)
    }

    @Test
    void testValidate_SingleLaneQaDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddySingleLaneQADirectories.values()*.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(RoddyBamFile.RUN_PREFIX)
    }

    @Test
    void testValidate_ExecutionStoreDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyExecutionStoreDirectory.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyExecutionStoreDirectory.path)
    }

    @Test
    void testValidate_AllFine() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        executePanCanJob.validate(roddyBamFile)
    }

    void createFileAndAddFileSize(File file, DataFile dataFile) {
        CreateFileHelper.createFile(file)
        dataFile.fileSize = file.length()
        assert dataFile.save(flush: true)
    }
}
