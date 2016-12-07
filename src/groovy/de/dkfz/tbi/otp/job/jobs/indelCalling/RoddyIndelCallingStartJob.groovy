package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component("roddyIndelStartJob")
@Scope("singleton")
class RoddyIndelCallingStartJob extends AbstractBamFilePairAnalysisStartJob {

    @Autowired
    IndelCallingService indelCallingService

    @Override
    String getJobExecutionPlanName() {
        return "IndelWorkflow"
    }

    @Override
    protected ConfigPerProject getConfig(SamplePair samplePair) {
        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByName(Pipeline.Name.RODDY_INDEL))
        RoddyWorkflowConfig config = (RoddyWorkflowConfig)RoddyWorkflowConfig.getLatestForIndividual(
                samplePair.individual, samplePair.seqType, pipeline)

        if (config == null) {
            throw new RuntimeException("No ${RoddyWorkflowConfig.simpleName} found for ${Pipeline.simpleName} ${pipeline}, ${Individual.simpleName} ${samplePair.individual} (${Project.simpleName} ${samplePair.project}), ${SeqType.simpleName} ${samplePair.seqType}")
        }
        return config
    }

    @Override
    protected Class<? extends ConfigPerProject> getConfigClass() {
        return RoddyWorkflowConfig
    }

    @Override
    protected Class<? extends IndelCallingInstance> getInstanceClass() {
        return IndelCallingInstance
    }

    @Override
    protected SamplePair findSamplePairToProcess(short minPriority) {
        return indelCallingService.samplePairForProcessing(minPriority, getConfigClass())
    }

    @Override
    protected void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        assert bamFilePairAnalysis : "bamFilePairAnalysis must not be null"
        trackingService.setStartedForSeqTracks(bamFilePairAnalysis.getContainedSeqTracks(), OtrsTicket.ProcessingStep.INDEL)
        bamFilePairAnalysis.samplePair.indelProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        bamFilePairAnalysis.samplePair.save()
    }
}
