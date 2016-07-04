package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import org.codehaus.groovy.grails.commons.*
import org.junit.*
import org.junit.rules.*

import static de.dkfz.tbi.otp.utils.ProcessHelperService.*


/**
 */
class ExecuteRoddyCommandServiceTests {

    GrailsApplication grailsApplication

    ExecuteRoddyCommandService executeRoddyCommandService = new ExecuteRoddyCommandService()

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()


    final String CONFIG_NAME = "WORKFLOW_VERSION"
    final String ANALYSIS_ID = "WHOLE_GENOME"

    public static final String RODDY_EXECUTION_DIR_NAME_1 = "exec_000000_000000000_a_a"
    public static final String RODDY_EXECUTION_DIR_NAME_2 = "exec_000000_000000000_b_b"

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
                roddyUser: System.getProperty("user.name"),
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


    void helperFor_testDefaultRoddyExecutionCommand_AllFine() {
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
    void testDefaultRoddyExecutionCommand_firstRun_AllFine() {
        helperFor_testDefaultRoddyExecutionCommand_AllFine()

        assert roddyBamFile.roddyExecutionDirectoryNames.empty
    }

    @Test
    void testDefaultRoddyExecutionCommand_restartWithoutDeletedWorkDirectory_AllFine() {
        roddyBamFile.roddyExecutionDirectoryNames = [
                RODDY_EXECUTION_DIR_NAME_1
        ]
        roddyBamFile.save(flush: true, failOnError: true)
        assert roddyBamFile.workDirectory.mkdirs()

        helperFor_testDefaultRoddyExecutionCommand_AllFine()

        assert 1 == roddyBamFile.roddyExecutionDirectoryNames.size()
        assert [RODDY_EXECUTION_DIR_NAME_1] == roddyBamFile.roddyExecutionDirectoryNames
    }

    @Test
    void testDefaultRoddyExecutionCommand_restartWithDeletedWorkDirectory_AllFine() {
        roddyBamFile.roddyExecutionDirectoryNames = [
                RODDY_EXECUTION_DIR_NAME_1,
                RODDY_EXECUTION_DIR_NAME_2,
        ]
        roddyBamFile.save(flush: true, failOnError: true)

        helperFor_testDefaultRoddyExecutionCommand_AllFine()

        assert roddyBamFile.roddyExecutionDirectoryNames.empty
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
    void testCorrectPermission_AllOkay() {
        executeRoddyCommandService.executionService = [
                executeCommandReturnProcessOutput: { Realm realm1, String cmd, String user ->
                    assert realm1 == realm
                    assert user == realm.roddyUser
                    ProcessOutput out =  waitForCommand(cmd)
                    assert out.stderrEmptyAndExitCodeZero
                    assert out.stdout == """
correct directory permission
2

correct file permission for non bam/bai files
1

correct file permission for bam/bai files
2
"""
                    return out
                }
        ] as ExecutionService

        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, "file"))
        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, roddyBamFile.baiFileName))
        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, roddyBamFile.bamFileName))
        assert new File(roddyBamFile.workDirectory, "dir").mkdirs()

        assert executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
chmod 777 ${roddyBamFile.workDirectory}/dir
chmod 777 ${roddyBamFile.workDirectory}/file
chmod 777 ${roddyBamFile.workDirectory}/${roddyBamFile.baiFileName}
chmod 777 ${roddyBamFile.workDirectory}/${roddyBamFile.bamFileName}
""").empty


        executeRoddyCommandService.correctPermissions(roddyBamFile, realm)


        assert executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
stat -c %a ${roddyBamFile.workDirectory}/dir
stat -c %a ${roddyBamFile.workDirectory}/file
stat -c %a ${roddyBamFile.workDirectory}/${roddyBamFile.baiFileName}
stat -c %a ${roddyBamFile.workDirectory}/${roddyBamFile.bamFileName}
""") == """\
2750
440
444
444
"""
    }

    @Test
    void testCorrectPermission_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctPermissions(null, realm)
        }.contains('RoddyBamFile')
    }

    @Test
    void testCorrectGroup_AllFine() {
        String primaryGroup = waitForCommand("id -g -n").stdout.trim()
        executeRoddyCommandService.executionService = [
                executeCommandReturnProcessOutput: { Realm realm1, String cmd, String user ->
                    assert realm1 == realm
                    assert user == realm.roddyUser
                    ProcessOutput out =  waitForCommand(cmd)
                    assert out.stderrEmptyAndExitCodeZero
                    assert out.stdout == """
correct group permission to ${primaryGroup}
./file
""" as String
                    return out
                }
        ] as ExecutionService

        String testingGroup = grailsApplication.config.otp.testing.group
        assert testingGroup: '"otp.testing.group" is not set in your "otp.properties". Please add it with an existing secondary group.'

        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, "file"))
        assert executeAndAssertExitCodeAndErrorOutAndReturnStdout("chgrp ${testingGroup} ${roddyBamFile.workDirectory}/file").empty

        executeRoddyCommandService.correctGroups(roddyBamFile, realm)


        assert executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
stat -c %G ${roddyBamFile.workDirectory}/file
""") == """\
${primaryGroup}
""" as String
    }

    @Test
    void testCorrectGroup_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctGroups(null, realm)
        }.contains('RoddyBamFile')
    }


    void helperTestDeleteContentOfOtherUnixUserDirectory_AllFine(boolean fileExist, Closure checkCallback) {
        executeRoddyCommandService.executionService = [
                executeCommandReturnProcessOutput: { Realm realm1, String cmd, String user ->
                    assert realm1 == realm
                    assert user == realm.roddyUser
                    ProcessOutput out =  waitForCommand(cmd)
                    assert out.stderrEmptyAndExitCodeZero
                    assert out.stdout.startsWith("\ndelete ${user} directory content of ${roddyBamFile.workDirectory}\n")
                    checkCallback(out.stdout)
                    return out
                }
        ] as ExecutionService

        File ownDir = new File(roddyBamFile.workDirectory, "ownDir")
        assert ownDir.mkdirs()
        File ownFileInDir = CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, "ownFileInDir"))
        File ownFile = CreateFileHelper.createFile(new File(ownDir, "ownFile"))

        //For a real test it would be necessary to create dirs and files as another user

        executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(roddyBamFile.workDirectory, realm)

        assert fileExist == ownFileInDir.exists()
        assert fileExist == ownFile.exists()
    }

    @Test
    void testDeleteContentOfOtherUnixUserDirectory_AllFineCorrectUser() {
        helperTestDeleteContentOfOtherUnixUserDirectory_AllFine(false) { String output ->
            assert output.contains("delete content of .")
        }
    }

    @Test
    void testDeleteContentOfOtherUnixUserDirectory_AllFineWrongUser() {
        realm.roddyUser = grailsApplication.config.otp.testing.workflows.account
        realm.save(flush: true)
        helperTestDeleteContentOfOtherUnixUserDirectory_AllFine(true) { String output ->
            assert !output.contains("delete content of")
        }
    }

    @Test
    void testDeleteContentOfOtherUnixUserDirectory_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.deleteContentOfOtherUnixUserDirectory(null, realm)
        }.contains('basePath is not allowed to be null')
    }
}
