package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*

abstract class AbstractSnvCallingStartJob extends AbstractBamFilePairAnalysisStartJob {

    @Autowired
    SnvCallingService snvCallingService

    @Override
    SamplePair findSamplePairToProcess(short minPriority) {
        return snvCallingService.samplePairForProcessing(minPriority, getConfigClass())
    }

    @Override
    void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        trackingService.setStartedForSeqTracks(bamFilePairAnalysis.getContainedSeqTracks(), OtrsTicket.ProcessingStep.SNV)
        bamFilePairAnalysis.samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        bamFilePairAnalysis.samplePair.save()
    }

    @Override
    protected void withdrawSnvJobResultsIfAvailable(BamFilePairAnalysis bamFilePairAnalysis) {
        SnvJobResult.withTransaction {
            SnvJobResult.findAllBySnvCallingInstance(bamFilePairAnalysis).each {
                it.withdraw()
            }
        }
    }
}
