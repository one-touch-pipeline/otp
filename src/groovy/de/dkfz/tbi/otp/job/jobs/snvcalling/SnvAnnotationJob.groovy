package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob

import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

class SnvAnnotationJob extends AbstractSnvCallingJob implements AutoRestartableJob {

    @Autowired
    ConfigService configService
    @Autowired
    PbsService pbsService
    @Autowired
    AbstractMergedBamFileService abstractMergedBamFileService

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
        final Realm realm = configService.getRealmDataProcessing(instance.project)
        if (config.getExecuteStepFlag(step)) {
            // Get results from the previous (calling) step
            SnvJobResult inputResult = instance.findLatestResultForSameBamFiles(previousStep)
            assert inputResult

            createAndSaveSnvJobResult(instance, step.getExternalScript(config.externalScriptVersion), null, inputResult)

            // Check that all needed files are existing
            final File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)
            final File sampleType1BamFile = abstractMergedBamFileService.getExistingBamFilePath(instance.sampleType1BamFile)

            // Write the config file in the project directory or if exists already checks that content is correct
            final File configFileInProjectDirectory = writeConfigFile(instance)


            // The annotation script of the CO group writes a temporary file in the same folder where its input is.
            // To prevent mixing up results of old and new SnvCallingInstances we link the input file to output folder.
            File inputFileCopy = new File(instance.instancePath.absoluteDataManagementPath, inputResultFile.name)

            /*
             * Input which is needed for the Annotation script. It is just for Roddy intern job system handling.
             * File can be deleted afterwards. Delete it if it exists already.
             */
            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteDataManagementPath
            deleteResultFileIfExists(checkpointFile, realm)

            // Get path of the snv annotation result file
            final File annotationResultFile = new OtpPath(instance.instancePath, step.getResultFileName(instance.individual)).absoluteDataManagementPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(annotationResultFile, realm)

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
                script << "ln -sf ${inputResultFile.absolutePath} ${inputFileCopy.absolutePath}; "
            }
            script << "${ensureFileHasExpectedSizeScript(sampleType1BamFile, instance.sampleType1BamFile.fileSize)} "
            script << "${ensureFileDoesNotExistScript(annotationResultFile)} "
            script << "${step.getExternalScript(config.externalScriptVersion).scriptFilePath} "
            pbsService.executeJob(realm, script.toString(), qsubParameters)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance)
            linkPreviousResults(instance, realm)
            return NextAction.SUCCEED
        }
    }

    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {

        // check if the final vcf result file exists
        final OtpPath resultFile = new OtpPath(instance.instancePath, step.getResultFileName(instance.individual))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFile.absoluteDataManagementPath)

        validateNonCallingJobs(instance, step)
    }
}
