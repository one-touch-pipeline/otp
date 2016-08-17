package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

@Component
@Scope("prototype")
@UseJobLog
class SnvDeepAnnotationJob extends AbstractSnvCallingJob implements AutoRestartableJob {

    @Autowired
    ConfigService configService
    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService


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
        final Realm realm = configService.getRealmDataProcessing(instance.project)

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



            /*
             * Input which is needed for the DeepAnnotation script. It is just for Roddy intern job system handling.
             * File can be deleted afterwards. Delete it if it exists already.
             */
            final File checkpointFile = step.getCheckpointFilePath(instance).absoluteDataManagementPath
            deleteResultFileIfExists(checkpointFile, realm)

            // Get path of the snv deep annotation result file
            final File deepAnnotationResultFile = new OtpPath(instance.instancePath, step.getResultFileName(instance.individual)).absoluteDataManagementPath
            // In case the file exists already from an earlier -not successful- run it should be deleted first
            deleteResultFileIfExists(deepAnnotationResultFile, realm)

            /*
             * The snv deep annotation script does not use the output vcf parameter but just overrides the input file.
             * Therefore the snv annotation file has to be copied so that it has the correct naming.
             * It was decided not to override the snv annotation file to be on the safe side.
             */
            final Map<String, String> environmentVariables = [
                    CONFIG_FILE: configFileInProjectDirectory.absolutePath,
                    pid: instance.individual.pid,
                    PID: instance.individual.pid,
                    TOOL_ID: "snvDeepAnnotation",
                    PIPENAME: "SNV_DEEPANNOTATION",
                    FILENAME_VCF: deepAnnotationResultFile.absolutePath,
                    FILENAME_VCF_SNVS: deepAnnotationResultFile.absolutePath,
                    FILENAME_CHECKPOINT: checkpointFile.absolutePath,
            ]
            final String script = "cp ${inputResultFile} ${deepAnnotationResultFile}; " +
                    "${step.getExternalScript(config.externalScriptVersion).scriptFilePath}; " +
                    "md5sum ${deepAnnotationResultFile} > ${deepAnnotationResultFile}.md5sum"

            clusterJobSchedulerService.executeJob(realm, script, environmentVariables)

            return NextAction.WAIT_FOR_CLUSTER_JOBS
        } else {
            checkIfResultFilesExistsOrThrowException(instance)
            linkPreviousResults(instance, realm)
            return NextAction.SUCCEED
        }
    }

    @Override
    protected void validate(final SnvCallingInstance instance) throws Throwable {

        // check if the final vcf result file exists
        final OtpPath resultFile = new OtpPath(instance.instancePath, step.getResultFileName(instance.individual))
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(resultFile.absoluteDataManagementPath)

        validateNonCallingJobs(instance, step)
    }
}
