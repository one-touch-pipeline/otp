package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.job.jobs.AutoRestartable
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvJobResult
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

class SnvDeepAnnotationJob extends AbstractSnvCallingJob implements AutoRestartable {

    @Autowired
    ConfigService configService
    @Autowired
    PbsService pbsService


    @Override
    SnvCallingStep getStep() {
        return SnvCallingStep.SNV_DEEPANNOTATION
    }

    SnvCallingStep getPreviousStep() {
        return SnvCallingStep.SNV_ANNOTATION
    }

    @Override
    protected NextAction maybeSubmit(final SnvCallingInstance instance) throws Throwable {
        final SnvConfig config = instance.config.evaluate()
        if (config.getExecuteStepFlag(step)) {
            // Get results from the previous (annotation) step
            SnvJobResult inputResult = instance.findLatestResultForSameBamFiles(previousStep)
            assert inputResult

            createAndSaveSnvJobResult(instance, step.getExternalScript(config.externalScriptVersion), null, inputResult)

            /*
             * Check that the needed input file exists.
             */
            final File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(inputResultFile)

            final File configFileInProjectDirectory = writeConfigFile(instance)

            final Realm realm = configService.getRealmDataProcessing(instance.project)

            /*
             * Input which is needed for the DeepAnnotation script. It is just for Roddy intern job system handling.
             * File can be deleted afterwards. Delete it if it exists already.
             */
            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteDataManagementPath
            deleteResultFileIfExists(checkpointFile, realm)

            // Get path of the snv deep annotation result file
            final File deepAnnotationResultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual)).absoluteDataManagementPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(deepAnnotationResultFile, realm)

            /*
             * The snv deep annotation script does not use the output vcf parameter but just overrides the input file.
             * Therefore the snv annotation file has to be copied so that it has the correct naming.
             * It was decided not to override the snv annotation file to be on the safe side.
             */
            final String qsubParameters="{ '-v': '"+
                    "CONFIG_FILE=${configFileInProjectDirectory}," +
                    "pid=${instance.individual.pid}," +
                    "PID=${instance.individual.pid}," +
                    "TOOL_ID=snvDeepAnnotation," +
                    "PIPENAME=SNV_DEEPANNOTATION," +
                    "FILENAME_VCF=${deepAnnotationResultFile}," +
                    "FILENAME_VCF_SNVS=${deepAnnotationResultFile}," +
                    "FILENAME_CHECKPOINT=${checkpointFile}," +
                    "'}"
            final String script = "cp ${inputResultFile} ${deepAnnotationResultFile}; " +
                    "${step.getExternalScript(config.externalScriptVersion).scriptFilePath}; " +
                    "md5sum ${deepAnnotationResultFile} > ${deepAnnotationResultFile}.md5sum"

            pbsService.executeJob(realm, script, qsubParameters)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance)
            return NextAction.SUCCEED
        }
    }

    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {

        // check if the final vcf result file exists
        final OtpPath resultFile = new OtpPath(instance.snvInstancePath, step.getResultFileName(instance.individual))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFile.absoluteDataManagementPath)

        validateNonCallingJobs(instance, step)
    }
}
