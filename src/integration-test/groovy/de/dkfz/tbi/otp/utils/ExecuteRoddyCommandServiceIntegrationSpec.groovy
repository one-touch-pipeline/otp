/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import java.nio.file.*

@Rollback
@Integration
class ExecuteRoddyCommandServiceIntegrationSpec extends Specification {

    static final private String LOAD_MODULE = "LOAD MODULE"

    static final private String ACTIVATE_JAVA = "ACTIVATE JAVA"

    static final private String ACTIVATE_GROOVY = "ACTIVATE GROOVY"

    static final private String INIT_MODULES = "${LOAD_MODULE}\n${ACTIVATE_JAVA}\n${ACTIVATE_GROOVY}\n"

    ExecuteRoddyCommandService executeRoddyCommandService

    RemoteShellHelper remoteShellHelper
    ProcessingOptionService processingOptionService

    @TempDir
    Path tempDir

    static final String CONFIG_NAME = "WORKFLOW_VERSION"
    static final String ANALYSIS_ID = "WHOLE_GENOME"

    static final String RODDY_EXECUTION_DIR_NAME_1 = "exec_000000_000000000_a_a"
    static final String RODDY_EXECUTION_DIR_NAME_2 = "exec_000000_000000000_b_b"

    TestConfigService configService
    File roddyPath
    File roddyCommand
    File tmpOutputDir
    RoddyBamFile roddyBamFile
    File roddyBaseConfigsPath
    File applicationIniPath
    File featureTogglesConfigPath
    JobScheduler scheduler

    void setupData() {
        DomainFactory.createRoddyProcessingOptions(tempDir.toFile())

        DomainFactory.createProcessingOptionLazy([
                name: OptionName.OTP_USER_LINUX_GROUP,
                value: configService.testingGroup,
                type: null,
        ])

        roddyPath = new File(processingOptionService.findOptionAsString(OptionName.RODDY_PATH))
        roddyCommand = new File(roddyPath, 'roddy.sh')
        tmpOutputDir = Files.createDirectories(tempDir.resolve("temporaryOutputDir")).toFile()
        roddyBamFile = DomainFactory.createRoddyBamFile()
        configService.addOtpProperties(tempDir)

        roddyBaseConfigsPath = new File(processingOptionService.findOptionAsString(OptionName.RODDY_BASE_CONFIGS_PATH))
        roddyBaseConfigsPath.mkdirs()
        new File(roddyBaseConfigsPath, "file name").write("file content")
        applicationIniPath = new File(processingOptionService.findOptionAsString(OptionName.RODDY_APPLICATION_INI))
        assert CreateFileHelper.createFile(applicationIniPath)
        featureTogglesConfigPath = new File(processingOptionService.findOptionAsString(OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH))
        scheduler = JobScheduler.PBS

        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        executeRoddyCommandService.individualService = new IndividualService()
    }

    void cleanup() {
        configService.clean()
        roddyPath = null
        tmpOutputDir = null
        roddyBamFile = null
        executeRoddyCommandService.remoteShellHelper = remoteShellHelper
    }

