package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvJobResult
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.job.processing.AbstractOtpJob
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.LinkFileUtils
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static de.dkfz.tbi.otp.utils.WaitingFileUtils.*
import static org.springframework.util.Assert.notNull

abstract class AbstractSnvCallingJob extends AbstractOtpJob {

    @Autowired
    ExecutionService executionService
    @Autowired
    ConfigService configService
    @Autowired
    LsdfFilesService lsdfFilesService
    @Autowired
    LinkFileUtils linkFileUtils

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


    void deleteResultFileIfExists(final File resultFile, Realm realm) {
        if (waitUntilExists(resultFile)) {
            log.info "Result file ${resultFile} already exists. Presumably from an earlier, failed execution of this job. Will delete it."
            executionService.executeCommand(realm, "rm ${resultFile.path}")
        }
    }

    /**
     * Writes the config file in the staging dir, copies it to the project directory, deletes it from the staging dir and returns its path.
     * If the file already exists in the project directory the method just checks that its content is as expected.
     * If the config file already exists in the staging directory the method copies it to the project directory
     * and checks if the content is as expected.
     */
    File writeConfigFile(final SnvCallingInstance instance) {
        notNull(instance, "The input for method writeConfigFile is null")
        final Realm realm = configService.getRealmDataProcessing(instance.project)
        final File configFileInStagingDirectory = instance.configFilePath.absoluteStagingPath
        final File configFileInProjectDirectory = instance.configFilePath.absoluteDataManagementPath

        if (waitUntilExists(configFileInProjectDirectory)) {
            assertDataManagementConfigContentsOk(instance)
            return configFileInProjectDirectory
        }
        if (waitUntilDoesNotExist(configFileInStagingDirectory)) {
            lsdfFilesService.createDirectory(configFileInStagingDirectory.parentFile, instance.project)
            assert waitUntilExists(configFileInStagingDirectory.parentFile, extendedWaitingTime)
            instance.config.writeToFile(configFileInStagingDirectory)
        }
        lsdfFilesService.ensureFileIsReadableAndNotEmpty(configFileInStagingDirectory, extendedWaitingTime)
        assertStagingConfigContentsOk(instance)

        String command ="""
mkdir -p ${configFileInProjectDirectory.parent}; \
chmod 2750 ${configFileInProjectDirectory.parent}; \
cp ${configFileInStagingDirectory} ${configFileInProjectDirectory}; \
chmod 640 ${configFileInProjectDirectory}; \
rm ${configFileInStagingDirectory}
"""
        executionService.executeCommand(realm, command)

        assert waitUntilExists(configFileInProjectDirectory, extendedWaitingTime)
        assertDataManagementConfigContentsOk(instance)

        return configFileInProjectDirectory
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
        File resultFile = result.getResultFilePath().absoluteDataManagementPath
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

    protected File getExistingBamFilePath(final AbstractMergedBamFile bamFile) {
        final File file = new File(AbstractMergedBamFileService.destinationDirectory(bamFile), bamFile.bamFileName)
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
    protected void checkIfResultFilesExistsOrThrowException(SnvCallingInstance instance) {
        final boolean instanceWithResultExists = instance.findLatestResultForSameBamFiles(step) != null
        if (!instanceWithResultExists) {
            throw new RuntimeException("This SNV workflow instance is configured not to do the SNV ${step.name()} and no non-withdrawn SNV ${step.name()} was done before, so subsequent jobs will have no input.")
        }
    }

    protected String getSnvPBSOptionsNameSeqTypeSpecific(SeqType seqType) {
        notNull(seqType, "The input seqType must not be null in method getSnvPBSOptionsNameSeqTypeSpecific")
        if (seqType.name == SeqTypeNames.WHOLE_GENOME.seqTypeName) {
            return "snvPipeline_${step.name()}_WGS"
        } else if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            return "snvPipeline_${step.name()}_WES"
        } else {
            throw new RuntimeException("There are no PBS Options available for the SNV pipeline for seqtype ${seqType}")
        }
    }



    /** checks if the on-disk staging config file matches the expected config for this instance */
    protected static void assertStagingConfigContentsOk(SnvCallingInstance instance) {
        assert instance.configFilePath.absoluteStagingPath.text == instance.config.configuration
    }

    /** checks if the on-disk project-folder config file matches the expected config for this instance */
    protected static void assertDataManagementConfigContentsOk(SnvCallingInstance instance) {
        assert instance.configFilePath.absoluteDataManagementPath.text == instance.config.configuration
    }

    protected void confirmCheckPointFileExistsAndDeleteIt(SnvCallingInstance instance, SnvCallingStep step) {
        final File checkpointFile = step.getCheckpointFilePath(instance).absoluteDataManagementPath
        assert waitUntilExists(checkpointFile, extendedWaitingTime)
        deleteResultFileIfExists(checkpointFile, configService.getRealmDataProcessing(instance.project))
        assert waitUntilDoesNotExist(checkpointFile)
    }

    /**
     * Contains the checks which are similar for the Annotation, DeepAnnotation and Filter step.
     * This method can be called within the validate method of each SNV-Job except for the Calling step.
     */
    protected void validateNonCallingJobs(SnvCallingInstance instance, SnvCallingStep step) {
        assertDataManagementConfigContentsOk(instance)
        confirmCheckPointFileExistsAndDeleteIt(instance, step)

        try {
            SnvJobResult inputResult = getSnvJobResult(instance).inputResult
            File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)
        } catch (final AssertionError e) {
            throw new RuntimeException("The input result file for step ${step.name()} has changed on the file system while this job processed them.", e)
        }
        // mark the result of the snv annotation step as finished
        changeProcessingStateOfJobResult(instance, SnvProcessingStates.FINISHED)
    }

}
