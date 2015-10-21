package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.job.processing.ExecutionService
import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

class SnvCallingJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecutionHelperService executionHelperService
    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService
    @Autowired
    ExecutionService executionService

    final static String CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER = "CHROMOSOME_VCF_JOIN"

    @Override
    SnvCallingStep getStep() {
        return SnvCallingStep.CALLING
    }

    @Override
    public SnvCallingStep getPreviousStep() {
        return null
    }

    @Override
    protected NextAction maybeSubmit(final SnvCallingInstance instance) throws Throwable {
        final SnvConfig config = instance.config.evaluate()
        if (config.getExecuteStepFlag(step)) {
            ExternalScript externalScriptJoining = ExternalScript.getLatestVersionOfScript(CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER, config.externalScriptVersion)
            createAndSaveSnvJobResult(instance, step.getExternalScript(config.externalScriptVersion), externalScriptJoining)

            final List<String> executedClusterJobsPerChromosome = []
            final Realm realm = configService.getRealmDataProcessing(instance.project)
            final String pbsOptionName = getSnvPBSOptionsNameSeqTypeSpecific(instance.seqType)

            // write the config file in the project directory
            final File configFileInProjectDirectory = writeConfigFile(instance)

            //create the parameters for calling the SnvCalling.sh per chromosome
            final File sampleType1BamFilePath = getExistingBamFilePath(instance.sampleType1BamFile)
            final File sampleType2BamFilePath = getExistingBamFilePath(instance.sampleType2BamFile)
            final String qsubParametersGeneral =
                    "CONFIG_FILE=${configFileInProjectDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TUMOR_BAMFILE_FULLPATH_BP=${sampleType1BamFilePath}," +
                    "CONTROL_BAMFILE_FULLPATH_BP=${sampleType2BamFilePath}"

            //submit one SnvCalling run per chromosome
            final List<String> chromosomeFilePaths = []
            config.chromosomeNames.each { String chromosome ->
                final File chromosomeResultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual, chromosome)).absoluteStagingPath
                // In case the file exists already from an earlier -not successful- run it should be deleted first
                deleteResultFileIfExists(chromosomeResultFile, realm)
                chromosomeFilePaths.add(chromosomeResultFile)
                final String qsubParametersChromosomeSpecific =
                        "TOOL_ID=snvCalling," +
                        "PARM_CHR_INDEX=${chromosome}," +
                        "FILENAME_VCF_SNVS=${chromosomeResultFile}"
                final String qsubParameters = "{" +
                        "'-v': '${qsubParametersGeneral},${qsubParametersChromosomeSpecific}'" +
                        "}"
                final String script =
                        ensureFileHasExpectedSizeScript(sampleType1BamFilePath, instance.sampleType1BamFile.fileSize) +
                        ensureFileHasExpectedSizeScript(sampleType2BamFilePath, instance.sampleType2BamFile.fileSize) +
                        ensureFileDoesNotExistScript(chromosomeResultFile) +
                        step.getExternalScript(config.externalScriptVersion).scriptFilePath
                executedClusterJobsPerChromosome.add(executionHelperService.sendScript(realm, script, pbsOptionName, qsubParameters))
            }

            //if all SnvCallings per chromosome are finished they can be merged together
            File vcfRawFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual, null)).absoluteDataManagementPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(vcfRawFile, realm)
            String allChromosomeFilePaths = chromosomeFilePaths.join(" ")

            final String qsubParametersDependency = "{'depend': 'afterok:${executedClusterJobsPerChromosome.join(":")}'}"
            final String qsubParametersToMergeVcfs =
                    "TOOL_ID=snvVcfJoin," +
                    "FILENAME_VCF_RAW=${vcfRawFile}," +
                    "VCF_FOR_SNV_FILES=\\'(${allChromosomeFilePaths})\\'"
            final String qsubParameters = "{" +
                    "'-W': ${qsubParametersDependency}, " +
                    "'-v': '${qsubParametersGeneral},${qsubParametersToMergeVcfs}'" +
                    "}"
            final String script =
                    "sleep 5m; " +
                    "${ensureFileHasExpectedSizeScript(sampleType1BamFilePath, instance.sampleType1BamFile.fileSize)}" +
                    "${ensureFileHasExpectedSizeScript(sampleType2BamFilePath, instance.sampleType2BamFile.fileSize)}" +
                    "${ensureFileDoesNotExistScript(vcfRawFile)}" +
                    "${externalScriptJoining.scriptFilePath.path}; " +
                    "md5sum ${vcfRawFile} > ${vcfRawFile}.md5sum"
            executionHelperService.sendScript(realm, script, pbsOptionName, qsubParameters)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance)
            return NextAction.SUCCEED
        }
    }


    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {
        assertDataManagementConfigContentsOk(instance)

        final SnvConfig config = instance.config.evaluate()

        // check if the final vcf result file exists
        final OtpPath resultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual, null))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFile.absoluteDataManagementPath)

        try {
            getExistingBamFilePath(instance.sampleType1BamFile)
            getExistingBamFilePath(instance.sampleType2BamFile)
        } catch (final AssertionError e) {
            throw new RuntimeException('The input BAM files have changed on the file system while this job processed them.', e)
        }

        changeProcessingStateOfJobResult(instance, SnvProcessingStates.FINISHED)
    }
}
