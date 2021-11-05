/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
class ExecuteRoddyCommandServiceIntegrationTests {

    static final private String LOAD_MODULE = "LOAD MODULE"

    static final private String ACTIVATE_JAVA = "ACTIVATE JAVA"

    static final private String ACTIVATE_GROOVY = "ACTIVATE GROOVY"

    static final private String INIT_MODULES = "${LOAD_MODULE}\n${ACTIVATE_JAVA}\n${ACTIVATE_GROOVY}\n"


    ExecuteRoddyCommandService executeRoddyCommandService

    RemoteShellHelper remoteShellHelper
    ProcessingOptionService processingOptionService

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()


    static final String CONFIG_NAME = "WORKFLOW_VERSION"
    static final String ANALYSIS_ID = "WHOLE_GENOME"

    static final String RODDY_EXECUTION_DIR_NAME_1 = "exec_000000_000000000_a_a"
    static final String RODDY_EXECUTION_DIR_NAME_2 = "exec_000000_000000000_b_b"

    TestConfigService configService
    File roddyPath
    File roddyCommand
    File tmpOutputDir
    Realm realm
    RoddyBamFile roddyBamFile
    File roddyBaseConfigsPath
    File applicationIniPath
    File featureTogglesConfigPath

    void setupData() {
        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        DomainFactory.createProcessingOptionLazy([
                name: OptionName.OTP_USER_LINUX_GROUP,
                value: configService.testingGroup,
                type: null,
        ])

        roddyPath = new File(processingOptionService.findOptionAsString(OptionName.RODDY_PATH))
        roddyCommand = new File(roddyPath, 'roddy.sh')
        tmpOutputDir = temporaryFolder.newFolder("temporaryOutputDir")
        roddyBamFile = DomainFactory.createRoddyBamFile()
        configService.addOtpProperties(temporaryFolder.newFolder().toPath())
        realm = roddyBamFile.project.realm
        assert realm.save(flush: true)

        roddyBaseConfigsPath = new File(processingOptionService.findOptionAsString(OptionName.RODDY_BASE_CONFIGS_PATH))
        roddyBaseConfigsPath.mkdirs()
        new File(roddyBaseConfigsPath, "file name").write("file content")
        applicationIniPath = new File(processingOptionService.findOptionAsString(OptionName.RODDY_APPLICATION_INI))
        assert CreateFileHelper.createFile(applicationIniPath)
        featureTogglesConfigPath = new File(processingOptionService.findOptionAsString(OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH))

        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
    }

