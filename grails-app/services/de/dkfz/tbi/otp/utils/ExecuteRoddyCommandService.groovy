package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
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
        String pipelineVersion = config.externalScriptVersion
        File configFile = new File(config.configFilePath)

        //base view by pid directory
        File viewByPid = roddyResult.individual.getViewByPidPathBase(roddyResult.seqType).absoluteDataManagementPath

        return roddyBaseCommand(roddyPath, nameInConfigFile, analysisIDinConfigFile) +
                "${roddyResult.individual.pid} " +
                "--useconfig=${applicationIniPath} " +
                "--useRoddyVersion=${roddyVersion} " +
                "--usePluginVersion=${pipelineVersion} " +
                "--configurationDirectories=${configFile.parent},${roddyBaseConfigsPath} " +
                "--useiodir=${viewByPid},${tempOutputDir} "
    }


    private String executeCommandAsRoddyUser() {
        return "sudo -u OtherUnixUser"
    }

    private String executionCommand(File roddyPath, String configName, String analysisId){
        return "${roddyPath}/roddy.sh rerun ${configName}.config@${analysisId}"
    }


    Process executeRoddyCommand(String cmd) {
        assert cmd : "The input cmd must not be null"
        return [ 'bash', '-c', cmd ].execute()
    }


     String returnStdoutOfFinishedCommandExecution(Process process) {
         assert process : "The input process must not be null"
         StringBuffer stdout = new StringBuffer()
         StringBuffer stderr = new StringBuffer()
         process.waitForProcessOutput(stdout, stderr)

         //Roddy give some output in stderr, therefor we can not check for empty stderr

         LogThreadLocal.getThreadLog().debug("Roddy stderr:\n ${stderr}")
         LogThreadLocal.getThreadLog().debug("Roddy stdout:\n ${stdout}")
         return stdout.toString().trim()
    }


    void checkIfRoddyWFExecutionWasSuccessful(Process process) {
        assert process : "The input process must not be null"
        assert process.exitValue() == 0 : "The Roddy exit code is not equals 0, but ${process.exitValue()}"
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
        executionService.executeCommand(realm, "mkdir -m 2750 -p ${file.parent} && mkdir -m 2770 ${file};")
        assert WaitingFileUtils.waitUntilExists(file) : "Creation of '${file}' failed"
    }
}
