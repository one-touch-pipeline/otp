package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import org.codehaus.groovy.grails.commons.*
import org.junit.*
import org.junit.rules.*

import static de.dkfz.tbi.otp.utils.ProcessHelperService.*

class ExecuteRoddyCommandServiceTests {

    GrailsApplication grailsApplication

    ExecuteRoddyCommandService executeRoddyCommandService

    ExecutionService executionService

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

    @Before
    void setUp() {
        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        DomainFactory.createProcessingOptionLazy([
                name: OptionName.OTP_USER_LINUX_GROUP,
                value: TestConfigHelper.testingGroup(grailsApplication),
                type: null,
        ])

        roddyPath = new File(ProcessingOptionService.getValueOfProcessingOption(OptionName.RODDY_PATH))
        roddyCommand = new File(roddyPath, 'roddy.sh')
        tmpOutputDir = temporaryFolder.newFolder("temporaryOutputDir")

        roddyBamFile = DomainFactory.createRoddyBamFile()

        realm = DomainFactory.createRealmDataManagementDKFZ([
                name              : roddyBamFile.project.realmName,
                rootPath          : "${tmpOutputDir}/root",
                processingRootPath: "${tmpOutputDir}/processing",
        ])
        assert realm.save(flush: true)

        roddyBaseConfigsPath = new File(ProcessingOptionService.getValueOfProcessingOption(OptionName.RODDY_BASE_CONFIGS_PATH))
        roddyBaseConfigsPath.mkdirs()
        new File(roddyBaseConfigsPath, "file name").write("file content")
        applicationIniPath = new File(ProcessingOptionService.getValueOfProcessingOption(OptionName.RODDY_APPLICATION_INI))
        assert CreateFileHelper.createFile(applicationIniPath)
        featureTogglesConfigPath = new File(ProcessingOptionService.getValueOfProcessingOption(OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH))
    }

    @After
    void tearDown() {
        roddyPath = null
        tmpOutputDir = null
        realm = null
        roddyBamFile = null
        TestCase.removeMetaClass(ExecutionService, executeRoddyCommandService.executionService)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, executeRoddyCommandService)
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
        executeRoddyCommandService.executionService = executionService
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
        String expectedCmd = "${roddyPath}/roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} "
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
        DomainFactory.createRoddyAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = SeqType.wholeGenomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqType.wholeGenomePairedSeqType.roddyName == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_SeqTypeEXOME() {
        DomainFactory.createRoddyAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = SeqType.exomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqType.exomePairedSeqType.roddyName == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_DifferentSeqType_ShouldFail() {
        DomainFactory.createRoddyAlignableSeqTypes()
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

        ProcessingOption.findByName(OptionName.RODDY_PATH).delete(flush: true)
        assert !ProcessingOption.findByName(OptionName.RODDY_PATH)
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyBaseConfigsPathIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName(OptionName.RODDY_BASE_CONFIGS_PATH).delete(flush: true)
        assert !ProcessingOption.findByName(OptionName.RODDY_BASE_CONFIGS_PATH)
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName(OptionName.RODDY_APPLICATION_INI).delete(flush: true)
        assert !ProcessingOption.findByName(OptionName.RODDY_APPLICATION_INI)
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

        String expectedCmd =
                "${roddyCommand} rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
                "${roddyBamFile.individual.pid} " +
                "--useconfig=${applicationIniPath} " +
                "--usefeaturetoggleconfig=${featureTogglesConfigPath} " +
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
        String expectedCmd =
                "${roddyCommand} printidlessruntimeconfig ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
                "--useconfig=${applicationIniPath} " +
                "--usefeaturetoggleconfig=${featureTogglesConfigPath} " +
                "--usePluginVersion=${roddyBamFile.config.pluginVersion} " +
                "--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} "

        LogThreadLocal.withThreadLog(System.out) {
            String actualCmd = executeRoddyCommandService.roddyGetRuntimeConfigCommand(roddyBamFile.config, CONFIG_NAME, ANALYSIS_ID)
            assert expectedCmd == actualCmd
        }
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
            ['bash', '-c', command].execute().waitFor()
        }
        assert tmpOutputDir.delete()

        //when:
        executeRoddyCommandService.createWorkOutputDirectory(realm, tmpOutputDir)

        //then
        assert tmpOutputDir.exists()

        String permissionAndGroup = executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${tmpOutputDir}
            stat -c %G ${tmpOutputDir}
            """.stripIndent())
        //make the 2 optional, since it does not work for all developer, allow group jenkins, since user jenkins is not part of jenkins
        String expected = """\
            2?770
            (${ProcessingOptionService.getValueOfProcessingOption(OptionName.OTP_USER_LINUX_GROUP)}|jenkins)
            """.stripIndent()

        assert permissionAndGroup ==~ expected
    }


    @Test
    void testCreateWorkOutputDirectory_DirectoryAlreadyExist_AllFine() {
        executeRoddyCommandService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            ['bash', '-c', command].execute().waitFor()
        }
        assert tmpOutputDir.exists()

        //when:
        executeRoddyCommandService.createWorkOutputDirectory(realm, tmpOutputDir)

        //then:
        String permissionAndGroup = executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${tmpOutputDir}
            stat -c %G ${tmpOutputDir}
            """.stripIndent())
        //make the 2 optional, since it does not work for all developer, allow group jenkins, since user jenkins is not part of jenkins
        String expected = """\
            2?770
            (${ProcessingOptionService.getValueOfProcessingOption(OptionName.OTP_USER_LINUX_GROUP)}|jenkins)
            """.stripIndent()

