/*
 * Copyright 2011-2019 The OTP authors
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

import groovy.transform.TupleConstructor
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class ExecuteRoddyCommandService {

    @Autowired
    RemoteShellHelper remoteShellHelper

    ExecutionHelperService executionHelperService
    ProcessingOptionService processingOptionService


    String activateModulesForRoddyCommand() {
        String loadModule = processingOptionService.findOptionAsString(OptionName.COMMAND_LOAD_MODULE_LOADER)
        String activateJava = processingOptionService.findOptionAsString(OptionName.COMMAND_ACTIVATION_JAVA)
        String activateGroovy = processingOptionService.findOptionAsString(OptionName.COMMAND_ACTIVATION_GROOVY)

        return [
                loadModule,
                activateJava,
                activateGroovy,
        ].findAll().join('\n')
    }

    /**
     * returns the part of the command to execute Roddy-Workflows which is equal for all Roddy-Workflows
     *
     * @param roddyResult , This is the roddyResult on which each workflow works on (i.e. RoddyBamFile)
     * @param nameInConfigFile , This is the part before the @ in the ProjectConfigFile (i.e. $workflow_$version)
     * @param analysisIDinConfigFile , This is the part after the @ in the ProjectConfigFile (i.e. EXOME)
     * @param realm , This is the realm to work on.
     */
    String defaultRoddyExecutionCommand(RoddyResult roddyResult, String nameInConfigFile, String analysisIDinConfigFile, Realm realm) {
        assert roddyResult: "The input roddyResult is not allowed to be null"
        assert nameInConfigFile: "The input nameInConfigFile is not allowed to be null"
        assert analysisIDinConfigFile: "The input analysisIDinConfigFile is not allowed to be null"
        assert realm: "The input realm is not allowed to be null"

        if (roddyResult.roddyExecutionDirectoryNames && !roddyResult.workDirectory.exists()) {
            roddyResult.roddyExecutionDirectoryNames.clear()
            roddyResult.save(flush: true)
        }

        File workOutputDir = roddyResult.workDirectory
        createWorkOutputDirectory(realm, workOutputDir)

        RoddyWorkflowConfig config = roddyResult.config

        //base view by pid directory
        File viewByPid = roddyResult.individual.getViewByPidPathBase(roddyResult.seqType).absoluteDataManagementPath

        String roddyCommand = [
                roddyBaseCommand(nameInConfigFile, analysisIDinConfigFile, RoddyInvocationType.EXECUTE),
                "${roddyResult.individual.pid}",
                commonRoddy(config, roddyResult.project.realm.jobScheduler),
                "--useiodir=${viewByPid},${workOutputDir}",
        ].join(" ")

        return [
                activateModulesForRoddyCommand(),
                roddyCommand,
        ].join('\n')
    }


    String roddyGetRuntimeConfigCommand(RoddyWorkflowConfig config, String nameInConfigFile, String analysisIDinConfigFile) {
        String roddyConfigCommand = [
                roddyBaseCommand(nameInConfigFile, analysisIDinConfigFile, RoddyInvocationType.CONFIG),
                commonRoddy(config, null),
        ].join(" ")

        return [
                activateModulesForRoddyCommand(),
                roddyConfigCommand,
        ].join('\n')
    }


    String commonRoddy(RoddyWorkflowConfig config, Realm.JobScheduler jobScheduler) {
        File roddyBaseConfigsPath = processingOptionService.findOptionAsString(OptionName.RODDY_BASE_CONFIGS_PATH) as File
        File applicationIniPath = processingOptionService.findOptionAsString(OptionName.RODDY_APPLICATION_INI) as File

        //ensure that needed input files are available on the file system
        LsdfFilesService.ensureDirIsReadableAndNotEmpty(roddyBaseConfigsPath)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(applicationIniPath)


        String pluginVersion = config.pluginVersion
        File configFile = new File(config.configFilePath)
        String jobSchSpecificConfig = ""
        if (jobScheduler) {
            jobSchSpecificConfig = ",${roddyBaseConfigsPath}/resource/${jobScheduler.toString().toLowerCase()}"
        }

        return [
                "--useconfig=${applicationIniPath}",
                "--usefeaturetoggleconfig=${featureTogglesConfigPath()}",
                "--usePluginVersion=${pluginVersion}",
                "--configurationDirectories=${configFile.parent},${roddyBaseConfigsPath}${jobSchSpecificConfig}",
        ].join(" ")
    }


    String roddyBaseCommand(String configName, String analysisId, RoddyInvocationType type) {
        File roddyPath = processingOptionService.findOptionAsString(OptionName.RODDY_PATH) as File
        return roddyBaseCommand(roddyPath, configName, analysisId, type)
    }

    String roddyBaseCommand(File roddyPath, String configName, String analysisId, RoddyInvocationType type) {
        assert roddyPath: "roddyPath is not allowed to be null"
        assert configName: "configName is not allowed to be null"
        assert analysisId: "analysisId is not allowed to be null"
        assert type: "type is not allowed to be null"
        return "${roddyPath}/roddy.sh ${type.cmd} ${configName}.config@${analysisId}"
    }

    /**
     * Returns the analysis id which has to be used in this roddy workflow
     */
    String getAnalysisIDinConfigFile(RoddyResult roddyResult) {
        assert roddyResult: "The input roddyResult must not be null"
        assert roddyResult.seqType: "There is not seqType available for ${roddyResult}"

        if (SeqTypeService.roddyAlignableSeqTypes.contains(roddyResult.seqType)) {
            String roddyName = roddyResult.seqType.roddyName
            assert roddyName: "roddyName is not specified for ${roddyResult.seqType}"
            return roddyName
        } else {
            throw new RuntimeException("The seqType ${roddyResult.seqType} can not be processed at the moment." as String)
        }
    }


    void createWorkOutputDirectory(Realm realm, File file) {
        assert realm: "Realm must not be null"
        assert file: "File must not be null"
        if (file.exists()) {
            remoteShellHelper.executeCommand(realm,
                    "umask 027; chgrp ${processingOptionService.findOptionAsString(OptionName.OTP_USER_LINUX_GROUP)} ${file} ; chmod 2770 ${file}")
        } else {
            remoteShellHelper.executeCommand(realm,
                    "umask 027; mkdir -m 2750 -p ${file.parent} && mkdir -m 2770 -p ${file} && " +
                            "chgrp ${processingOptionService.findOptionAsString(OptionName.OTP_USER_LINUX_GROUP)} ${file};")
            WaitingFileUtils.waitUntilExists(file)
        }
    }

    void correctPermissionsAndGroups(RoddyResult roddyResult, Realm realm) {
        executionHelperService.setPermission(realm, roddyResult.workDirectory, CreateClusterScriptService.DIRECTORY_PERMISSION)
        correctPermissions(roddyResult, realm)
        String group = executionHelperService.getGroup(roddyResult.baseDirectory)
        executionHelperService.setGroup(realm, roddyResult.workDirectory, group)
        correctGroups(roddyResult, realm)
    }

    @SuppressWarnings('LineLength')
    void correctPermissions(RoddyResult roddyResult, Realm realm) {
        assert roddyResult: "roddyResult should not be null"
        String cmd = """\
            set -e
            set -o pipefail
            cd "${roddyResult.workDirectory}"

            echo ""
            echo "correct directory permission"
            find -type d -not -perm 2750 -print -exec chmod 2750 '{}' \\; | wc -l

            echo ""
            echo "correct file permission for non bam/bai files"
            # The file is not changed, since it needs to be stay writable"
            find -type f -not -perm 440 -not -name "*.bam" -not -name "*.bai" -not -name ".roddyExecCache.txt" -not -name "zippedAnalysesMD5.txt" -print -exec chmod 440 '{}' \\; | wc -l

            echo ""
            echo "correct file permission for bam/bai files"
            find -type f -not -perm 444 \\( -name "*.bam" -or -name "*.bai" \\) -print -exec chmod 444 '{}' \\; | wc -l
            """.stripMargin()
        remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd).assertExitCodeZeroAndStderrEmpty()
    }

    void correctGroups(RoddyResult roddyResult, Realm realm) {
        assert roddyResult: "roddyResult should not be null"
        String cmd = """\
            set -e
            set -o pipefail
            cd "${roddyResult.workDirectory}"

            #correct group
            groupname=`stat -c '%G' .`
            echo ""
            echo "correct group permission to" \$groupname
            find -not -type l -not -group \$groupname -print -exec chgrp \$groupname '{}' \\; | wc -l
            """.stripMargin()
        remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd).assertExitCodeZeroAndStderrEmpty()
    }

    File featureTogglesConfigPath() {
        return new File(processingOptionService.findOptionAsString(OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH))
    }

    @TupleConstructor
    enum RoddyInvocationType {
        EXECUTE("rerun"),
        CONFIG("printidlessruntimeconfig")

        final String cmd
    }
}
