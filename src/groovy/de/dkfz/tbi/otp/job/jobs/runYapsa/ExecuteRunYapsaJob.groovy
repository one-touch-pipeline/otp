package de.dkfz.tbi.otp.job.jobs.runYapsa

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction.*

@Component
@Scope("prototype")
@UseJobLog
class ExecuteRunYapsaJob extends AbstractOtpJob implements AutoRestartableJob {

    @Autowired BedFileService bedFileService
    @Autowired ClusterJobSchedulerService clusterJobSchedulerService
    @Autowired ReferenceGenomeService referenceGenomeService
    @Autowired ConfigService configService
    @Autowired ProcessingOptionService processingOptionService

    @Override
    protected final AbstractMultiJob.NextAction maybeSubmit() throws Throwable {
        final RunYapsaInstance instance = getProcessParameterObject()
        final Realm realm = instance.project.realm

        String jobScript = createScript(instance)

        clusterJobSchedulerService.executeJob(realm, jobScript)

        return WAIT_FOR_CLUSTER_JOBS
    }


    private createScript(RunYapsaInstance instance) {
        final RunYapsaConfig config = instance.config
        File outputDirectory = instance.getWorkDirectory()
        ReferenceGenome referenceGenome = instance.getReferenceGenome()

        String moduleLoader = processingOptionService.findOptionAsString(COMMAND_LOAD_MODULE_LOADER)
        String rActivation = processingOptionService.findOptionAsString(COMMAND_ACTIVATION_R)
        String rCommand = processingOptionService.findOptionAsString(COMMAND_R)
        String runYapsaActivationPrefix = processingOptionService.findOptionAsString(COMMAND_ACTIVATION_RUN_YAPSA_PREFIX)
        String runYapsaCommand = processingOptionService.findOptionAsString(COMMAND_RUN_YAPSA)

        List<String> runYapsaCall = []
        runYapsaCall << rCommand
        runYapsaCall << configService.getToolsPath().getAbsolutePath() + "/" + runYapsaCommand
        runYapsaCall << "-i ${instance.samplePair.findLatestSnvCallingInstance().getResultRequiredForRunYapsa()}"
        runYapsaCall << "-o ${outputDirectory.absolutePath}"
        if (instance.seqType == SeqType.wholeGenomePairedSeqType) {
            runYapsaCall << "-s WGS"
        } else if (instance.seqType == SeqType.exomePairedSeqType) {
            runYapsaCall << "-s WES"
            BedFile bedFile = BedFile.findByReferenceGenomeAndLibraryPreparationKit(
                    referenceGenome,
                    instance.getLibraryPreparationKit(),
            )
            runYapsaCall << "-t ${bedFileService.filePath(bedFile)}"
        } else {
            throw new UnsupportedOperationException("Sequencing type '${instance.seqType}' not supported by runYapsa")
        }
        runYapsaCall << "-r ${referenceGenomeService.fastaFilePath(referenceGenome)}"
        runYapsaCall << "-v"

        return """\
            ${moduleLoader}
            ${rActivation}
            ${runYapsaActivationPrefix} ${config.programVersion}

            mkdir -p -m 2755 ${outputDirectory.absolutePath}

            ${runYapsaCall.join(" ")}

            """.stripIndent()
    }


    @Override
    protected final void validate() throws Throwable {
        final RunYapsaInstance instance = getProcessParameterObject()
        instance.updateProcessingState(AnalysisProcessingStates.FINISHED)
    }
}