        assert permissionAndGroup ==~ expected
    }

    @Test
    void testCorrectPermission_AllOkay() {
        executeRoddyCommandService.executionService = [
                executeCommandReturnProcessOutput: { Realm realm1, String cmd ->
                    assert realm1 == realm
                    ProcessOutput out = executeAndWait(cmd)
                    out.assertExitCodeZeroAndStderrEmpty()
                    assert out.stdout == """\

                        correct directory permission
                        2

                        correct file permission for non bam/bai files
                        1

                        correct file permission for bam/bai files
                        2
                        """.stripIndent()
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
            """.stripIndent()).empty


        executeRoddyCommandService.correctPermissions(roddyBamFile, realm)


        String expected = """\
            2750
            440
            444
            444
            """.stripIndent()

        assert expected == executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${roddyBamFile.workDirectory}/dir
            stat -c %a ${roddyBamFile.workDirectory}/file
            stat -c %a ${roddyBamFile.workDirectory}/${roddyBamFile.baiFileName}
            stat -c %a ${roddyBamFile.workDirectory}/${roddyBamFile.bamFileName}
            """.stripIndent())
    }

    @Test
    void testCorrectPermission_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctPermissions(null, realm)
        }.contains('roddyResult')
    }

    @Test
    void testCorrectGroup_AllFine() {
        String primaryGroup = TestCase.primaryGroup()
        executeRoddyCommandService.executionService = [
                executeCommandReturnProcessOutput: { Realm realm1, String cmd ->
                    assert realm1 == realm
                    ProcessOutput out = executeAndWait(cmd)
                    out.assertExitCodeZeroAndStderrEmpty()
                    assert out.stdout == """\

                        correct group permission to ${primaryGroup}
                        1
                        """.stripIndent() as String
                    return out
                }
        ] as ExecutionService

        String testingGroup = TestConfigHelper.testingGroup(grailsApplication)

        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, "file"))
        assert executeAndAssertExitCodeAndErrorOutAndReturnStdout("chgrp ${testingGroup} ${roddyBamFile.workDirectory}/file").empty

        executeRoddyCommandService.correctGroups(roddyBamFile, realm)


        assert "${primaryGroup}\n" as String == executeAndAssertExitCodeAndErrorOutAndReturnStdout(
                "stat -c %G ${roddyBamFile.workDirectory}/file"
        )
    }

    @Test
    void testCorrectGroup_BamFileIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctGroups(null, realm)
        }.contains('roddyResult')
    }

    @Test
    void testCorrectPermissionsAndGroups() {
        String primaryGroup = TestCase.primaryGroup()
        String group = TestConfigHelper.testingGroup(grailsApplication)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        assert executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            chmod -R 777 ${roddyBamFile.workDirectory}
            chgrp -R ${group} ${roddyBamFile.workDirectory}
            chgrp ${primaryGroup} ${roddyBamFile.baseDirectory}
            """.stripIndent()).empty

        executeRoddyCommandService.executionService.metaClass.executeCommandReturnProcessOutput = { Realm realm1, String cmd, String user ->
            ProcessOutput out = executeAndWait(cmd)
            out.assertExitCodeZeroAndStderrEmpty()
            return out
        }
        executeRoddyCommandService.executionService.metaClass.executeCommandReturnProcessOutput = { Realm realm1, String cmd ->
            ProcessOutput out = executeAndWait(cmd)
            out.assertExitCodeZeroAndStderrEmpty()
            return out
        }

        executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile, realm)


        String value = executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${roddyBamFile.workDirectory.path}
            stat -c %a ${roddyBamFile.workMergedQADirectory.path}
            stat -c %a ${roddyBamFile.workBamFile.path}
            stat -c %a ${roddyBamFile.workBaiFile.path}
            stat -c %a ${roddyBamFile.workMergedQAJsonFile.path}

            stat -c %G ${roddyBamFile.workDirectory.path}
            stat -c %G ${roddyBamFile.workMergedQADirectory.path}
            stat -c %G ${roddyBamFile.workBamFile.path}
            stat -c %G ${roddyBamFile.workBaiFile.path}
            stat -c %G ${roddyBamFile.workMergedQAJsonFile.path}
            """.stripIndent()
        )

        //On some computers the group id are not set for unknown reasons.
        //Therefore allow for the test also the other case
        String expected = """\
            2?750
            2?750
            444
            444
            440
            ${primaryGroup}
            ${primaryGroup}
            ${primaryGroup}
            ${primaryGroup}
            ${primaryGroup}
            """.stripIndent()

        assert value ==~ expected
    }
}
