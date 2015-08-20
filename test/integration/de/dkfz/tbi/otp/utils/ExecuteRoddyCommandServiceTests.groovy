package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 */
class ExecuteRoddyCommandServiceTests {

    ExecuteRoddyCommandService executeRoddyCommandService = new ExecuteRoddyCommandService()

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()


    final String CONFIG_NAME = "WORKFLOW_VERSION"
    final String ANALYSIS_ID = "WHOLE_GENOME"

    File roddyPath
    File roddyCommand
    File tmpOutputDir
    Realm realm
    RoddyBamFile roddyBamFile
    File roddyBaseConfigsPath
    File applicationIniPath

    @Before
    void setUp() {
        temporaryFolder.create()

        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        roddyPath = new File(ProcessingOptionService.getValueOfProcessingOption("roddyPath"))
        roddyCommand = new File(roddyPath, 'roddy.sh')
        tmpOutputDir = temporaryFolder.newFolder("temporaryOutputDir")

        SeqType.buildLazy(name: SeqTypeNames.EXOME.seqTypeName, alias: SeqTypeNames.EXOME.seqTypeName, libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        roddyBamFile = DomainFactory.createRoddyBamFile()

        realm = DomainFactory.createRealmDataManagementDKFZ([
                name: roddyBamFile.project.realmName,
                rootPath: "${tmpOutputDir}/root",
                processingRootPath: "${tmpOutputDir}/processing",
        ])
        assert realm.save(flush: true)

        roddyBaseConfigsPath = new File(ProcessingOptionService.getValueOfProcessingOption("roddyBaseConfigsPath"))
        roddyBaseConfigsPath.mkdirs()
        new File(roddyBaseConfigsPath, "file name").write("file content")
        applicationIniPath = new File(ProcessingOptionService.getValueOfProcessingOption("roddyApplicationIni"))
        assert CreateFileHelper.createFile(applicationIniPath)

        ProcessHelperService.metaClass.static.executeCommandAndAssertExistCodeAndReturnProcessOutput = {String cmd ->
            assert cmd ==~ "cd /tmp && sudo -u OtherUnixUser ${temporaryFolder.getRoot()}/.*/correctPathPermissionsOtherUnixUserRemoteWrapper.sh ${temporaryFolder.getRoot()}/.*/merged-alignment"
            return new ProcessHelperService.ProcessOutput('', '', 0)
        }
    }

    @After
    void tearDown() {
        roddyPath = null
        tmpOutputDir = null
        realm = null
        roddyBamFile = null
        TestCase.removeMetaClass(ExecuteRoddyCommandService, executeRoddyCommandService)
        TestCase.removeMetaClass(ExecutionService, executeRoddyCommandService.executionService)
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }

    @Test
    void testRoddyBaseCommand_RoddyPathIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(null, CONFIG_NAME, ANALYSIS_ID)
        }.contains("roddyPath is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_ConfigNameIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(roddyPath, null, ANALYSIS_ID)
        }.contains("configName is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_AnalysisIdIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(roddyPath, CONFIG_NAME, null)
        }.contains("analysisId is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_AllFine() {
        String expectedCmd =  "cd /tmp && sudo -u OtherUnixUser ${roddyPath}/roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} "
        String actualCmd = executeRoddyCommandService.roddyBaseCommand(roddyPath, CONFIG_NAME, ANALYSIS_ID)
        assert expectedCmd == actualCmd
    }


