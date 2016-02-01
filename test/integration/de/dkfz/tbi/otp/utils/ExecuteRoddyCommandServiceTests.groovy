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
    File featureTogglesConfigPath
    String roddyVersion

    @Before
    void setUp() {
        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        roddyPath = new File(ProcessingOptionService.getValueOfProcessingOption("roddyPath"))
        roddyCommand = new File(roddyPath, 'roddy.sh')
        tmpOutputDir = temporaryFolder.newFolder("temporaryOutputDir")

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
        featureTogglesConfigPath = new File(ProcessingOptionService.getValueOfProcessingOption(ExecuteRoddyCommandService.FEATURE_TOGGLES_CONFIG_PATH))
        roddyVersion = ProcessingOptionService.getValueOfProcessingOption("roddyVersion")
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
            executeRoddyCommandService.roddyBaseCommand(null, CONFIG_NAME, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)
        }.contains("roddyPath is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_ConfigNameIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(roddyPath, null, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)
        }.contains("configName is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_AnalysisIdIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(roddyPath, CONFIG_NAME, null, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)
        }.contains("analysisId is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_AllFine() {
        String expectedCmd =  "cd /tmp && sudo -u OtherUnixUser ${roddyPath}/roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} "
        String actualCmd = executeRoddyCommandService.roddyBaseCommand(roddyPath, CONFIG_NAME, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)
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
        DomainFactory.createAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = SeqType.wholeGenomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqType.wholeGenomePairedSeqType.roddyName == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_SeqTypeEXOME() {
        DomainFactory.createAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = SeqType.exomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqType.exomePairedSeqType.roddyName == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_DifferentSeqType_ShouldFail() {
        DomainFactory.createAlignableSeqTypes()
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
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyPath").delete(flush: true)
        assert !ProcessingOption.findByName("roddyPath")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyVersionIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyVersion").delete(flush: true)
        assert !ProcessingOption.findByName("roddyVersion")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyBaseConfigsPathIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyBaseConfigsPath").delete(flush: true)
        assert !ProcessingOption.findByName("roddyBaseConfigsPath")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyApplicationIni").delete(flush: true)
        assert !ProcessingOption.findByName("roddyApplicationIni")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }


    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniDoesNotExistInFilesystem_ShouldFail() {
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }
        assert applicationIniPath.delete()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains(applicationIniPath.path)
    }

    @Test
    void testDefaultRoddyExecutionCommand_BaseConfigFolderDoesNotExistInFilesystem_ShouldFail() {
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }
        assert roddyBaseConfigsPath.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains(roddyBaseConfigsPath.path)
    }


    @Test
    void testDefaultRoddyExecutionCommand_AllFine() {
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        String viewByPid = roddyBamFile.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path

        String expectedCmd = "cd /tmp && " +
"sudo -u OtherUnixUser ${roddyCommand} rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
"${roddyBamFile.individual.pid} " +
"--useconfig=${applicationIniPath} " +
"--usefeaturetoggleconfig=${featureTogglesConfigPath} " +
"--useRoddyVersion=${roddyVersion} " +
"--usePluginVersion=${roddyBamFile.config.pluginVersion} " +
"--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} " +
"--useiodir=${viewByPid},${roddyBamFile.workDirectory} "

        LogThreadLocal.withThreadLog(System.out) {
            String actualCmd = executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
            assert expectedCmd == actualCmd
        }
    }

    @Test
    void testRoddyGetRuntimeConfigCommand_AllFine() {
        String expectedCmd = "cd /tmp && " +
"sudo -u OtherUnixUser ${roddyCommand} printidlessruntimeconfig ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
"--useconfig=${applicationIniPath} " +
"--usefeaturetoggleconfig=${featureTogglesConfigPath} " +
"--useRoddyVersion=${roddyVersion} " +
"--usePluginVersion=${roddyBamFile.config.pluginVersion} " +
"--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} "

        LogThreadLocal.withThreadLog(System.out) {
            String actualCmd = executeRoddyCommandService.roddyGetRuntimeConfigCommand(roddyBamFile.config, CONFIG_NAME, ANALYSIS_ID)
            assert expectedCmd == actualCmd
        }
    }


    @Test
    void testExecuteCommandAsRoddyUser() {
        assert 'sudo -u OtherUnixUser' == executeRoddyCommandService.executeCommandAsRoddyUser()
    }



    @Test
    void testCreateWorkOutputDirectory_RealmIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createWorkOutputDirectory(null, tmpOutputDir)
        }
    }

    @Test
    void testCreateWorkOutputDirectory_FileIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createWorkOutputDirectory(realm, null)
        }
    }

   @Test
    void testCreateWorkOutputDirectory_DirectoryCreationFailed_ShouldFail() {
       executeRoddyCommandService.executionService.metaClass.executeCommand = { Realm realm, String command -> }
       tmpOutputDir.absoluteFile.delete()
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createWorkOutputDirectory(realm, tmpOutputDir)
        }
    }

    @Test
    void testCreateWorkOutputDirectory_AllFine() {
        executeRoddyCommandService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            [ 'bash', '-c', command ].execute().waitFor()
        }
        executeRoddyCommandService.createWorkOutputDirectory(realm, tmpOutputDir)
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
        ProcessHelperService.metaClass.static.executeCommandAndAssertExistCodeAndReturnProcessOutput = {String cmd ->
            assert cmd ==~ "cd /tmp && sudo -u OtherUnixUser ${temporaryFolder.getRoot()}/.*/correctPathPermissionsOtherUnixUserRemoteWrapper.sh ${roddyBamFile.workDirectory}"
            return new ProcessHelperService.ProcessOutput('', '', 0)
        }

        executeRoddyCommandService.correctPermissions(roddyBamFile)
    }

    @Test
    void testCorrectPermission_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctPermissions(null)
        }.contains('RoddyBamFile')
    }


    @Test
    void testCorrectGroupCommand_AllFine() {
        String expected = "cd /tmp && sudo -u OtherUnixUser ${ProcessingOptionService.getValueOfProcessingOption(ExecuteRoddyCommandService.CORRECT_GROUP_SCRIPT_NAME)} ${tmpOutputDir}"
        String cmd = executeRoddyCommandService.correctGroupCommand(tmpOutputDir)

        assert expected == cmd
    }

    @Test
    void testCorrectGroupCommand_BasePathIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctGroupCommand(null)
        }.contains('basePath is not allowed to be null')
    }

    @Test
    void testCorrectGroupCommand_processingOptionNotSet_ShouldFail() {
        ProcessingOption.findByName(ExecuteRoddyCommandService.CORRECT_GROUP_SCRIPT_NAME).delete(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctGroupCommand(tmpOutputDir)
        }.contains('Collection contains 0 elements. Expected 1.')
    }

    @Test
    void testCorrectGroup_AllFine() {
        ProcessHelperService.metaClass.static.executeCommandAndAssertExistCodeAndReturnProcessOutput = {String cmd ->
            assert cmd ==~ "cd /tmp && sudo -u OtherUnixUser ${temporaryFolder.getRoot()}/.*/correctGroupOtherUnixUserRemoteWrapper.sh ${roddyBamFile.workDirectory}"
            return new ProcessHelperService.ProcessOutput('', '', 0)
        }

        executeRoddyCommandService.correctGroups(roddyBamFile)
    }

    @Test
    void testCorrectGroup_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctGroups(null)
        }.contains('RoddyBamFile')
    }



    @Test
    void testDeleteContentOfOtherUnixUserDirectory_AllFine() {
        ProcessHelperService.metaClass.static.executeCommandAndAssertExistCodeAndReturnProcessOutput = {String cmd ->
            assert cmd ==~ "cd /tmp && sudo -u OtherUnixUser ${temporaryFolder.getRoot()}/.*/deleteContentOfRoddyDirectoriesRemoteWrapper.sh ${roddyBamFile.workDirectory}"
            return new ProcessHelperService.ProcessOutput('', '', 0)
        }

        executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(roddyBamFile.workDirectory)
    }

    @Test
    void testDeleteContentOfOtherUnixUserDirectory_processingOptionNotSet_ShouldFail() {
        ProcessingOption.findByName(ExecuteRoddyCommandService.DELETE_CONTENT_OF_OTHERUNIXUSER_DIRECTORIES_SCRIPT).delete(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(roddyBamFile.workDirectory)
        }.contains('Collection contains 0 elements. Expected 1.')
    }


    @Test
    void testDeleteContentOfOtherUnixUserDirectory_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(null)
        }.contains('basePath is not allowed to be null')
    }
}
