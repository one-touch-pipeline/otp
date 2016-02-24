package de.dkfz.tbi.otp.job.jobs.snvcalling

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExternalScript

abstract class AbstractSnvCallJoinJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecutionHelperService executionHelperService

    final static String CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER = "CHROMOSOME_VCF_JOIN"

    @Override
    SnvCallingStep getStep() {
        return SnvCallingStep.CALLING
    }

    @Override
    public SnvCallingStep getPreviousStep() {
        return null
    }

    protected Map<String, File> getChromosomeResultFiles(SnvJobResult jobResult) {
        return jobResult.snvCallingInstance.config.chromosomeNames.collectEntries { [
                (it): jobResult.getResultFilePath(it).absoluteStagingPath
        ] }
    }

    @Override
    protected NextAction maybeSubmit(final SnvCallingInstance instance) throws Throwable {
        final SnvConfig config = instance.config.evaluate()
        if (config.getExecuteStepFlag(step)) {
            ExternalScript externalScriptJoining = ExternalScript.getLatestVersionOfScript(CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER, config.externalScriptVersion)
            SnvJobResult jobResult = createAndSaveSnvJobResult(instance, step.getExternalScript(config.externalScriptVersion), externalScriptJoining)

            final Realm realm = configService.getRealmDataProcessing(instance.project)

            final File configFileInProjectDirectory = writeConfigFile(instance)

            final File sampleType1BamFilePath = getExistingBamFilePath(instance.sampleType1BamFile)
            final File sampleType2BamFilePath = getExistingBamFilePath(instance.sampleType2BamFile)
            final String qsubParametersGeneral =
                    "CONFIG_FILE=${configFileInProjectDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TUMOR_BAMFILE_FULLPATH_BP=${sampleType1BamFilePath}," +
                    "CONTROL_BAMFILE_FULLPATH_BP=${sampleType2BamFilePath}"

            submit(jobResult, realm, { String clusterScript, String specificQsubParameters ->
                executionHelperService.sendScript(realm, clusterScript, "{" +
                        "'-v': '${qsubParametersGeneral},${specificQsubParameters}'" +
                        "}")
            })

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance)
            return NextAction.SUCCEED
        }
    }

    protected abstract void submit(SnvJobResult jobResult, Realm realm, Closure sendScript)

    protected void validateConfigFileAndInputBamFiles(final SnvCallingInstance instance) throws Throwable {

        assertDataManagementConfigContentsOk(instance)

        try {
            getExistingBamFilePath(instance.sampleType1BamFile)
            getExistingBamFilePath(instance.sampleType2BamFile)
        } catch (final AssertionError e) {
            throw new RuntimeException('The input BAM files have changed on the file system while this job processed them.', e)
        }
    }
}
