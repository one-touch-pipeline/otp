package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.ngsdata.*

class FilterVcfJob extends AbstractSnvCallingJob implements AutoRestartableJob {

    @Autowired
    ConfigService configService
    @Autowired
    PbsService pbsService
    @Autowired
    AbstractMergedBamFileService abstractMergedBamFileService

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
        final Realm realm = configService.getRealmDataProcessing(instance.project)
        if (config.getExecuteStepFlag(step)) {

            // Get results from the previous (deep annotation) step
            SnvJobResult inputResult = instance.findLatestResultForSameBamFiles(previousStep)
            assert inputResult

            createAndSaveSnvJobResult(instance, step.getExternalScript(config.externalScriptVersion), null, inputResult)

            // Check that all needed files are existing
            final File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)

            final File configFileInProjectDirectory = writeConfigFile(instance)

            // the filter script of the CO group writes its output in the same folder where its input is,
            // so we link the input file to output folder if they are different
            File inputFileCopy = new File(instance.instancePath.absoluteDataManagementPath, inputResultFile.name)

            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteDataManagementPath
            deleteResultFileIfExists(checkpointFile, realm)

            final Map<String, String> environmentVariables = [
                    CONFIG_FILE: configFileInProjectDirectory.absolutePath,
                    pid: instance.individual.pid,
                    PID: instance.individual.pid,
                    TOOL_ID: "snvFilter",
                    SNVFILE_PREFIX: "snvs_",
                    TUMOR_BAMFILE_FULLPATH_BP: abstractMergedBamFileService.getExistingBamFilePath(instance.sampleType1BamFile).absolutePath,
                    FILENAME_VCF: inputFileCopy.absolutePath,
                    FILENAME_CHECKPOINT: checkpointFile.absolutePath,
            ]

            final StringBuilder script = new StringBuilder()
            if (inputFileCopy.absolutePath != inputResultFile.absolutePath) {
                script << "ln -sf ${inputResultFile.absolutePath} ${inputFileCopy.absolutePath}; "
            }
            script << "${step.getExternalScript(config.externalScriptVersion).scriptFilePath}; "
            // In case the input file had to be linked to the output folder it has to be deleted afterwards.
            // Otherwise it would be twice in the file system.
            if (inputFileCopy.absolutePath != inputResultFile.absolutePath) {
                script << "rm -f ${inputFileCopy.absolutePath}"
            }

            pbsService.executeJob(realm, script.toString(), environmentVariables)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance)
            return NextAction.SUCCEED
        }
    }

    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {
        validateNonCallingJobs(instance, step)
    }
}
