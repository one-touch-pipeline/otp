package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static org.springframework.util.Assert.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMaybeSubmitWaitValidateJob
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.utils.ExternalScript

abstract class AbstractSnvCallingJob extends AbstractMaybeSubmitWaitValidateJob {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    abstract SnvCallingStep getStep()

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        SnvCallingInstance.withTransaction {
            final SnvCallingInstance instance = getProcessParameterObject()
            assert instance.processingState == SnvProcessingStates.IN_PROGRESS
            assert !instance.sampleType1BamFile.withdrawn
            assert !instance.sampleType2BamFile.withdrawn
            return maybeSubmit(instance)
        }
    }

    protected abstract NextAction maybeSubmit(SnvCallingInstance instance) throws Throwable

    @Override
    protected final void validate() throws Throwable {
        SnvCallingInstance.withTransaction {
            final SnvCallingInstance instance = getProcessParameterObject()
            assert instance.processingState == SnvProcessingStates.IN_PROGRESS
            validate(instance)
        }
    }

    protected abstract void validate(SnvCallingInstance instance) throws Throwable

    void deleteResultFileIfExists(final File resultFile) {
        if (resultFile.exists()) {
            log.info "Result file ${resultFile} already exists. Presumably from an earlier, failed execution of this job. Will delete it."
            assert resultFile.delete()
        }
    }

    /**
     * Writes the config file in the staging directory and returns its path.
     * If the file already exists, the method ensures that its content is as expected.
     */
    File writeConfigFile(final SnvCallingInstance instance) {
        notNull(instance, "The input for method writeConfigFile is null")
        final File configFileInStagingDirectory = instance.configFilePath.absoluteStagingPath
        if (!configFileInStagingDirectory.exists()) {
            configFileInStagingDirectory.parentFile.mkdirs()
            instance.config.writeToFile(configFileInStagingDirectory)
        }
        assert configFileInStagingDirectory.text == instance.config.configuration
        return configFileInStagingDirectory
    }

    void createAndSaveSnvJobResult(final SnvCallingInstance instance, ExternalScript externalScript, ExternalScript externalScriptJoining, SnvJobResult inputResult = null) {
        // In case the validation step failed there exists already one result file for this instance. This has to be reused.
        SnvJobResult resultInProgress = atMostOneElement(
                SnvJobResult.findAllBySnvCallingInstanceAndStep(instance, step)
                )

        if (resultInProgress) {
            assert resultInProgress.processingState == SnvProcessingStates.IN_PROGRESS
            assert !resultInProgress.withdrawn
            // It can not happen that another SnvCallingInstance for this SampleTypeCombination starts while this one is not finished.
            assert resultInProgress.inputResult == inputResult

            resultInProgress.externalScript = externalScript
            resultInProgress.chromosomeJoinExternalScript = externalScriptJoining
            assert resultInProgress.save()
        } else {
            final SnvJobResult result = new SnvJobResult(
                    snvCallingInstance: instance,
                    step: step,
                    inputResult: inputResult,
                    processingState: SnvProcessingStates.IN_PROGRESS,
                    externalScript: externalScript,
                    chromosomeJoinExternalScript: externalScriptJoining
                    )
            assert result.save()
        }
    }

    void changeProcessingStateOfJobResult(final SnvCallingInstance instance, SnvProcessingStates newState) {
        notNull(instance)
        notNull(newState)
        SnvJobResult result = getSnvJobResult(instance)

        result.processingState = newState
        assert result.save()
    }

    SnvJobResult getSnvJobResult(SnvCallingInstance instance) {
        notNull(instance)
        SnvJobResult result = exactlyOneElement(
                SnvJobResult.findAllBySnvCallingInstanceAndStepAndWithdrawn(instance, step, false)
                )
        assert result.processingState == SnvProcessingStates.IN_PROGRESS
        return result
    }

    protected File getExistingBamFilePath(final ProcessedMergedBamFile bamFile) {
        final File file = new File(processedMergedBamFileService.destinationDirectory(bamFile), processedMergedBamFileService.fileName(bamFile))
        assert bamFile.fileExists
        assert bamFile.md5sum ==~ /^[0-9a-fA-F]{32}$/
        assert bamFile.fileSize > 0L
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
        assert file.length() == bamFile.fileSize
        return file
    }

    /**
     * This SNV workflow instance is configured not to do the SNV calling.
     * Make sure there already is a result that subsequent jobs can use as input.
     */
    protected void checkIfResultFilesExistsOrThrowException(SnvCallingInstance instance, boolean addBlankOutputParameters) {
        final boolean instanceWithResultExists = instance.findLatestResultForSameBamFiles(step) != null
        if (instanceWithResultExists) {
            log.info "This SNV workflow instance is configured not to do the SNV ${step.name()}. Subsequent jobs will use the results of a previous run as input."
            if (addBlankOutputParameters) {
                addOutputParameter(REALM, "")
                addOutputParameter(SCRIPT, "")
            }
        } else {
            throw new RuntimeException("This SNV workflow instance is configured not to do the SNV ${step.name()} and no non-withdrawn SNV ${step.name()} was done before, so subsequent jobs will have no input.")
        }
    }
}
