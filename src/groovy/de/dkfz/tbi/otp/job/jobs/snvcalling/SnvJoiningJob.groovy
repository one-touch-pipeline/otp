package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm

class SnvJoiningJob extends AbstractSnvCallJoinJob {

    @Override
    protected void submit(SnvJobResult jobResult, Realm realm, Closure sendClusterScript) throws Throwable {
        SnvCallingInstance instance = jobResult.snvCallingInstance

        //if all SnvCallings per chromosome are finished they can be merged together
        File vcfRawFile = jobResult.getResultFilePath().absoluteDataManagementPath
        // In case the file exists already from an earlier -not successful- run it should be deleted first
        deleteResultFileIfExists(vcfRawFile, realm)
        String allChromosomeFilePaths = getChromosomeResultFiles(jobResult).values().join(" ")

        final String qsubParametersToMergeVcfs =
                "TOOL_ID=snvVcfJoin," +
                "FILENAME_VCF_RAW=${vcfRawFile}," +
                "VCF_FOR_SNV_FILES=\\'(${allChromosomeFilePaths})\\'"
        final String script =
                "${ensureFileHasExpectedSizeScript(getExistingBamFilePath(instance.sampleType1BamFile), instance.sampleType1BamFile.fileSize)}" +
                "${ensureFileHasExpectedSizeScript(getExistingBamFilePath(instance.sampleType2BamFile), instance.sampleType2BamFile.fileSize)}" +
                "${ensureFileDoesNotExistScript(vcfRawFile)}" +
                "${jobResult.chromosomeJoinExternalScript.scriptFilePath.path}; " +
                "md5sum ${vcfRawFile} > ${vcfRawFile}.md5sum"
        sendClusterScript(script, qsubParametersToMergeVcfs)
    }

    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {

        validateConfigFileAndInputBamFiles(instance)

        // check if the final vcf result file exists
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(getSnvJobResult(instance).getResultFilePath().absoluteDataManagementPath)

        changeProcessingStateOfJobResult(instance, SnvProcessingStates.FINISHED)
    }
}
