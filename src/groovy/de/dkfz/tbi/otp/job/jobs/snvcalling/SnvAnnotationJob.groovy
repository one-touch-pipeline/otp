package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction


class SnvAnnotationJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecutionHelperService executionHelperService

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

            // Write the config file in the project directory or if exists already checks that content is correct
            final File configFileInProjectDirectory = writeConfigFile(instance)


            // The annotation script of the CO group writes a temporary file in the same folder where its input is.
            // To prevent mixing up results of old and new SnvCallingInstances we link the input file to output folder.
            File inputFileCopy = new File(instance.snvInstancePath.absoluteDataManagementPath, inputResultFile.name)

            final Realm realm = configService.getRealmDataProcessing(instance.project)
            /*
             * Input which is needed for the Annotation script. It is just for Roddy intern job system handling.
             * File can be deleted afterwards. Delete it if it exists already.
             */
            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteDataManagementPath
            deleteResultFileIfExists(checkpointFile, realm)

            // Get path of the snv annotation result file
            final File annotationResultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual)).absoluteDataManagementPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(annotationResultFile, realm)

            final String pbsOptionName = getSnvPBSOptionsNameSeqTypeSpecific(instance.seqType)

            final String qsubParameters="{ '-v': '"+
                    "CONFIG_FILE=${configFileInProjectDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TUMOR_BAMFILE_FULLPATH_BP=${sampleType1BamFile}," +
                    "TOOL_ID=snvAnnotation," +
                    "FILENAME_VCF_IN=${inputFileCopy}," +
                    "FILENAME_VCF_OUT=${annotationResultFile}," +
                    "FILENAME_CHECKPOINT=${checkpointFile}" +
                    "'}"
            final StringBuilder script = new StringBuilder()
            if (inputFileCopy.absolutePath != inputResultFile.absolutePath) {
                script << "ln -s ${inputResultFile.absolutePath} ${inputFileCopy.absolutePath}; "
            }
            script << "${ensureFileHasExpectedSizeScript(sampleType1BamFile, instance.sampleType1BamFile.fileSize)} "
            script << "${ensureFileDoesNotExistScript(annotationResultFile)} "
            script << "${step.getExternalScript(config.externalScriptVersion).scriptFilePath} "
            // In case the input file had to be linked to the output folder it has to be deleted afterwards.
            // Otherwise it would be twice in the file system.
            if (inputFileCopy.absolutePath != inputResultFile.absolutePath) {
                script << " rm -f ${inputFileCopy.absolutePath}"
            }
            executionHelperService.sendScript(realm, script.toString(), pbsOptionName, qsubParameters)

            createAndSaveSnvJobResult(instance, step.getExternalScript(config.externalScriptVersion), null, inputResult)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance)
            return NextAction.SUCCEED
        }
    }

    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {

        // check if the final vcf result file exists
        final OtpPath resultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFile.absoluteDataManagementPath)

        validateNonCallingJobs(instance, step)
    }
}
