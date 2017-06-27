package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFileService
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.processing.ClusterJobSchedulerService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExternalScript

abstract class AbstractSnvCallJoinJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService
    @Autowired
    AbstractMergedBamFileService abstractMergedBamFileService
    @Autowired
    SnvCallingService snvCallingService

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
        final Realm realm = configService.getRealmDataProcessing(instance.project)

        if (config.getExecuteStepFlag(step)) {
            ExternalScript externalScriptJoining = ExternalScript.getLatestVersionOfScript(CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER, config.externalScriptVersion)
            SnvJobResult jobResult = createAndSaveSnvJobResult(instance, step.getExternalScript(config.externalScriptVersion), externalScriptJoining)

            final File configFileInProjectDirectory = writeConfigFile(instance)

            final File sampleType1BamFilePath = abstractMergedBamFileService.getExistingBamFilePath(instance.sampleType1BamFile)
            final File sampleType2BamFilePath = abstractMergedBamFileService.getExistingBamFilePath(instance.sampleType2BamFile)
            final Map<String, String> generalEnvironmentVariables = [
                    CONFIG_FILE: configFileInProjectDirectory.absolutePath,
                    pid: instance.individual.pid,
                    PID: instance.individual.pid,
                    TUMOR_BAMFILE_FULLPATH_BP: sampleType1BamFilePath.absolutePath,
                    CONTROL_BAMFILE_FULLPATH_BP: sampleType2BamFilePath.absolutePath,
            ]

            submit(jobResult, realm, { String clusterScript, Map specificEnvironmentVariables ->
                generalEnvironmentVariables.putAll(specificEnvironmentVariables)
                clusterJobSchedulerService.executeJob(realm, clusterScript, generalEnvironmentVariables)
            })

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance)
            linkPreviousResults(instance, realm)
            return NextAction.SUCCEED
        }
    }

    protected abstract void submit(SnvJobResult jobResult, Realm realm, Closure sendScript)

    protected void validateConfigFileAndInputBamFiles(final SnvCallingInstance instance) throws Throwable {
        assertDataManagementConfigContentsOk(instance)
        snvCallingService.validateInputBamFiles(instance)
    }

}
