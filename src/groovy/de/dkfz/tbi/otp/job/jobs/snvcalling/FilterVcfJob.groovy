package de.dkfz.tbi.otp.job.jobs.snvcalling

import groovy.io.FileType

import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.ngsdata.*

class FilterVcfJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecutionHelperService executionHelperService
    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Override
    public SnvCallingStep getStep() {
        return SnvCallingStep.FILTER_VCF
    }

    @Override
    public SnvCallingStep getPreviousStep() {
        return SnvCallingStep.SNV_DEEPANNOTATION
    }

    @Override
    protected NextAction maybeSubmit(final SnvCallingInstance instance) throws Throwable {
        final SnvConfig config = instance.config.evaluate()
        if (config.getExecuteStepFlag(step)) {

            // Get results from the previous (deep annotation) step
            SnvJobResult inputResult = instance.findLatestResultForSameBamFiles(previousStep)
            assert inputResult

            // Check that all needed files are existing
            final File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)


            // All files, which are currently in the staging folder are not needed anymore -> delete them
            File instancePath = instance.snvInstancePath.absoluteStagingPath
            if(instancePath.exists()) {
                instancePath.eachFileRecurse (FileType.FILES) { File file ->
                    assert file.delete()
                }
            }

            final File configFileInStagingDirectory = writeConfigFile(instance)

            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteStagingPath

            final Realm realm = configService.getRealmDataProcessing(instance.project)

            final String qsubParameters="{ '-v': '"+
                    "CONFIG_FILE=${configFileInStagingDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TOOL_ID=snvFilter," +
                    "SNVFILE_PREFIX=snvs_," +
                    "TUMOR_BAMFILE_FULLPATH_BP=${getExistingBamFilePath(instance.sampleType1BamFile)}," +
                    "FILENAME_VCF=${inputResultFile}," +
                    "FILENAME_CHECKPOINT=${checkpointFile}" +
                    "'}"

            final String script = step.externalScript.scriptFilePath

            executionHelperService.sendScript(realm, script, null, qsubParameters)

            createAndSaveSnvJobResult(instance, step.externalScript, null, inputResult)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance, true)
            return NextAction.SUCCEED
        }
    }

    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {
        // check that the content of the config file is still the same
        File configFile = instance.configFilePath.absoluteStagingPath
        assert configFile.text == instance.config.configuration

        // check if the final vcf result file exists -> there are not only vcf files, but also pngs and pdfs
        File instancePath = instance.snvInstancePath.absoluteStagingPath
        instancePath.eachFileRecurse (FileType.FILES) { File resultFile ->
            assert resultFile.exists()
            assert resultFile.isAbsolute()
            assert resultFile.isFile()
        }

        // check that the checkpoint file, produced by the script exists
        final File checkpointFile = step.getCheckpointFilePath(instance).absoluteStagingPath
        assert checkpointFile.exists()
        assert checkpointFile.delete()

        try {
            SnvJobResult inputResult = getSnvJobResult(instance).inputResult
            File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)
        } catch (final AssertionError e) {
            throw new RuntimeException('The input SnvDeepAnnotation result file has changed on the file system while this job processed them.', e)
        }
        // mark the result of the snv annotation step as finished
        changeProcessingStateOfJobResult(instance, SnvProcessingStates.FINISHED)

        //paths for the result files, index file and config file
        List<File> sourceLocation = []
        List<File> targetLocation = []
        List<File> linkLocation = []
        instancePath.eachFileRecurse (FileType.FILES) { File resultFile ->
            // one file (snvs_${pid}_intermutation_distance_conf_8_to_10.txt), which is only used as input for a plot shall not be copied
            if (!(resultFile =~ /intermutation_distance(.*)\.txt$/ || resultFile =~ /\.tbi/)) {
                OtpPath resultFilePath = new OtpPath(instance.snvInstancePath, resultFile.getName())
                sourceLocation.add(resultFilePath.absoluteStagingPath)
                targetLocation.add(resultFilePath.absoluteDataManagementPath)
                if (resultFile.getName() == configFile.getName()) {
                    linkLocation.add(instance.getStepConfigFileLinkedPath(step).absoluteDataManagementPath)
                } else {
                    linkLocation.add(new OtpPath(instance.sampleTypeCombination.getResultFileLinkedPath(SnvCallingStep.FILTER_VCF), resultFile.getName()).absoluteDataManagementPath)
                }
            }
        }
        String transferClusterScript = createClusterScriptService.createTransferScript(sourceLocation, targetLocation, linkLocation, true)

        //parameter for copying job
        final Realm realm = configService.getRealmDataProcessing(instance.project)
        addOutputParameter(REALM, realm.id.toString())
        addOutputParameter(SCRIPT, transferClusterScript)
    }
}
