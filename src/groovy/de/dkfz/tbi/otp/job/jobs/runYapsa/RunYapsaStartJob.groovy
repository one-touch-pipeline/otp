package de.dkfz.tbi.otp.job.jobs.runYapsa

import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.CollectionUtils

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
        return "runYapsa_${ config.programVersion.replace("/", "-") }_${ getFormattedDate() }"
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