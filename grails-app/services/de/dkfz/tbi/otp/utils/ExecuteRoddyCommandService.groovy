package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import groovy.transform.TupleConstructor

class ExecuteRoddyCommandService {

    static final String CORRECT_PERMISSION_SCRIPT_NAME = "OtherUnixUserCorrectPermissionScript"

    static final String CORRECT_GROUP_SCRIPT_NAME = "OtherUnixUserCorrectGroupScript"

    static final String DELETE_CONTENT_OF_OTHERUNIXUSER_DIRECTORIES_SCRIPT = "OtherUnixUserDeleteContentOfOtherUserDirectoriesScript"

    static final String FEATURE_TOGGLES_CONFIG_PATH = "OtherUnixUserFeatureTogglesConfigPath"


    ExecutionService executionService


    /**
     * returns the part of the command to execute Roddy-Workflows which is equal for all Roddy-Workflows
     *
     * @param roddyResult, This is the roddyResult on which each workflow works on (i.e. RoddyBamFile)
     * @param nameInConfigFile, This is the part before the @ in the ProjectConfigFile (i.e. $workflow_$version)
     * @param analysisIDinConfigFile, This is the part after the @ in the ProjectConfigFile (i.e. EXOME)
     * @param realm, This is the realm to work on.
     */
    String defaultRoddyExecutionCommand(RoddyResult roddyResult, String nameInConfigFile, String analysisIDinConfigFile, Realm realm) {
        assert roddyResult : "The input roddyResult is not allowed to be null"
        assert nameInConfigFile : "The input nameInConfigFile is not allowed to be null"
        assert analysisIDinConfigFile : "The input analysisIDinConfigFile is not allowed to be null"
        assert realm : "The input realm is not allowed to be null"

        File workOutputDir = roddyResult.workDirectory
        createWorkOutputDirectory(realm, workOutputDir)

        RoddyWorkflowConfig config = roddyResult.config

        //base view by pid directory
        File viewByPid = roddyResult.individual.getViewByPidPathBase(roddyResult.seqType).absoluteDataManagementPath

        return roddyBaseCommand(nameInConfigFile, analysisIDinConfigFile, RoddyInvocationType.EXECUTE) +
                "${roddyResult.individual.pid} " +
                commonRoddy(config) +
                "--useiodir=${viewByPid},${workOutputDir} "
    }


    String roddyGetRuntimeConfigCommand(RoddyWorkflowConfig config, String nameInConfigFile, String analysisIDinConfigFile) {
        return roddyBaseCommand(nameInConfigFile, analysisIDinConfigFile, RoddyInvocationType.CONFIG) +
                commonRoddy(config)
    }


    String commonRoddy(RoddyWorkflowConfig config) {
        String roddyVersion = ProcessingOptionService.getValueOfProcessingOption("roddyVersion")
        File roddyBaseConfigsPath = ProcessingOptionService.getValueOfProcessingOption("roddyBaseConfigsPath") as File
        File applicationIniPath = ProcessingOptionService.getValueOfProcessingOption("roddyApplicationIni") as File

        //ensure that needed input files are available on the file system
        LsdfFilesService.ensureDirIsReadableAndNotEmpty(roddyBaseConfigsPath)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(applicationIniPath)


        String pluginVersion = config.pluginVersion
        File configFile = new File(config.configFilePath)

        return \
            "--useconfig=${applicationIniPath} " +
            "--usefeaturetoggleconfig=${featureTogglesConfigPath()} " +
            "--useRoddyVersion=${roddyVersion} " +
            "--usePluginVersion=${pluginVersion} " +
            "--configurationDirectories=${configFile.parent},${roddyBaseConfigsPath} "
    }


    String roddyBaseCommand(String configName, String analysisId, RoddyInvocationType type) {
        File roddyPath = ProcessingOptionService.getValueOfProcessingOption("roddyPath") as File
        return roddyBaseCommand(roddyPath, configName, analysisId, type)
    }

    String roddyBaseCommand(File roddyPath, String configName, String analysisId, RoddyInvocationType type) {
        assert roddyPath : "roddyPath is not allowed to be null"
        assert configName : "configName is not allowed to be null"
        assert analysisId : "analysisId is not allowed to be null"
        assert type : "type is not allowed to be null"
        //we change to tmp dir to be on a specified directory where OtherUnixUser has access to
        return "cd /tmp && ${executeCommandAsRoddyUser()} ${roddyPath}/roddy.sh ${type.cmd} ${configName}.config@${analysisId} "
    }

    String executeCommandAsRoddyUser() {
        return "sudo -u OtherUnixUser"
    }


    /**
     * Returns the analysis id which has to be used in this roddy workflow
     */
    String getAnalysisIDinConfigFile(RoddyResult roddyResult) {
        assert roddyResult : "The input roddyResult must not be null"
        assert roddyResult.seqType : "There is not seqType available for ${roddyResult}"

        if (SeqTypeService.alignableSeqTypes().contains(roddyResult.seqType)) {
            String roddyName = roddyResult.seqType.roddyName
            assert roddyName : "roddyName is not specified for ${roddyResult.seqType}"
            return roddyName
        } else {
            throw new RuntimeException("The seqType ${roddyResult.seqType} can not be processed at the moment." as String)
        }
    }


    public void createWorkOutputDirectory(Realm realm, File file) {
        assert realm : "Realm must not be null"
        assert file : "File must not be null"
        executionService.executeCommand(realm, "umask 027; mkdir -m 2750 -p ${file.parent} && mkdir -m 2770 -p ${file} && chgrp localGroup ${file};")
        WaitingFileUtils.waitUntilExists(file)
    }

    String correctPermissionCommand(File basePath) {
        assert basePath : "basePath is not allowed to be null"
        File permissionScript = ProcessingOptionService.getValueOfProcessingOption(CORRECT_PERMISSION_SCRIPT_NAME) as File
        return "cd /tmp && ${executeCommandAsRoddyUser()} ${permissionScript} ${basePath}"
    }

    void correctPermissions(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "RoddyBamFile should not be null"
        String cmd = correctPermissionCommand(roddyBamFile.workDirectory)
        ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput(cmd)
    }

    String correctGroupCommand(File basePath) {
        assert basePath : "basePath is not allowed to be null"
        File permissionScript = ProcessingOptionService.getValueOfProcessingOption(CORRECT_GROUP_SCRIPT_NAME) as File
        return "cd /tmp && ${executeCommandAsRoddyUser()} ${permissionScript} ${basePath}"
    }

    void correctGroups(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "RoddyBamFile should not be null"
        String cmd = correctGroupCommand(roddyBamFile.workDirectory)
        ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput(cmd)
    }

    void deleteContentOfOtherUnixUserDirectory(File basePath) {
        assert basePath : "basePath is not allowed to be null"
        File script = ProcessingOptionService.getValueOfProcessingOption(DELETE_CONTENT_OF_OTHERUNIXUSER_DIRECTORIES_SCRIPT) as File
        String cmd = "cd /tmp && ${executeCommandAsRoddyUser()} ${script} ${basePath}"
        ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput(cmd)
    }

    File featureTogglesConfigPath() {
        return new File(ProcessingOptionService.getValueOfProcessingOption(FEATURE_TOGGLES_CONFIG_PATH))
    }

    @TupleConstructor
    enum RoddyInvocationType {
        EXECUTE("rerun"),
        CONFIG("printidlessruntimeconfig")

        final String cmd
    }
}
