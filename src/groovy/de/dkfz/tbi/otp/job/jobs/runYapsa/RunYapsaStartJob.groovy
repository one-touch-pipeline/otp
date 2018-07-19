package de.dkfz.tbi.otp.job.jobs.runYapsa

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import org.hibernate.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component("runYapsaStartJob")
@Scope("singleton")
class RunYapsaStartJob extends AbstractBamFilePairAnalysisStartJob {
    @Autowired
    RunYapsaService runYapsaService

    @Override
    void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        assert bamFilePairAnalysis : "bamFilePairAnalysis must not be null"

        trackingService.setStartedForSeqTracks(bamFilePairAnalysis.getContainedSeqTracks(), OtrsTicket.ProcessingStep.RUN_YAPSA)
        bamFilePairAnalysis.samplePair.runYapsaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        bamFilePairAnalysis.samplePair.save()
    }

    @Override
    BamFileAnalysisService getBamFileAnalysisService() {
        return runYapsaService
    }

    @Override
    String getInstanceName(ConfigPerProjectAndSeqType config) {
        assert RunYapsaConfig.isAssignableFrom(Hibernate.getClass(config)): "RunYapsa startjob should only ever be started with a YAPSA config, not something else; got ${ config.class }"
        return "runYapsa_${ config.programVersion }_${ getFormattedDate() }"
    }

    @Override
    ConfigPerProjectAndSeqType getConfig(SamplePair samplePair) {
        Pipeline pipeline = getBamFileAnalysisService().getPipeline()
        RunYapsaConfig config = CollectionUtils.<RunYapsaConfig> atMostOneElement(RunYapsaConfig.findAllByProjectAndPipelineAndSeqTypeAndObsoleteDate(samplePair.project, pipeline, samplePair.seqType, null))

        if (config == null) {
            throw new RuntimeException("No ${RunYapsaConfig.simpleName} found for ${Pipeline.simpleName} ${pipeline}, ${Individual.simpleName} ${samplePair.individual} (${Project.simpleName} ${samplePair.project}), ${SeqType.simpleName} ${samplePair.seqType}")
        }
        return config
    }
}