package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.AceseqService
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("aceseqStartJob")
@Scope("singleton")
class RoddyAceseqStartJob extends AbstractBamFilePairAnalysisStartJob {

    @Autowired
    AceseqService aceseqService

    @Override
    protected ConfigPerProject getConfig(SamplePair samplePair) {
        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByName(Pipeline.Name.RODDY_ACESEQ))
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
    protected Class<? extends AceseqInstance> getInstanceClass() {
        return AceseqInstance
    }

    @Override
    protected SamplePair findSamplePairToProcess(short minPriority) {
        return aceseqService.samplePairForProcessing(minPriority, getConfigClass(), [SeqType.wholeGenomePairedSeqType])
    }

    @Override
    protected void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        assert bamFilePairAnalysis : "bamFilePairAnalysis must not be null"
        trackingService.setStartedForSeqTracks(bamFilePairAnalysis.getContainedSeqTracks(), OtrsTicket.ProcessingStep.ACESEQ)
        bamFilePairAnalysis.samplePair.aceseqProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        bamFilePairAnalysis.samplePair.save()
    }

}
