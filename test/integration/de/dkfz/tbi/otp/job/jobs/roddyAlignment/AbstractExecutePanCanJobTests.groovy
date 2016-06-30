package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
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


class AbstractExecutePanCanJobTests {

    AbstractExecutePanCanJob abstractExecutePanCanJob

    LsdfFilesService lsdfFilesService

    Realm dataManagement
    RoddyBamFile roddyBamFile
    File configFile
    File roddyCommand
    String roddyVersion
    File roddyBaseConfigsPath
    File roddyApplicationIni
    File chromosomeStatSizeFile
    File featureTogglesConfigPath
    File referenceGenomeFile

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
        roddyBamFile.workPackage.metaClass.findMergeableSeqTracks = { -> SeqTrack.list() }

        Realm dataProcessing = DomainFactory.createRealmDataProcessing(tmpDir.root, [name: roddyBamFile.project.realmName])
        dataManagement = DomainFactory.createRealmDataManagement(tmpDir.root, [name: roddyBamFile.project.realmName])
        abstractExecutePanCanJob = [
                prepareAndReturnWorkflowSpecificCValues: { RoddyBamFile bamFile -> ",workflowSpecificCValues" },
                prepareAndReturnWorkflowSpecificParameter: { RoddyBamFile bamFile -> "workflowSpecificParameter " },
                workflowSpecificValidation: { RoddyBamFile bamFile -> }

        ] as AbstractExecutePanCanJob

        abstractExecutePanCanJob.referenceGenomeService = new ReferenceGenomeService()
        abstractExecutePanCanJob.referenceGenomeService.configService = new ConfigService()
        abstractExecutePanCanJob.lsdfFilesService = new LsdfFilesService()
        abstractExecutePanCanJob.executeRoddyCommandService = new ExecuteRoddyCommandService()
        abstractExecutePanCanJob.bedFileService = new BedFileService()
        abstractExecutePanCanJob.configService = new ConfigService()

        File processingRootPath = dataProcessing.processingRootPath as File

        DomainFactory.createRoddyProcessingOptions(tmpDir.newFolder())

        File roddyPath = ProcessingOption.findByName("roddyPath").value as File
        roddyCommand = new File(roddyPath, 'roddy.sh')
        roddyVersion = ProcessingOption.findByName("roddyVersion").value
        roddyBaseConfigsPath = ProcessingOption.findByName("roddyBaseConfigsPath").value as File
        roddyBaseConfigsPath.mkdirs()
        roddyApplicationIni = ProcessingOption.findByName("roddyApplicationIni").value as File
        roddyApplicationIni.text = "Some Text"
        featureTogglesConfigPath = ProcessingOption.findByName(ExecuteRoddyCommandService.FEATURE_TOGGLES_CONFIG_PATH).value as File

        File referenceGenomeDir = new File("${processingRootPath}/reference_genomes/${roddyBamFile.referenceGenome.path}")
        assert referenceGenomeDir.mkdirs()
        referenceGenomeFile = new File(referenceGenomeDir, "${roddyBamFile.referenceGenome.fileNamePrefix}.fa")
        CreateFileHelper.createFile(referenceGenomeFile)

        configFile = tmpDir.newFile()
        CreateFileHelper.createFile(configFile)
        RoddyWorkflowConfig config = roddyBamFile.config
        config.configFilePath = configFile.path
        assert config.save(failOnError: true)