    void "test roddyBaseCommand_RoddyPathIsNull_ShouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.roddyBaseCommand(null, CONFIG_NAME, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("roddyPath is not allowed to be null")
    }

    void "test roddyBaseCommand_ConfigNameIsNull_ShouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.roddyBaseCommand(roddyPath, null, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("configName is not allowed to be null")
    }

    void "test roddyBaseCommand_AnalysisIdIsNull_ShouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.roddyBaseCommand(roddyPath, CONFIG_NAME, null, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("analysisId is not allowed to be null")
   }

    void "test roddyBaseCommand_AllFine"() {
        given:
        setupData()

        expect:
        executeRoddyCommandService.roddyBaseCommand(roddyPath, CONFIG_NAME, ANALYSIS_ID, ExecuteRoddyCommandService.RoddyInvocationType.EXECUTE) ==
                "${roddyPath}/roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID}"
    }

    void "test getAnalysisIDinConfigFile_InputIsNull_ShouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.getAnalysisIDinConfigFile(null)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("The input roddyResult must not be null")
    }

    void "test getAnalysisIDinConfigFile_ObjectDoesNotHaveGetSeqTypeMethod_ShouldFail"() {
        given:
        setupData()
        roddyBamFile.workPackage.seqType = null

        when:
        executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("There is not seqType available")
    }

    void "test getAnalysisIDinConfigFile_SeqTypeWGS"() {
        given:
        setupData()
        DomainFactory.createRoddyAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = SeqTypeService.wholeGenomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        expect:
        SeqTypeService.wholeGenomePairedSeqType.roddyName == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    void "test getAnalysisIDinConfigFile_SeqTypeEXOME"() {
        given:
        setupData()
        DomainFactory.createRoddyAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = SeqTypeService.exomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        expect:
        SeqTypeService.exomePairedSeqType.roddyName == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    void "test getAnalysisIDinConfigFile_DifferentSeqType_ShouldFail"() {
        given:
        setupData()
        DomainFactory.createRoddyAlignableSeqTypes()
        roddyBamFile.mergingWorkPackage.seqType = DomainFactory.createSeqType(name: "differentSeqType")
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        when:
        executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)

        then:
        Throwable e = thrown(RuntimeException)
        e.message.contains("The seqType ${roddyBamFile.seqType} can not be processed at the moment")
    }

    void "test defaultRoddyExecutionCommand_ObjectIsNull_ShouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.defaultRoddyExecutionCommand(null, CONFIG_NAME, ANALYSIS_ID)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("The input roddyResult is not allowed to be null")
    }

    void "test defaultRoddyExecutionCommand_NameInConfigFileIsNull_ShouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, null, ANALYSIS_ID)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("The input nameInConfigFile is not allowed to be null")
    }

    void "test defaultRoddyExecutionCommand_AnalysisIDinConfigFileIsNull_ShouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, null)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("The input analysisIDinConfigFile is not allowed to be null")
    }

    // false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "test defaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniDoesNotExistInFilesystem_ShouldFail"() {
        given:
        setupData()

        executeRoddyCommandService = Spy(ExecuteRoddyCommandService)
        executeRoddyCommandService.createWorkOutputDirectory(_) >> { File file -> }
        executeRoddyCommandService.individualService = Mock(IndividualService) {
            getViewByPidPathBase(_, _) >> { Individual individual, SeqType seqType ->
                Paths.get("/view-by-pid-path")
            }
        }
        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        executeRoddyCommandService.configService = Mock(ConfigService) {
            getJobScheduler() >> JobScheduler.PBS
        }

        assert applicationIniPath.delete()

        when:
        executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains(applicationIniPath.path)
    }

    void "test defaultRoddyExecutionCommand_BaseConfigFolderDoesNotExistInFilesystem_ShouldFail"() {
        given:
        setupData()
        executeRoddyCommandService = Spy(ExecuteRoddyCommandService)
        executeRoddyCommandService.createWorkOutputDirectory(_) >> { File file -> }
        executeRoddyCommandService.individualService = Mock(IndividualService) {
            getViewByPidPathBase(_, _) >> { Individual individual, SeqType seqType ->
                Paths.get("/view-by-pid-path")
            }
        }
        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        executeRoddyCommandService.configService = Mock(ConfigService) {
            getJobScheduler() >> JobScheduler.PBS
        }

        assert roddyBaseConfigsPath.deleteDir()

        when:
        executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains(roddyBaseConfigsPath.path)
    }

    private String getExpectedCmd() {
        return INIT_MODULES +
                "${roddyCommand} rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
                "${roddyBamFile.individual.pid} " +
                "--useconfig=${applicationIniPath} " +
                "--usefeaturetoggleconfig=${featureTogglesConfigPath} " +
                "--usePluginVersion=${roddyBamFile.config.programVersion} " +
                "--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath}," +
                "${roddyBaseConfigsPath}/${ExecuteRoddyCommandService.RESOURCE_PATH}/${scheduler.toString().toLowerCase()} " +
                "--useiodir=/view-by-pid-path,${roddyBamFile.workDirectory}"
    }

    void "test defaultRoddyExecutionCommand_firstRun_AllFine"() {
        given:
        setupData()
        initRoddyModule()
        executeRoddyCommandService = Spy(ExecuteRoddyCommandService)
        executeRoddyCommandService.createWorkOutputDirectory(_) >> { File file -> }
        executeRoddyCommandService.individualService = Mock(IndividualService) {
            getViewByPidPathBase(_, _) >> { Individual individual, SeqType seqType ->
                Paths.get("/view-by-pid-path")
            }
        }
        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        executeRoddyCommandService.configService = Mock(ConfigService) {
            getJobScheduler() >> scheduler
        }

        when:
        String actualCmd = LogThreadLocal.withThreadLog(System.out) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID)
        }

        then:
        expectedCmd == actualCmd
        roddyBamFile.roddyExecutionDirectoryNames.empty
    }

    void "test defaultRoddyExecutionCommand_restartWithoutDeletedWorkDirectory_AllFine"() {
        given:
        setupData()
        initRoddyModule()
        roddyBamFile.roddyExecutionDirectoryNames = [
                RODDY_EXECUTION_DIR_NAME_1,
        ]
        roddyBamFile.save(flush: true)
        assert roddyBamFile.workDirectory.mkdirs()

        executeRoddyCommandService = Spy(ExecuteRoddyCommandService)
        executeRoddyCommandService.createWorkOutputDirectory(_) >> { File file -> }
        executeRoddyCommandService.individualService = Mock(IndividualService) {
            getViewByPidPathBase(_, _) >> { Individual individual, SeqType seqType ->
                Paths.get("/view-by-pid-path")
            }
        }
        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        executeRoddyCommandService.configService = Mock(ConfigService) {
            getJobScheduler() >> scheduler
        }

        when:
        String actualCmd = LogThreadLocal.withThreadLog(System.out) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID)
        }

        then:
        expectedCmd == actualCmd
        roddyBamFile.roddyExecutionDirectoryNames.size() == 1
        [RODDY_EXECUTION_DIR_NAME_1] == roddyBamFile.roddyExecutionDirectoryNames
    }

    void "test defaultRoddyExecutionCommand_restartWithDeletedWorkDirectory_AllFine"() {
        given:
        setupData()
        initRoddyModule()
        roddyBamFile.roddyExecutionDirectoryNames = [
                RODDY_EXECUTION_DIR_NAME_1,
                RODDY_EXECUTION_DIR_NAME_2,
        ]
        roddyBamFile.save(flush: true)

        executeRoddyCommandService = Spy(ExecuteRoddyCommandService)
        executeRoddyCommandService.createWorkOutputDirectory(_) >> { File file -> }
        executeRoddyCommandService.individualService = Mock(IndividualService) {
            getViewByPidPathBase(_, _) >> { Individual individual, SeqType seqType ->
                Paths.get("/view-by-pid-path")
            }
        }
        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        executeRoddyCommandService.configService = Mock(ConfigService) {
            getJobScheduler() >> scheduler
        }

        when:
        String actualCmd = LogThreadLocal.withThreadLog(System.out) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID)
        }

        then:
        expectedCmd == actualCmd
        roddyBamFile.roddyExecutionDirectoryNames.empty
    }

    void "test roddyGetRuntimeConfigCommand_AllFine"() {
        given:
        setupData()
        initRoddyModule()
        String expectedCmd =
                INIT_MODULES +
                "${roddyCommand} printidlessruntimeconfig ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
                "--useconfig=${applicationIniPath} " +
                "--usefeaturetoggleconfig=${featureTogglesConfigPath} " +
                "--usePluginVersion=${roddyBamFile.config.programVersion} " +
                "--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath}"

        when:
        String actualCmd = LogThreadLocal.withThreadLog(System.out) {
            executeRoddyCommandService.roddyGetRuntimeConfigCommand(roddyBamFile.config, CONFIG_NAME, ANALYSIS_ID)
        }

        then:
        expectedCmd == actualCmd
    }

    void "test createWorkOutputDirectory_FileIsNull_ShouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.createWorkOutputDirectory(null)

        then:
        thrown(AssertionError)
    }

    // false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "test createWorkOutputDirectory_DirectoryCreationFailed_ShouldFail"() {
        given:
        setupData()
        executeRoddyCommandService = new ExecuteRoddyCommandService()
        executeRoddyCommandService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommand(_) >> { String command -> }
        }
        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        tmpOutputDir.absoluteFile.delete()

        when:
        executeRoddyCommandService.createWorkOutputDirectory(tmpOutputDir)

        then:
        thrown(AssertionError)
    }

    // false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "test createWorkOutputDirectory_AllFine"() {
        given:
        setupData()
        executeRoddyCommandService = new ExecuteRoddyCommandService()
        executeRoddyCommandService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommand(_) >> { String command ->
                ['bash', '-c', command].execute().waitFor()
            }
        }
        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        assert tmpOutputDir.delete()

        when:
        executeRoddyCommandService.createWorkOutputDirectory(tmpOutputDir)

        then:
        tmpOutputDir.exists()

        String permissionAndGroup = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${tmpOutputDir}
            stat -c %G ${tmpOutputDir}
            """.stripIndent())
        // make the 2 optional, since it does not work for all developers.
        String expected = """\
            2?750
            ${processingOptionService.findOptionAsString(OptionName.OTP_USER_LINUX_GROUP)}
            """.stripIndent()

        assert permissionAndGroup ==~ expected
    }

    void "test createWorkOutputDirectory_DirectoryAlreadyExist_AllFine"() {
        given:
        setupData()
        executeRoddyCommandService = new ExecuteRoddyCommandService()
        executeRoddyCommandService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommand(_) >> { String command ->
                ['bash', '-c', command].execute().waitFor()
            }
        }
        executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        assert tmpOutputDir.exists()

        when:
        executeRoddyCommandService.createWorkOutputDirectory(tmpOutputDir)

        then:
        String permissionAndGroup = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${tmpOutputDir}
            stat -c %G ${tmpOutputDir}
            """.stripIndent())
        // make the 2 optional, since it does not work for all developers.
        String expected = """\
            2?750
            ${processingOptionService.findOptionAsString(OptionName.OTP_USER_LINUX_GROUP)}
            """.stripIndent()

        permissionAndGroup ==~ expected
    }

    void "test correctPermission_AllOkay"() {
        given:
        setupData()
        executeRoddyCommandService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { String cmd ->
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

        when:
        executeRoddyCommandService.correctPermissions(roddyBamFile)

        then:
        String expected = """\
            2750
            440
            444
            444
            """.stripIndent()

        expected == LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            stat -c %a ${roddyBamFile.workDirectory}/dir
            stat -c %a ${roddyBamFile.workDirectory}/file
            stat -c %a ${roddyBamFile.workDirectory}/${roddyBamFile.baiFileName}
            stat -c %a ${roddyBamFile.workDirectory}/${roddyBamFile.bamFileName}
            """.stripIndent())
    }

    void "test correctPermission_BamFileIsNull_shouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.correctPermissions(null)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("roddyResult")
    }

    void "test correctGroup_AllFine"() {
        given:
        setupData()

        // test data in temp-folder will be created with primary group, which is...
        String primaryGroup = TestConfigService.primaryGroup

        executeRoddyCommandService.remoteShellHelper = [
                executeCommandReturnProcessOutput: { String cmd ->
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

        when:
        executeRoddyCommandService.correctGroups(roddyBamFile)

        then:
        "${primaryGroup}\n" as String == LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(
                "stat -c %G ${roddyBamFile.workDirectory}/file"
        )
    }

    void "test correctGroup_BamFileIsNull_shouldFail"() {
        given:
        setupData()

        when:
        executeRoddyCommandService.correctGroups(null)

        then:
        Throwable e = thrown(AssertionError)
        e.message.contains("roddyResult")
    }

    void "test correctPermissionsAndGroups"() {
        given:
        setupData()
        String primaryGroup = configService.workflowProjectUnixGroup
        String group = configService.testingGroup

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("""\
            chgrp -hR ${group} ${roddyBamFile.workDirectory}
            chgrp -h ${primaryGroup} ${roddyBamFile.baseDirectory}
            chmod -R 777 ${roddyBamFile.workDirectory}
            """.stripIndent()).empty

        RemoteShellHelper remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd ->
                ProcessOutput out = LocalShellHelper.executeAndWait(cmd)
                out.assertExitCodeZeroAndStderrEmpty()
                return out
            }
        }
        executeRoddyCommandService = new ExecuteRoddyCommandService()
        executeRoddyCommandService.remoteShellHelper = remoteShellHelper
        executeRoddyCommandService.executionHelperService = new ExecutionHelperService()
        executeRoddyCommandService.executionHelperService.remoteShellHelper = remoteShellHelper

        when:
        executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile)

        then:
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

        // On some computers the group id are not set for unknown reasons.
        // Therefore allow for the test also the other case
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

        value ==~ expected
    }

    private void initRoddyModule() {
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, LOAD_MODULE)
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA, ACTIVATE_JAVA)
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, ACTIVATE_GROOVY)
    }
}