    @After
    void tearDown() {
        configService.clean()
        roddyPath = null
        tmpOutputDir = null
        realm = null
        roddyBamFile = null
        TestCase.removeMetaClass(RemoteShellHelper, executeRoddyCommandService.remoteShellHelper)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, executeRoddyCommandService)
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
        executeRoddyCommandService.remoteShellHelper = remoteShellHelper
    }

    @Test
    void testRoddyBaseCommand_RoddyPathIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(null, CONFIG_NAME, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)
        }.contains("roddyPath is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_ConfigNameIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(roddyPath, null, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)
        }.contains("configName is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_AnalysisIdIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(roddyPath, CONFIG_NAME, null, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)
        }.contains("analysisId is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_AllFine() {
        setupData()
        String expectedCmd = "${roddyPath}/roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID}"
        String actualCmd = executeRoddyCommandService.roddyBaseCommand(roddyPath, CONFIG_NAME, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)
        assert expectedCmd == actualCmd
    }

    @Test
    void testGetAnalysisIDinConfigFile_InputIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(null)
        }.contains("The input roddyResult must not be null")
    }

    @Test
    void testGetAnalysisIDinConfigFile_ObjectDoesNotHaveGetSeqTypeMethod_ShouldFail() {
        setupData()
        roddyBamFile.metaClass.getSeqType = { -> null }
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        }.contains("There is not seqType available")
    }

    @Test
    void testGetAnalysisIDinConfigFile_SeqTypeWGS() {
        setupData()
        DomainFactory.createRoddyAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = SeqTypeService.wholeGenomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqTypeService.wholeGenomePairedSeqType.roddyName == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_SeqTypeEXOME() {
        setupData()
        DomainFactory.createRoddyAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = SeqTypeService.exomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqTypeService.exomePairedSeqType.roddyName == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_DifferentSeqType_ShouldFail() {
        setupData()
        DomainFactory.createRoddyAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = DomainFactory.createSeqType(name: "differentSeqType")
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert TestCase.shouldFail(RuntimeException) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        }.contains("The seqType ${roddyBamFile.seqType} can not be processed at the moment")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ObjectIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(null, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("The input roddyResult is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_NameInConfigFileIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, null, ANALYSIS_ID, realm)
        }.contains("The input nameInConfigFile is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_AnalysisIDinConfigFileIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, null, realm)
        }.contains("The input analysisIDinConfigFile is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_RealmIsNull_ShouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, null)
        }.contains("The input realm is not allowed to be null")
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniDoesNotExistInFilesystem_ShouldFail() {
        setupData()
        executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }
        assert applicationIniPath.delete()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains(applicationIniPath.path)
    }

    @Test
    void testDefaultRoddyExecutionCommand_BaseConfigFolderDoesNotExistInFilesystem_ShouldFail() {
        setupData()
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
                INIT_MODULES +
                "${roddyCommand} rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
                "${roddyBamFile.individual.pid} " +
                "--useconfig=${applicationIniPath} " +
                "--usefeaturetoggleconfig=${featureTogglesConfigPath} " +
                "--usePluginVersion=${roddyBamFile.config.programVersion} " +
                "--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath}," +
                "${roddyBaseConfigsPath}/${ExecuteRoddyCommandService.RESOURCE_PATH}/${roddyBamFile.project.realm.jobScheduler.toString().toLowerCase()} " +
                "--useiodir=${viewByPid},${roddyBamFile.workDirectory}"

        LogThreadLocal.withThreadLog(System.out) {
            String actualCmd = executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
            assert expectedCmd == actualCmd
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_firstRun_AllFine() {
        setupData()
        initRoddyModule()
        helperFor_testDefaultRoddyExecutionCommand_AllFine()

        assert roddyBamFile.roddyExecutionDirectoryNames.empty
    }

    @Test
    void testDefaultRoddyExecutionCommand_restartWithoutDeletedWorkDirectory_AllFine() {
        setupData()
        initRoddyModule()
        roddyBamFile.roddyExecutionDirectoryNames = [
                RODDY_EXECUTION_DIR_NAME_1,
        ]
        roddyBamFile.save(flush: true)
        assert roddyBamFile.workDirectory.mkdirs()

        helperFor_testDefaultRoddyExecutionCommand_AllFine()

        assert roddyBamFile.roddyExecutionDirectoryNames.size() == 1
        assert [RODDY_EXECUTION_DIR_NAME_1] == roddyBamFile.roddyExecutionDirectoryNames
    }

    @Test
    void testDefaultRoddyExecutionCommand_restartWithDeletedWorkDirectory_AllFine() {
        setupData()
        initRoddyModule()
        roddyBamFile.roddyExecutionDirectoryNames = [
                RODDY_EXECUTION_DIR_NAME_1,
                RODDY_EXECUTION_DIR_NAME_2,
        ]
        roddyBamFile.save(flush: true)

        helperFor_testDefaultRoddyExecutionCommand_AllFine()

        assert roddyBamFile.roddyExecutionDirectoryNames.empty
    }

    @Test
    void testRoddyGetRuntimeConfigCommand_AllFine() {
        setupData()
        initRoddyModule()
        String expectedCmd =
                INIT_MODULES +
                "${roddyCommand} printidlessruntimeconfig ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
                "--useconfig=${applicationIniPath} " +
                "--usefeaturetoggleconfig=${featureTogglesConfigPath} " +
                "--usePluginVersion=${roddyBamFile.config.programVersion} " +
                "--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath}"

        LogThreadLocal.withThreadLog(System.out) {
            String actualCmd = executeRoddyCommandService.roddyGetRuntimeConfigCommand(roddyBamFile.config, CONFIG_NAME, ANALYSIS_ID)
            assert expectedCmd == actualCmd
        }
    }

    @Test
    void testCreateWorkOutputDirectory_RealmIsNull_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createWorkOutputDirectory(null, tmpOutputDir)
        }
    }

    @Test
    void testCreateWorkOutputDirectory_FileIsNull_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createWorkOutputDirectory(realm, null)
        }
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @Test
    void testCreateWorkOutputDirectory_DirectoryCreationFailed_ShouldFail() {
        setupData()
        executeRoddyCommandService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command -> }
        tmpOutputDir.absoluteFile.delete()
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createWorkOutputDirectory(realm, tmpOutputDir)
        }
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @Test
    void testCreateWorkOutputDirectory_AllFine() {
        setupData()
        executeRoddyCommandService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command ->
            ['bash', '-c', command].execute().waitFor()
        }
        assert tmpOutputDir.delete()

        //when:
        executeRoddyCommandService.createWorkOutputDirectory(realm, tmpOutputDir)

        //then
        assert tmpOutputDir.exists()

        String permissionAndGroup = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${tmpOutputDir}
            stat -c %G ${tmpOutputDir}
            """.stripIndent())
        //make the 2 optional, since it does not work for all developers.
        String expected = """\
            2?750
            ${processingOptionService.findOptionAsString(OptionName.OTP_USER_LINUX_GROUP)}
            """.stripIndent()

        assert permissionAndGroup ==~ expected
    }

    @Test
    void testCreateWorkOutputDirectory_DirectoryAlreadyExist_AllFine() {
        setupData()
        executeRoddyCommandService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command ->
            ['bash', '-c', command].execute().waitFor()
        }
        assert tmpOutputDir.exists()

        //when:
        executeRoddyCommandService.createWorkOutputDirectory(realm, tmpOutputDir)

        //then:
        String permissionAndGroup = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${tmpOutputDir}
            stat -c %G ${tmpOutputDir}
            """.stripIndent())
        //make the 2 optional, since it does not work for all developers.
        String expected = """\
            2?750
            ${processingOptionService.findOptionAsString(OptionName.OTP_USER_LINUX_GROUP)}
            """.stripIndent()

        assert permissionAndGroup ==~ expected
    }

    @Test
    void testCorrectPermission_AllOkay() {
        setupData()
        executeRoddyCommandService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm1, String cmd ->
                    assert realm1 == realm
                    ProcessOutput out = LocalShellHelper.executeAndWait(cmd)
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
        ] as RemoteShellHelper

        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, "file"))
        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, roddyBamFile.baiFileName))
        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, roddyBamFile.bamFileName))
        assert new File(roddyBamFile.workDirectory, "dir").mkdirs()

        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
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

        assert expected == LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${roddyBamFile.workDirectory}/dir
            stat -c %a ${roddyBamFile.workDirectory}/file
            stat -c %a ${roddyBamFile.workDirectory}/${roddyBamFile.baiFileName}
            stat -c %a ${roddyBamFile.workDirectory}/${roddyBamFile.bamFileName}
            """.stripIndent())
    }

    @Test
    void testCorrectPermission_BamFileIsNull_shouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctPermissions(null, realm)
        }.contains('roddyResult')
    }

    @Test
    void testCorrectGroup_AllFine() {
        setupData()

        // test data in temp-folder will be created with primary group, which is...
        String primaryGroup = TestConfigService.primaryGroup

        executeRoddyCommandService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm1, String cmd ->
                    assert realm1 == realm
                    ProcessOutput out = LocalShellHelper.executeAndWait(cmd)
                    out.assertExitCodeZeroAndStderrEmpty()
                    assert out.stdout == """\

                        correct group permission to ${primaryGroup}
                        1
                        """.stripIndent() as String
                    return out
                }
        ] as RemoteShellHelper

        String testingGroup = configService.testingGroup

        CreateFileHelper.createFile(new File(roddyBamFile.workDirectory, "file"))
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chgrp -h ${testingGroup} ${roddyBamFile.workDirectory}/file").empty

        executeRoddyCommandService.correctGroups(roddyBamFile, realm)

        assert "${primaryGroup}\n" as String == LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(
                "stat -c %G ${roddyBamFile.workDirectory}/file"
        )
    }

    @Test
    void testCorrectGroup_BamFileIsNull_shouldFail() {
        setupData()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.correctGroups(null, realm)
        }.contains('roddyResult')
    }

    @Test
    void testCorrectPermissionsAndGroups() {
        setupData()
        String primaryGroup = configService.workflowProjectUnixGroup
        String group = configService.testingGroup

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            chgrp -hR ${group} ${roddyBamFile.workDirectory}
            chgrp -h ${primaryGroup} ${roddyBamFile.baseDirectory}
            chmod -R 777 ${roddyBamFile.workDirectory}
            """.stripIndent()).empty

        executeRoddyCommandService.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm realm1, String cmd, String user ->
            ProcessOutput out = LocalShellHelper.executeAndWait(cmd)
            out.assertExitCodeZeroAndStderrEmpty()
            return out
        }
        executeRoddyCommandService.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm realm1, String cmd ->
            ProcessOutput out = LocalShellHelper.executeAndWait(cmd)
            out.assertExitCodeZeroAndStderrEmpty()
            return out
        }

        executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile, realm)

        String value = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
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

    private void initRoddyModule() {
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, LOAD_MODULE)
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA, ACTIVATE_JAVA)
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, ACTIVATE_GROOVY)
    }
}