        chromosomeStatSizeFile = abstractExecutePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage, false)
        CreateFileHelper.createFile(chromosomeStatSizeFile)

        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.correctPermissions = { RoddyBamFile bamFile, Realm realm -> }
    }


    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecuteRoddyCommandService, abstractExecutePanCanJob.executeRoddyCommandService)
        TestCase.removeMetaClass(ReferenceGenomeService, abstractExecutePanCanJob.referenceGenomeService)
    }


    private String viewByPidString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RoddyBamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(null, dataManagement)
        }.contains("roddyResult must not be null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RealmIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, null)
        }.contains("realm must not be null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ConfigFileIsNotInFileSystem_ShouldFail() {
        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }
        assert configFile.delete()

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(roddyBamFile.config.configFilePath)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_AllFine() {
        roddyBamFile.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert roddyBamFile.project.save(flush: true)

        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        String expectedCmd =  """\
cd /tmp \
&& sudo -u OtherUnixUser ${roddyCommand} rerun ${roddyBamFile.pipeline.name}_${roddyBamFile.config.pluginVersion}_${roddyBamFile.config.configVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--usePluginVersion=${roddyBamFile.config.pluginVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPidString()},${roddyBamFile.workDirectory} \
workflowSpecificParameter \
--cvalues="INDEX_PREFIX:${referenceGenomeFile.path},\
CHROM_SIZES_FILE:${chromosomeStatSizeFile},\
workflowSpecificCValues,\
PBS_AccountName:FASTTRACK"\
"""

        String actualCmd = abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }


    @Test
    void testPrepareAndReturnCValues_RoddyBamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnCValues(null)
        }.contains("roddyBamFile must not be null")
    }


    @Test
    void testPrepareAndReturnCValues_PathToReferenceGenomeFastaFileIsNull_ShouldFail() {
        abstractExecutePanCanJob.referenceGenomeService.metaClass.fastaFilePath = { Project project, ReferenceGenome referenceGenome ->
            return null
        }

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnCValues(roddyBamFile)
        }.contains("Path to the reference genome file is null")
    }


    @Test
    void testPrepareAndReturnCValues_PathToChromosomeStatSizeFileIsNull_ShouldFail() {
        abstractExecutePanCanJob.referenceGenomeService.metaClass.chromosomeStatSizeFile = { MergingWorkPackage mergingWorkPackage ->
            return null
        }

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnCValues(roddyBamFile)
        }.contains("Path to the chromosome stat size file is null")
    }


    @Test
    void testPrepareAndReturnCValues_NoFastTrack_setUpCorrect() {
        String expectedCommand = """\
--cvalues=\
"INDEX_PREFIX:${referenceGenomeFile.path},\
CHROM_SIZES_FILE:${chromosomeStatSizeFile},\
workflowSpecificCValues"\
"""
        assert expectedCommand == abstractExecutePanCanJob.prepareAndReturnCValues(roddyBamFile)
    }


    @Test
    void testPrepareAndReturnCValues_FastTrack_setUpCorrect() {
        roddyBamFile.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert roddyBamFile.project.save(flush: true)

        String expectedCommand = """\
--cvalues=\
"INDEX_PREFIX:${referenceGenomeFile.path},\
CHROM_SIZES_FILE:${chromosomeStatSizeFile},\
workflowSpecificCValues,\
PBS_AccountName:FASTTRACK"\
"""
        assert expectedCommand == abstractExecutePanCanJob.prepareAndReturnCValues(roddyBamFile)
    }


    @Test
    void testValidate_RoddyBamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(null)
        }.contains("Input roddyResultObject must not be null")
    }


    @Test
    void testValidate_PermissionChangeFail_ShouldFail() {
        String message = HelperUtils.uniqueString
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.correctPermissions = { RoddyBamFile bamFile, Realm realm -> assert false, message }

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(message)
    }


    @Test
    void testValidate_BaseBamFileChanged_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        assert roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)

        assert TestCase.shouldFail(RuntimeException) {
            abstractExecutePanCanJob.validate(roddyBamFile2)
        }.contains("The input BAM file seems to have changed on the file system while this job was processing it")

    }


    @Test
    void testValidate_BamDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        roddyBamFile.workBamFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workBamFile.path)
    }


    @Test
    void testValidate_BaiDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        roddyBamFile.workBaiFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workBaiFile.path)
    }


    @Test
    void testValidate_Md5sumDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        roddyBamFile.workMd5sumFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workMd5sumFile.path)
    }


    @Test
    void testValidate_MergedQaDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        roddyBamFile.workQADirectory.deleteDir()

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workQADirectory.path)
    }


    @Test
    void testValidate_MergedQaJsonFileDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        assert roddyBamFile.getWorkMergedQAJsonFile().delete()

        assert CreateFileHelper.createFile(new File(roddyBamFile.workMergedQADirectory, "someFile"))

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.getWorkMergedQAJsonFile().path)
    }


    @Test
    void testValidate_SingleLaneJsonFileDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        File singleLaneQa = roddyBamFile.getWorkSingleLaneQAJsonFiles().values().iterator().next()
        assert singleLaneQa.delete()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(singleLaneQa.path)
    }


    @Test
    void testValidate_ExecutionStoreDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        roddyBamFile.workExecutionStoreDirectory.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.workExecutionStoreDirectory.path)
    }


    @Test
    void testValidate_WrongFileOperationStatus_ShouldFail(){
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.INPROGRESS
        assert roddyBamFile.save(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.validate(roddyBamFile)
        }.contains(AbstractMergedBamFile.FileOperationStatus.INPROGRESS.name())
    }


    @Test
    void testValidate_AllFine(){
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        abstractExecutePanCanJob.validate(roddyBamFile)
    }

    @Test
    void testValidate_WgbsData_AllFine(){
        SeqType wgbsSeqType = DomainFactory.createWholeGenomeBisulfiteSeqType()
        DomainFactory.changeSeqType(roddyBamFile, wgbsSeqType, "lib1")

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        abstractExecutePanCanJob.validate(roddyBamFile)
    }


    @Test
    void testEnsureCorrectBaseBamFileIsOnFileSystem_NoBaseBamFile_AllFine() {
        abstractExecutePanCanJob.ensureCorrectBaseBamFileIsOnFileSystem(null)
    }


    @Test
    void testEnsureCorrectBaseBamFileIsOnFileSystem_BaseBamFileExistsButNotInFileSystem_ShouldFail() {
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        assert roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.ensureCorrectBaseBamFileIsOnFileSystem(roddyBamFile)
        }.contains(roddyBamFile.workBamFile.path)
    }


    @Test
    void testEnsureCorrectBaseBamFileIsOnFileSystem_BaseBamFileExistsButFileSizeIsWrong_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        roddyBamFile.fileSize = 12345678
        assert roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.ensureCorrectBaseBamFileIsOnFileSystem(roddyBamFile)
        }.contains(roddyBamFile.workBamFile.path)
    }


    @Test
    void testEnsureCorrectBaseBamFileIsOnFileSystem_BaseBamFileExistsAndIsCorrect() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        roddyBamFile.fileSize = roddyBamFile.getWorkBamFile().size()
        assert roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        abstractExecutePanCanJob.ensureCorrectBaseBamFileIsOnFileSystem(roddyBamFile)
    }
}
