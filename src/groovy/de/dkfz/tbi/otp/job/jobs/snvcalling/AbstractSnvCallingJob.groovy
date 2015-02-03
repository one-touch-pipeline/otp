package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames

import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static org.springframework.util.Assert.*
import static de.dkfz.tbi.otp.utils.WaitingFileUtils.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
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
    @Autowired
    ExecutionService executionService
    @Autowired
    ConfigService configService
    @Autowired
    CreateClusterScriptService scriptService
    @Autowired
    LsdfFilesService lsdfFilesService

    abstract SnvCallingStep getStep()
    abstract SnvCallingStep getPreviousStep()

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
        confirmDoesNotExist(configFileInStagingDirectory)
        if (!configFileInStagingDirectory.exists()) {
            lsdfFilesService.createDirectory(configFileInStagingDirectory.parentFile, instance.project)
            assert confirmExists(configFileInStagingDirectory.parentFile)
            instance.config.writeToFile(configFileInStagingDirectory)
        }
        assert confirmExists(configFileInStagingDirectory)
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
            // It can not happen that another SnvCallingInstance for this sample pair starts while this one is not finished.
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
        if ([SnvCallingStep.CALLING, SnvCallingStep.SNV_DEEPANNOTATION].contains(step)) {
            addFileInformationToJobResult(result)
        }
        result.processingState = newState
        assert result.save()
    }

    void addFileInformationToJobResult(SnvJobResult result) {
        File resultFile = result.getResultFilePath().absoluteStagingPath
        File md5sumFile = new File("${resultFile.path}.md5sum")
        result.fileSize = resultFile.size()
        result.md5sum = md5sumFile.text.split(" ")[0]
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

    protected String getSnvPBSOptionsNameSeqTypeSpecific(SeqType seqType) {
        notNull(seqType, "The input seqType must not be null in method getSnvPBSOptionsNameSeqTypeSpecific")
        if (seqType.name == SeqTypeNames.WHOLE_GENOME.seqTypeName) {
            return "snvPipeline_WGS"
        } else if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            return "snvPipeline_WES"
        } else {
            throw new RuntimeException("There are no PBS Options available for the SNV pipeline for seqtype ${seqType}")
        }
    }

}
