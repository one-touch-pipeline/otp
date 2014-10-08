package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvJobResult
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm

class SnvDeepAnnotationJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecutionHelperService executionHelperService
    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Override
    SnvCallingStep getStep() {
        return SnvCallingStep.SNV_DEEPANNOTATION
    }

    SnvCallingStep getPreviousStep() {
        return SnvCallingStep.SNV_ANNOTATION
    }

    @Override
    protected NextAction maybeSubmit(final SnvCallingInstance instance) throws Throwable {
        final SnvConfig config = instance.config.evaluate()
        if (config.getExecuteStepFlag(step)) {
            // Get results from the previous (annotation) step
            SnvJobResult inputResult = instance.findLatestResultForSameBamFiles(previousStep)

            // Check that all needed files are existing
            final File inputResultFile = new OtpPath(instance.snvInstancePath, previousStep.getResultFileName(instance.individual)).absoluteStagingPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)

            // Write the config file in the staging directory or if exists already checks that content is correct
            final File configFileInStagingDirectory = writeConfigFile(instance)

            /*
             * Input which is needed for the DeepAnnotation script. It is just for Roddy intern job system handling.
             * File can be deleted afterwards. Delete it if it exists already.
             */
            final File checkpointFile = new OtpPath(instance.snvInstancePath, step.checkpointFileName).absoluteStagingPath
            deleteResultFileIfExists(checkpointFile)

            // Get path of the snv deep annotation result file
            final File deepAnnotationResultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual)).absoluteStagingPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(deepAnnotationResultFile)

            /*
             * The snv deep annotation script does not use the output vcf parameter but just overrides the input file.
             * Therefore the snv annotation file has to be copied so that it has the correct naming.
             * It was decided not to override the anv annotation file to be on the save side.
             */
            assert ! deepAnnotationResultFile.exists()
            deepAnnotationResultFile.createNewFile()
            deepAnnotationResultFile.text = inputResultFile.text

            final Realm realm = configService.getRealmDataProcessing(instance.project)

            final String qsubParameters="{ '-v': '"+
                    "CONFIG_FILE=${configFileInStagingDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TUMOR_BAMFILE_FULLPATH_BP=${sampleType1BamFile}," +
                    "PIPENAME=SNV_DEEPANNOTATION," +
                    "TOOL_ID=snvDeepAnnotation," +
                    "FILENAME_VCF=${deepAnnotationResultFile}" +
                    "FILENAME_VCF_SNVS=${deepAnnotationResultFile}" +
                    "FILENAME_CHECKPOINT=${checkpointFile}" +
                    "'}"
            final String script =
                    ensureFileDoesNotExistScript(deepAnnotationResultFile) +
                    step.externalScript.scriptFilePath +
                    executionHelperService.sendScript(realm, script, null, qsubParameters)
            createAndSaveSnvJobResult(instance, step.externalScript, inputResult)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            // This SNV workflow instance is configured not to do the SNV deep annotation. Make sure there already is a result
            // that subsequent jobs can use as input.
            final boolean instanceWithResultExists = instance.findLatestResultForSameBamFiles(step) != null
            if (instanceWithResultExists) {
                log.info "This SNV workflow instance is configured not to do the SNV deep annotation. Subsequent jobs will use the results of a previous SNV deep annotation run as input."
                addOutputParameter(REALM, "")
                addOutputParameter(SCRIPT, "")
                return NextAction.SUCCEED
            } else {
                throw new RuntimeException("This SNV workflow instance is configured not to do the SNV deep annotation and no non-withdrawn SNV deep annotation was done before, so subsequent jobs will have no input.")
            }
        }
    }

    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {
        // check if the final vcf result file exists
        final OtpPath resultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFile.absoluteStagingPath)
        // check that the checkpoint file, produced by the script exists
        final File checkpointFile = new OtpPath(instance.snvInstancePath, step.checkpointFileName).absoluteStagingPath
        assert checkpointFile.exists()
        deleteResultFileIfExists(checkpointFile)

        try {
            File inputResultFile = new OtpPath(instance.snvInstancePath, previousStep.getResultFileName(instance.individual)).absoluteStagingPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)
        } catch (final AssertionError e) {
            throw new RuntimeException('The input SnvAnnotation result file has changed on the file system while this job processed them.', e)
        }
        // mark the result of the snv deep annotation step as finished
        changeProcessingStateOfJobResult(instance, SnvProcessingStates.FINISHED)

        //paths for the result file
        List<File> sourceLocation = [
            resultFile.absoluteStagingPath
        ]
        List<File> targetLocation = [
            resultFile.absoluteDataManagementPath
        ]
        List<File> linkLocation = [
            instance.sampleTypeCombination.snvCallingFileLinkedPath.absoluteDataManagementPath
        ]
        //path for the config file
        sourceLocation.add(instance.configFilePath.absoluteStagingPath)
        targetLocation.add(instance.configFilePath.absoluteDataManagementPath)
        linkLocation.add(instance.getStepConfigFileLinkedPath(step).absoluteDataManagementPath)
        String transferClusterScript = createClusterScriptService.createTransferScript(sourceLocation, targetLocation, linkLocation, true)
        //parameter for copying job
        final Realm realm = configService.getRealmDataProcessing(instance.project)
        addOutputParameter(REALM, realm.id.toString())
        addOutputParameter(SCRIPT, transferClusterScript)
    }
}
