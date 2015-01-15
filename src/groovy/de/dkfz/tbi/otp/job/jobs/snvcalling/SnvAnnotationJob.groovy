package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*
import static de.dkfz.tbi.otp.utils.WaitingFileUtils.confirmExists
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.ngsdata.*

class SnvAnnotationJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecutionHelperService executionHelperService
    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Override
    SnvCallingStep getStep() {
        return SnvCallingStep.SNV_ANNOTATION
    }

    @Override
    SnvCallingStep getPreviousStep() {
        return SnvCallingStep.CALLING
    }

    @Override
    protected NextAction maybeSubmit(final SnvCallingInstance instance) throws Throwable {
        final SnvConfig config = instance.config.evaluate()
        if (config.getExecuteStepFlag(step)) {
            // Get results from the previous (calling) step
            SnvJobResult inputResult = instance.findLatestResultForSameBamFiles(previousStep)
            assert inputResult

            // Check that all needed files are existing
            final File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)
            final File sampleType1BamFile = getExistingBamFilePath(instance.sampleType1BamFile)

            // Write the config file in the staging directory or if exists already checks that content is correct
            final File configFileInStagingDirectory = writeConfigFile(instance)

            // the annotation script of the CO group writes a temporary file in the same folder where its input is
            // which caused a permission problem when when using the BQ LSDF,
            // so we copy the input file to output folder
            File inputFileCopy = new File(instance.snvInstancePath.absoluteStagingPath, inputResultFile.name)
            // TODO: replace with Files.createSymbolicLink() in Java 7 (OTP-933)
            java.lang.Process process = Runtime.getRuntime().exec(["ln", "-s", inputResultFile.absolutePath, inputFileCopy.absolutePath] as String[])
            assert process.waitFor() == 0

            /*
             * Input which is needed for the Annotation script. It is just for Roddy intern job system handling.
             * File can be deleted afterwards. Delete it if it exists already.
             */
            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteStagingPath
            deleteResultFileIfExists(checkpointFile)

            // Get path of the snv annotation result file
            final File annotationResultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual)).absoluteStagingPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(annotationResultFile)

            final Realm realm = configService.getRealmDataProcessing(instance.project)

            final String qsubParameters="{ '-v': '"+
                    "CONFIG_FILE=${configFileInStagingDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TUMOR_BAMFILE_FULLPATH_BP=${sampleType1BamFile}," +
                    "TOOL_ID=snvAnnotation," +
                    "FILENAME_VCF_IN=${inputFileCopy}," +
                    "FILENAME_VCF_OUT=${annotationResultFile}," +
                    "FILENAME_CHECKPOINT=${checkpointFile}" +
                    "'}"
            final String script =
                    ensureFileHasExpectedSizeScript(sampleType1BamFile, instance.sampleType1BamFile.fileSize) +
                    ensureFileDoesNotExistScript(annotationResultFile) +
                    step.externalScript.scriptFilePath
            executionHelperService.sendScript(realm, script, null, qsubParameters)

            createAndSaveSnvJobResult(instance, step.externalScript, null, inputResult)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance, false)
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
            File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)
        } catch (final AssertionError e) {
            throw new RuntimeException('The input SnvCalling result file has changed on the file system while this job processed them.', e)
        }
        // mark the result of the snv annotation step as finished
        changeProcessingStateOfJobResult(instance, SnvProcessingStates.FINISHED)
    }
}
