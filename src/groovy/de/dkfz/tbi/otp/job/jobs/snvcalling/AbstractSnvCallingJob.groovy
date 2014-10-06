package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMaybeSubmitWaitValidateJob
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.utils.ExternalScript

abstract class AbstractSnvCallingJob extends AbstractMaybeSubmitWaitValidateJob {

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

    void createAndSaveSnvJobResult(final SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult = null) {
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
            assert resultInProgress.save()
        } else {
            final SnvJobResult result = new SnvJobResult(
                    snvCallingInstance: instance,
                    step: step,
                    inputResult: inputResult,
                    processingState: SnvProcessingStates.IN_PROGRESS,
                    externalScript: externalScript
                    )
            assert result.save()
        }
    }

    void changeProcessingStateOfJobResult(final SnvCallingInstance instance, SnvProcessingStates newState) {
        notNull(instance)
        notNull(newState)
        SnvJobResult result = exactlyOneElement(
                SnvJobResult.findAllBySnvCallingInstanceAndStepAndWithdrawn(instance, step, false)
                )
        assert result.processingState == SnvProcessingStates.IN_PROGRESS

        result.processingState = newState
        assert result.save()
    }
}
