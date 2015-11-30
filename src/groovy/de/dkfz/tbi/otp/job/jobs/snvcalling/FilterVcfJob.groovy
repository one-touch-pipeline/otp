package de.dkfz.tbi.otp.job.jobs.snvcalling

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.ngsdata.*

class FilterVcfJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecutionHelperService executionHelperService

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

            createAndSaveSnvJobResult(instance, step.getExternalScript(config.externalScriptVersion), null, inputResult)

            // Check that all needed files are existing
            final File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)

            final File configFileInProjectDirectory = writeConfigFile(instance)

            // the filter script of the CO group writes its output in the same folder where its input is,
            // so we link the input file to output folder if they are different
            File inputFileCopy = new File(instance.snvInstancePath.absoluteDataManagementPath, inputResultFile.name)

            final Realm realm = configService.getRealmDataProcessing(instance.project)

            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteDataManagementPath
            deleteResultFileIfExists(checkpointFile, realm)

            final String qsubParameters="{ '-v': '"+
                    "CONFIG_FILE=${configFileInProjectDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TOOL_ID=snvFilter," +
                    "SNVFILE_PREFIX=snvs_," +
                    "TUMOR_BAMFILE_FULLPATH_BP=${getExistingBamFilePath(instance.sampleType1BamFile)}," +
                    "FILENAME_VCF=${inputFileCopy}," +
                    "FILENAME_CHECKPOINT=${checkpointFile}" +
                    "'}"

            final StringBuilder script = new StringBuilder()
            if (inputFileCopy.absolutePath != inputResultFile.absolutePath) {
                script << "ln -s ${inputResultFile.absolutePath} ${inputFileCopy.absolutePath}; "
            }
            script << "${step.getExternalScript(config.externalScriptVersion).scriptFilePath}; "
            // In case the input file had to be linked to the output folder it has to be deleted afterwards.
            // Otherwise it would be twice in the file system.
            if (inputFileCopy.absolutePath != inputResultFile.absolutePath) {
                script << "rm -f ${inputFileCopy.absolutePath}"
            }

            executionHelperService.sendScript(realm, script.toString(), qsubParameters)

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
