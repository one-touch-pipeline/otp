package de.dkfz.tbi.otp.job.jobs.snvcalling


import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*
import static de.dkfz.tbi.otp.utils.WaitingFileUtils.confirmExists
import org.apache.commons.io.FileUtils
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
            assert inputResult

            /*
             * Check that the needed input file exists.
             * There is no copy step after the annotation job,
             * therefore the result of the annotation job is still in the staging directory.
             */
            final File inputResultFile = inputResult.resultFilePath.absoluteStagingPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)

            final File configFileInStagingDirectory = writeConfigFile(instance)

            /*
             * Input which is needed for the DeepAnnotation script. It is just for Roddy intern job system handling.
             * File can be deleted afterwards. Delete it if it exists already.
             */
            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteStagingPath
            deleteResultFileIfExists(checkpointFile)

            // Get path of the snv deep annotation result file
            final File deepAnnotationResultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual)).absoluteStagingPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(deepAnnotationResultFile)

            /*
             * The snv deep annotation script does not use the output vcf parameter but just overrides the input file.
             * Therefore the snv annotation file has to be copied so that it has the correct naming.
             * It was decided not to override the snv annotation file to be on the safe side.
             */
            FileUtils.copyFile(inputResultFile, deepAnnotationResultFile)

            final Realm realm = configService.getRealmDataProcessing(instance.project)

            final String qsubParameters="{ '-v': '"+
                    "CONFIG_FILE=${configFileInStagingDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TOOL_ID=snvDeepAnnotation," +
                    "PIPENAME=SNV_DEEPANNOTATION," +
                    "FILENAME_VCF=${deepAnnotationResultFile}," +
                    "FILENAME_VCF_SNVS=${deepAnnotationResultFile}," +
                    "FILENAME_CHECKPOINT=${checkpointFile}," +
                    "'}"
            final String script = "${step.externalScript.scriptFilePath}; " +
                    "md5sum ${deepAnnotationResultFile} > ${deepAnnotationResultFile}.md5sum"

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
        assert instance.configFilePath.absoluteStagingPath.text == instance.config.configuration
        // check if the final vcf result file exists
        final OtpPath resultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFile.absoluteStagingPath)
        // check that the checkpoint file, produced by the script exists
        final File checkpointFile = step.getCheckpointFilePath(instance).absoluteStagingPath
        assert confirmExists(checkpointFile)
        assert checkpointFile.delete()

        try {
            SnvJobResult inputResult = getSnvJobResult(instance).inputResult
            File inputResultFile = inputResult.resultFilePath.absoluteStagingPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)
        } catch (final AssertionError e) {
            throw new RuntimeException('The input SnvAnnotation result file is not readable anymore or empty.', e)
        }
        // mark the result of the snv deep annotation step as finished
        changeProcessingStateOfJobResult(instance, SnvProcessingStates.FINISHED)

        // paths for the result file
        List<File> sourceLocation = [ resultFile.absoluteStagingPath ]
        List<File> targetLocation = [ resultFile.absoluteDataManagementPath ]
        List<File> linkLocation = [ instance.samplePair.getResultFileLinkedPath(step).absoluteDataManagementPath ]

        // path for index files
        OtpPath indexFile = new OtpPath(instance.snvInstancePath, step.getIndexFileName(instance.individual))
        sourceLocation.add(indexFile.absoluteStagingPath)
        targetLocation.add(indexFile.absoluteDataManagementPath)
        linkLocation.add(instance.samplePair.getIndexFileLinkedPath(step).absoluteDataManagementPath)

        //path for the config file
        sourceLocation.add(instance.configFilePath.absoluteStagingPath)
        targetLocation.add(instance.configFilePath.absoluteDataManagementPath)
        linkLocation.add(instance.getStepConfigFileLinkedPath(step).absoluteDataManagementPath)

        String transferClusterScript = createClusterScriptService.createTransferScript(sourceLocation, targetLocation, linkLocation, true)
        // parameter for copying job
        final Realm realm = configService.getRealmDataProcessing(instance.project)
        addOutputParameter(REALM, realm.id.toString())
        addOutputParameter(SCRIPT, transferClusterScript)
    }
}
