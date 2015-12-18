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
    File featureTogglesConfigPath
    File chromosomeStatSizeFile
    File processingRootPath
    File dataFile1File
    File dataFile2File

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
        dataProcessing = DomainFactory.createRealmDataProcessing(tmpDir.root, [name: roddyBamFile.project.realmName])
        dataManagement = DomainFactory.createRealmDataManagement(tmpDir.root, [name: roddyBamFile.project.realmName])

        processingRootPath = dataProcessing.processingRootPath as File

        DomainFactory.createRoddyProcessingOptions(tmpDir.newFolder())

        roddyPath = ProcessingOption.findByName("roddyPath").value as File
        roddyCommand = new File(roddyPath, 'roddy.sh')
        roddyVersion = ProcessingOption.findByName("roddyVersion").value
        roddyBaseConfigsPath = ProcessingOption.findByName("roddyBaseConfigsPath").value as File
        roddyBaseConfigsPath.mkdirs()
        roddyApplicationIni = ProcessingOption.findByName("roddyApplicationIni").value as File
        roddyApplicationIni.text = "Some Text"
        featureTogglesConfigPath = ProcessingOption.findByName(ExecuteRoddyCommandService.FEATURE_TOGGLES_CONFIG_PATH).value as File

        prepareDataFilesOnFileSystem()

        referenceGenomeDir = new File("${processingRootPath}/reference_genomes/${roddyBamFile.referenceGenome.path}")
        assert referenceGenomeDir.mkdirs()

        referenceGenomeFile = new File(referenceGenomeDir, "${roddyBamFile.referenceGenome.fileNamePrefix}.fa")
        CreateFileHelper.createFile(referenceGenomeFile)

        configFile = tmpDir.newFile()
        CreateFileHelper.createFile(configFile)
        RoddyWorkflowConfig config = roddyBamFile.config
        config.configFilePath = configFile.path
        assert config.save(failOnError: true)

        chromosomeStatSizeFile = executePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage, false)
        CreateFileHelper.createFile(chromosomeStatSizeFile)

        roddyBamFile.workPackage.metaClass.findMergeableSeqTracks = { -> SeqTrack.list() }

        executePanCanJob.executeRoddyCommandService.metaClass.correctPermissions = { RoddyBamFile bamFile -> }
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
        chromosomeStatSizeFile = null
        dataProcessing = null
        dataManagement = null
        seqTrack = null
        roddyBamFile = null
        configFile = null
        dataFile1File = null
        dataFile2File = null
    }

    private void prepareDataFilesOnFileSystem() {
        assert 1 == roddyBamFile.seqTracks.size()
        seqTrack = roddyBamFile.seqTracks.iterator()[0]
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack, [sort: 'readNumber'])
        assert 2 == dataFiles.size()
        dataFile1File = new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[0]))
        createFileAndAddFileSize(dataFile1File, dataFiles[0])
        dataFile2File = new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[1]))
        createFileAndAddFileSize(dataFile2File, dataFiles[1])
    }


    private String viewByPidString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path
    }

    private String fastqFilesAsString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.seqTracks.collect {SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                lsdfFilesService.getFileViewByPidPath(dataFile) as File
            }
        }.flatten().join(';')
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
    void testPrepareAndReturnWorkflowSpecificCommand_PathToChromosomeStatSizeFileIsNull_ShouldFail() {
        executePanCanJob.referenceGenomeService.metaClass.chromosomeStatSizeFile = { MergingWorkPackage mergingWorkPackage ->
            return null
        }

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains("Path to the chromosome stat size file is null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsButIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        assert roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        SeqTrack seqTrack = roddyBamFile2.seqTracks.iterator()[0]
        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
        assert 2 == dataFiles.size()
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[0])), dataFiles[0])
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles[1])), dataFiles[1])

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile2, dataManagement)
        }.contains(roddyBamFile.workBamFile.path)

    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_DataFilesAreNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        DataFile.findAllBySeqTrack(seqTrack).each {
            assert new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(it)).delete()
        }
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(dataFile1File.path)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ConfigFileIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        assert configFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(roddyBamFile.config.configFilePath)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ReferenceGenomeIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        assert referenceGenomeFile.delete()
        assert TestCase.shouldFail(RuntimeException) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(referenceGenomeFile.path)

    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ChromosomeStatSizeFileDoesNotExist_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }
        assert chromosomeStatSizeFile.delete()
        assert TestCase.shouldFail(RuntimeException) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(chromosomeStatSizeFile.path)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_NoBaseBamFileExists() {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        String expectedCmd =  """\
cd /tmp \
&& sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.pluginVersion}_${roddyBamFile.config.configVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.pluginVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPidString()},${roddyBamFile.workDirectory} \
--cvalues="fastq_list:${fastqFilesAsString()},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile.path},\
CHROM_SIZES_FILE:${chromosomeStatSizeFile},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}"\
"""

        String actualCmd = executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_NoBaseBamFileExists_FastTrack() {
        roddyBamFile.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert roddyBamFile.project.save(flush: true)

        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        String expectedCmd =  """\
cd /tmp \
&& sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.pluginVersion}_${roddyBamFile.config.configVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.pluginVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPidString()},${roddyBamFile.workDirectory} \
--cvalues="fastq_list:${fastqFilesAsString()},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile.path},\
CHROM_SIZES_FILE:${chromosomeStatSizeFile},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName},\
PBS_AccountName:FASTTRACK"\
"""

        String actualCmd = executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_Exome_NoBaseBamFileExists() {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        roddyBamFile.mergingWorkPackage.seqType = DomainFactory.createExomeSeqType()
        assert roddyBamFile.mergingWorkPackage.save(flush: true, failOnError: true)
        roddyBamFile.seqTracks = [DomainFactory.buildSeqTrackWithDataFile(roddyBamFile.mergingWorkPackage)]
        assert roddyBamFile.save(flush: true, failOnError: true)

        prepareDataFilesOnFileSystem()

        DomainFactory.createBedFile(
                referenceGenome: roddyBamFile.referenceGenome,
                libraryPreparationKit: roddyBamFile.mergingWorkPackage.libraryPreparationKit,
        )

        BedFile bedFile = roddyBamFile.bedFile
        File bedFilePath = executePanCanJob.bedFileService.filePath(dataProcessing, bedFile, false) as File
        assert bedFilePath.parentFile.mkdirs()
        assert bedFilePath.createNewFile()


        String expectedCmd =  """\
cd /tmp \
&& sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.pluginVersion}_${roddyBamFile.config.configVersion}.config@WES \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.pluginVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPidString()},${roddyBamFile.workDirectory} \
--cvalues="fastq_list:${fastqFilesAsString()},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile.path},\
CHROM_SIZES_FILE:${chromosomeStatSizeFile},\
TARGET_REGIONS_FILE:${bedFilePath},\
TARGETSIZE:${bedFile.targetSize},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}"\
"""

        String actualCmd = executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsAlready() {
        helperForTestPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsAlready('Work')
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsAlreadyButInOldStructure() {
        roddyBamFile.workDirectoryName = null
        assert roddyBamFile.save(flush: true, failOnError: true)

        helperForTestPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsAlready('Final')
    }

    private void helperForTestPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsAlready(String workOrFinal) {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        CreateFileHelper.createFile(roddyBamFile."get${workOrFinal}BamFile"())
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = HelperUtils.randomMd5sum
        roddyBamFile.fileSize = roddyBamFile."get${workOrFinal}BamFile"().length()
        assert roddyBamFile.save(flush: true, failOnError: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile, [config: roddyBamFile.config])

        SeqTrack seqTrack2 = roddyBamFile2.seqTracks.iterator()[0]
        List<DataFile> dataFiles2 = DataFile.findAllBySeqTrack(seqTrack2)
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles2[0])), dataFiles2[0])
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileViewByPidPath(dataFiles2[1])), dataFiles2[1])

        String expectedCmd = """\
cd /tmp \
&& sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile2.workflow.name}_${roddyBamFile2.config.pluginVersion}_${roddyBamFile.config.configVersion}.config@WGS \
${roddyBamFile2.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile2.config.pluginVersion} \
--configurationDirectories=${new File(roddyBamFile2.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPidString(roddyBamFile2)},${roddyBamFile2.workDirectory} \
--cvalues="fastq_list:${fastqFilesAsString(roddyBamFile2)},\
bam:${roddyBamFile."get${workOrFinal}BamFile"()},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile},\
CHROM_SIZES_FILE:${chromosomeStatSizeFile},\
possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}"\
"""

        String actualCmd =  executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile2, dataManagement)

        assert expectedCmd == actualCmd
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_WhenReadNumberOrderIsWrong_ShouldBeOk() {
        executePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        String expectedCmd =  """\
cd /tmp && \
sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.pluginVersion}_${roddyBamFile.config.configVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.pluginVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPidString()},${roddyBamFile.workDirectory} \
--cvalues="fastq_list:${fastqFilesAsString()},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile},\
CHROM_SIZES_FILE:${chromosomeStatSizeFile},\
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
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.workBamFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workBamFile.path)
    }

    @Test
    void testValidate_BaiDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.workBaiFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workBaiFile.path)
    }

    @Test
    void testValidate_Md5sumDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.workMd5sumFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workMd5sumFile.path)
    }

    @Test
    void testValidate_MergedQaDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.workQADirectory.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workQADirectory.path)
    }

    @Test
    void testValidate_SingleLaneQaDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        File singleLaneQa = roddyBamFile.workSingleLaneQADirectories.values().iterator().next()
        assert singleLaneQa.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(singleLaneQa.path)
    }

    @Test
    void testValidate_ExecutionStoreDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.workExecutionStoreDirectory.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workExecutionStoreDirectory.path)
    }

    @Test
    void testValidate_PermissionChangeFail_ShouldFail() {
        String message = HelperUtils.uniqueString
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        executePanCanJob.executeRoddyCommandService.metaClass.correctPermissions = { RoddyBamFile bamFile -> assert false, message }

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(message)
    }

    @Test
    void testValidate_PermissionChangeDoneBeforeFileChecking() {
        String MESSAGE_CORRECT_PERMISSION = 'CORECCT PERMISSION'
        String MESSAGE_CHECK_FILES = 'CHECK FILES'
        String message = MESSAGE_CORRECT_PERMISSION
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        executePanCanJob.executeRoddyCommandService.metaClass.correctPermissions = { RoddyBamFile bamFile ->
            assert MESSAGE_CORRECT_PERMISSION == message
            message = MESSAGE_CHECK_FILES
        }
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = {File file-> assert MESSAGE_CHECK_FILES == message}
        LsdfFilesService.metaClass.static.ensureDirIsReadableAndNotEmpty = {File file-> assert MESSAGE_CHECK_FILES == message}

        executePanCanJob.validate(roddyBamFile)
        assert MESSAGE_CHECK_FILES == message
    }

    @Test
    void testValidate_AllFine() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(dataManagement, roddyBamFile)
        executePanCanJob.validate(roddyBamFile)
    }

    void createFileAndAddFileSize(File file, DataFile dataFile) {
        CreateFileHelper.createFile(file)
        dataFile.fileSize = file.length()
        assert dataFile.save(flush: true)
    }
}
