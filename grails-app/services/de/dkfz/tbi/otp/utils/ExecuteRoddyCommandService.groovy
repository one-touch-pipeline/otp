package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.logging.JobLog
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal


class ExecuteRoddyCommandService {

    static final String CORRECT_PERMISSION_SCRIPT_NAME = "OtherUnixUserCorrectPermissionScript"

    ExecutionService executionService

    String roddyBaseCommand(File roddyPath, String configName, String analysisId) {
        assert roddyPath : "roddyPath is not allowed to be null"
        assert configName : "configName is not allowed to be null"
        assert analysisId : "analysisId is not allowed to be null"
        //we change to tmp dir to be on a specified directory where OtherUnixUser has access to
        return "cd /tmp && ${executeCommandAsRoddyUser()} ${executionCommand(roddyPath, configName, analysisId)} "
    }


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

        File tempOutputDir = roddyResult.tmpRoddyDirectory
        createTemporaryOutputDirectory(realm, tempOutputDir)

        File roddyPath = ProcessingOptionService.getValueOfProcessingOption("roddyPath") as File
        String roddyVersion = ProcessingOptionService.getValueOfProcessingOption("roddyVersion")
        File roddyBaseConfigsPath = ProcessingOptionService.getValueOfProcessingOption("roddyBaseConfigsPath") as File
        File applicationIniPath = ProcessingOptionService.getValueOfProcessingOption("roddyApplicationIni") as File

        //ensure that needed input files are available on the file system
        LsdfFilesService.ensureDirIsReadableAndNotEmpty(roddyBaseConfigsPath)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(applicationIniPath)


        RoddyWorkflowConfig config = roddyResult.config
        String pluginVersion = config.pluginVersion
        File configFile = new File(config.configFilePath)

        //base view by pid directory
        File viewByPid = roddyResult.individual.getViewByPidPathBase(roddyResult.seqType).absoluteDataManagementPath

        return roddyBaseCommand(roddyPath, nameInConfigFile, analysisIDinConfigFile) +
                "${roddyResult.individual.pid} " +
                "--useconfig=${applicationIniPath} " +
                "--useRoddyVersion=${roddyVersion} " +
                "--usePluginVersion=${pluginVersion} " +
                "--configurationDirectories=${configFile.parent},${roddyBaseConfigsPath} " +
                "--useiodir=${viewByPid},${tempOutputDir} "
    }


    String executeCommandAsRoddyUser() {
        return "sudo -u OtherUnixUser"
    }

    private String executionCommand(File roddyPath, String configName, String analysisId){
        return "${roddyPath}/roddy.sh rerun ${configName}.config@${analysisId}"
    }


    /**
     * Returns the analysis id which has to be used in this roddy workflow
     */
    String getAnalysisIDinConfigFile(RoddyResult roddyResult) {
        assert roddyResult : "The input roddyResult must not be null"
        assert roddyResult.seqType : "There is not seqType available for ${roddyResult}"

        if (SeqTypeService.alignableSeqTypes().contains(roddyResult.seqType)) {
            return roddyResult.seqType.alias
        } else {
            throw new RuntimeException("The seqType ${roddyResult.seqType} can not be processed at the moment." as String)
        }
    }


    public void createTemporaryOutputDirectory(Realm realm, File file) {
        assert realm : "Realm must not be null"
        assert file : "File must not be null"
        executionService.executeCommand(realm, "umask 027; mkdir -m 2750 -p ${file.parent} && mkdir -m 2770 -p ${file} && chgrp localGroup ${file};")
        assert WaitingFileUtils.waitUntilExists(file) : "Creation of '${file}' failed"
    }

    String correctPermissionCommand(File basePath) {
        assert basePath : "basePath is not allowed to be null"
        File permissionScript = ProcessingOptionService.getValueOfProcessingOption(CORRECT_PERMISSION_SCRIPT_NAME) as File
        return "cd /tmp && ${executeCommandAsRoddyUser()} ${permissionScript} ${basePath}"
    }

    void correctPermissions(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "RoddyBamFile should not be null"
        File baseFile = new File(AbstractMergedBamFileService.destinationDirectory(roddyBamFile))
        String cmd = correctPermissionCommand(baseFile)
        ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput(cmd)
    }

}
