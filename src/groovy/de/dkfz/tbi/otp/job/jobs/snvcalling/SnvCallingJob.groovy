package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob

import static de.dkfz.tbi.otp.job.processing.CreateClusterScriptService.*

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm

class SnvCallingJob extends AbstractSnvCallJoinJob implements AutoRestartableJob {

    @Override
    protected void submit(SnvJobResult jobResult, Realm realm, Closure sendClusterScript) throws Throwable {
        SnvCallingInstance instance = jobResult.snvCallingInstance

        //submit one SnvCalling run per chromosome
        getChromosomeResultFiles(jobResult).each { String chromosome, File chromosomeResultFile ->
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(chromosomeResultFile, realm)
            final String qsubParametersChromosomeSpecific =
                    "TOOL_ID=snvCalling," +
                    "PARM_CHR_INDEX=${chromosome}," +
                    "FILENAME_VCF_SNVS=${chromosomeResultFile}"
            final String script =
                    ensureFileHasExpectedSizeScript(getExistingBamFilePath(instance.sampleType1BamFile), instance.sampleType1BamFile.fileSize) +
                    ensureFileHasExpectedSizeScript(getExistingBamFilePath(instance.sampleType2BamFile), instance.sampleType2BamFile.fileSize) +
                    ensureFileDoesNotExistScript(chromosomeResultFile) +
                    jobResult.externalScript.scriptFilePath
            sendClusterScript(script, qsubParametersChromosomeSpecific)
        }
    }


    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {

        validateConfigFileAndInputBamFiles(instance)

        // check if the chromosome vcf result files exist
        instance.config.evaluate()
        getChromosomeResultFiles(getSnvJobResult(instance)).values().each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
        }
    }

    @Override
    protected void linkPreviousResults(SnvCallingInstance instance, Realm realm) {
        // The result file will be linked in the SNV Joining job
    }
}
