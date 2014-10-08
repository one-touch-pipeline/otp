package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExternalScript

class SnvCallingJob extends AbstractSnvCallingJob {

    @Autowired
    ConfigService configService
    @Autowired
    ExecutionHelperService executionHelperService
    @Autowired
    CreateClusterScriptService createClusterScriptService
    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    final static String CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER = "CHROMOSOME_VCF_JOIN"

    @Override
    SnvCallingStep getStep() {
        return SnvCallingStep.CALLING
    }

    @Override
    protected NextAction maybeSubmit(final SnvCallingInstance instance) throws Throwable {
        final SnvConfig config = instance.config.evaluate()
        if (config.getExecuteStepFlag(step)) {
            final List<String> executedClusterJobsPerChromosome = []
            final Realm realm = configService.getRealmDataProcessing(instance.project)

            // write the config file in the staging directory
            final File configFileInStagingDirectory = writeConfigFile(instance)

            //create the parameters for calling the SnvCalling.sh per chromosome
            final File sampleType1BamFilePath = getExistingBamFilePath(instance.sampleType1BamFile)
            final File sampleType2BamFilePath = getExistingBamFilePath(instance.sampleType2BamFile)
            final String qsubParametersGeneral =
                    "CONFIG_FILE=${configFileInStagingDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TUMOR_BAMFILE_FULLPATH_BP=${sampleType1BamFilePath}," +
                    "CONTROL_BAMFILE_FULLPATH_BP=${sampleType2BamFilePath}"

            //submit one SnvCalling run per chromosome
            final List<String> chromosomeFilePaths = []
            config.chromosomeNames.each { String chromosome ->
                final File chromosomeResultFile = new OtpPath(instance.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(instance.individual, chromosome)).absoluteStagingPath
                // In case the file exists already from an earlier -not successful- run it should be deleted first
                deleteResultFileIfExists(chromosomeResultFile)
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
                        step.externalScript.scriptFilePath
                executedClusterJobsPerChromosome.add(executionHelperService.sendScript(realm, script, null, qsubParameters))
            }

            //if all SnvCallings per chromosome are finished they can be merged together
            ExternalScript externalScriptJoining = ExternalScript.getLatestVersionOfScript(CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER)
            File vcfRawFile = new OtpPath(instance.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(instance.individual, null)).absoluteStagingPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(vcfRawFile)
            String allChromosomeFilePaths = chromosomeFilePaths.join(" ")

            final String qsubParametersDependency = "{'depend': 'afterok:${executedClusterJobsPerChromosome.join(",")}'}"
            final String qsubParametersToMergeVcfs =
                    "TOOL_ID=snvVcfJoin," +
                    "FILENAME_VCF_RAW=${vcfRawFile}," +
                    "VCF_FOR_SNV_FILES=\\'(${allChromosomeFilePaths})\\'"
            final String qsubParameters = "{" +
                    "'-W': ${qsubParametersDependency}, " +
                    "'-v': '${qsubParametersGeneral},${qsubParametersToMergeVcfs}'" +
                    "}"
            final String script =
                    ensureFileHasExpectedSizeScript(sampleType1BamFilePath, instance.sampleType1BamFile.fileSize) +
                    ensureFileHasExpectedSizeScript(sampleType2BamFilePath, instance.sampleType2BamFile.fileSize) +
                    ensureFileDoesNotExistScript(vcfRawFile) +
                    externalScriptJoining.scriptFilePath.path
            executionHelperService.sendScript(realm, script, null, qsubParameters)

            createAndSaveSnvJobResult(instance, step.externalScript)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance, true)
            return NextAction.SUCCEED
        }
    }


    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {
        assert instance.configFilePath.absoluteStagingPath.text == instance.config.configuration
        final SnvConfig config = instance.config.evaluate()
        // check if the chromosome vcf result file exist
        config.chromosomeNames.each { String chromosome ->
            final OtpPath resultFilePerChromosome = new OtpPath(instance.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(instance.individual, chromosome))
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFilePerChromosome.absoluteStagingPath)
        }
        // check if the final vcf result file exists
        final OtpPath resultFile = new OtpPath(instance.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(instance.individual, null))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFile.absoluteStagingPath)

        try {
            getExistingBamFilePath(instance.sampleType1BamFile)
            getExistingBamFilePath(instance.sampleType2BamFile)
        } catch (final AssertionError e) {
            throw new RuntimeException('The input BAM files have changed on the file system while this job processed them.', e)
        }

        changeProcessingStateOfJobResult(instance, SnvProcessingStates.FINISHED)

        //paths for the result file
        List<File> sourceLocation = [
            resultFile.absoluteStagingPath
        ]
        List<File> targetLocation = [
            resultFile.absoluteDataManagementPath
        ]
        List<File> linkLocation = [
            instance.sampleTypeCombination.snvCallingFileLinkedPath.absoluteDataManagementPath
        ]
        //path for the config file
        sourceLocation.add(instance.configFilePath.absoluteStagingPath)
        targetLocation.add(instance.configFilePath.absoluteDataManagementPath)
        linkLocation.add(instance.getStepConfigFileLinkedPath(step).absoluteDataManagementPath)
        String transferClusterScript = createClusterScriptService.createTransferScript(sourceLocation, targetLocation, linkLocation, true)
        //parameter for copying job
        final Realm realm = configService.getRealmDataProcessing(instance.project)
        addOutputParameter(REALM, realm.id.toString())
        addOutputParameter(SCRIPT, transferClusterScript)
    }
}