    @Test
    void testGetAnalysisIDinConfigFile_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(null)
        }.contains("The input roddyResult must not be null")
    }

    @Test
    void testGetAnalysisIDinConfigFile_ObjectDoesNotHaveGetSeqTypeMethod_ShouldFail() {
        roddyBamFile.metaClass.getSeqType = { -> null }
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        }.contains("There is not seqType available")
    }

    @Test
    void testGetAnalysisIDinConfigFile_SeqTypeWGS() {
        roddyBamFile.mergingWorkPackage.seqType = SeqType.wholeGenomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqType.wholeGenomePairedSeqType.alias == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_SeqTypeEXOME() {
        roddyBamFile.mergingWorkPackage.seqType = SeqType.exomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqType.exomePairedSeqType.alias == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_DifferentSeqType_ShouldFail() {
        roddyBamFile.mergingWorkPackage.seqType = SeqType.build(name: "differentSeqType")
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert TestCase.shouldFail(RuntimeException) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        }.contains("The seqType ${roddyBamFile.seqType} can not be processed at the moment")
    }


    @Test
    void testDefaultRoddyExecutionCommand_ObjectIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(null, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("The input roddyResult is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_NameInConfigFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, null, ANALYSIS_ID, realm)
        }.contains("The input nameInConfigFile is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_AnalysisIDinConfigFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, null, realm)
        }.contains("The input analysisIDinConfigFile is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_RealmIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, null)
        }.contains("The input realm is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyPathIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyPath").delete(flush: true)
        assert !ProcessingOption.findByName("roddyPath")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyVersionIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyVersion").delete(flush: true)
        assert !ProcessingOption.findByName("roddyVersion")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyBaseConfigsPathIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyBaseConfigsPath").delete(flush: true)
        assert !ProcessingOption.findByName("roddyBaseConfigsPath")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyApplicationIni").delete(flush: true)
        assert !ProcessingOption.findByName("roddyApplicationIni")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }


    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniDoesNotExistInFilesystem_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }
        assert applicationIniPath.delete()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains(applicationIniPath.path)
    }

    @Test
    void testDefaultRoddyExecutionCommand_BaseConfigFolderDoesNotExistInFilesystem_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }
        assert roddyBaseConfigsPath.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains(roddyBaseConfigsPath.path)
    }


    @Test
    void testDefaultRoddyExecutionCommand_AllFine() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        String viewByPid = roddyBamFile.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path

        String expectedCmd = "cd /tmp && " +
"sudo -u OtherUnixUser ${roddyCommand} rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
"${roddyBamFile.individual.pid} " +
"--useconfig=${applicationIniPath} " +
"--useRoddyVersion=2.1.28 " +
"--usePluginVersion=${roddyBamFile.config.pluginVersion} " +
"--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} " +
"--useiodir=${viewByPid},${realm.rootPath}/${roddyBamFile.project.dirName}/sequencing/${roddyBamFile.seqType.dirName}/view-by-pid/" +
"${roddyBamFile.individual.pid}/${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/" +
"merged-alignment/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id} "

        LogThreadLocal.withThreadLog(System.out) {
            String actualCmd = executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
            assert expectedCmd == actualCmd
        }
    }



    @Test
    void testExecuteCommandAsRoddyUser() {
        assert 'sudo -u OtherUnixUser' == executeRoddyCommandService.executeCommandAsRoddyUser()
    }



    @Test
    void testCreateTemporaryOutputDirectory_RealmIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createTemporaryOutputDirectory(null, tmpOutputDir)
        }
    }

    @Test
    void testCreateTemporaryOutputDirectory_FileIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createTemporaryOutputDirectory(realm, null)
        }
    }

   @Test
    void testCreateTemporaryOutputDirectory_DirectoryCreationFailed_ShouldFail() {
       executeRoddyCommandService.executionService.metaClass.executeCommand = { Realm realm, String command -> }
       tmpOutputDir.absoluteFile.delete()
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createTemporaryOutputDirectory(realm, tmpOutputDir)
        }
    }

    @Test
    void testCreateTemporaryOutputDirectory_AllFine() {
        executeRoddyCommandService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            [ 'bash', '-c', command ].execute().waitFor()
        }
        executeRoddyCommandService.createTemporaryOutputDirectory(realm, tmpOutputDir)
    }



    @Test
    void testCorrectPermissionCommand_AllFine() {
        String expected = "cd /tmp && sudo -u OtherUnixUser ${ProcessingOptionService.getValueOfProcessingOption(ExecuteRoddyCommandService.CORRECT_PERMISSION_SCRIPT_NAME)} ${tmpOutputDir}"
        String cmd = executeRoddyCommandService.correctPermissionCommand(tmpOutputDir)

        assert expected == cmd
    }

    @Test
    void testCorrectPermissionCommand_BasePathIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctPermissionCommand(null)
        }.contains('basePath is not allowed to be null')
    }

    @Test
    void testCorrectPermissionCommand_processingOptionNotSet_ShouldFail() {
        ProcessingOption.findByName(ExecuteRoddyCommandService.CORRECT_PERMISSION_SCRIPT_NAME).delete(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctPermissionCommand(tmpOutputDir)
        }.contains('Collection contains 0 elements. Expected 1.')
    }

    @Test
    void testCorrectPermission_AllFine() {
        executeRoddyCommandService.correctPermissions(roddyBamFile)
    }

    @Test
    void testCorrectPermission_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctPermissions(null)
        }.contains('RoddyBamFile')
    }
}
