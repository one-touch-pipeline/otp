package de.dkfz.tbi.otp.job.jobs.snvcalling

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.BamFileAnalysisService
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.RoddyBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.tracking.OtrsTicket

@Component("roddySnvStartJob")
@Scope("singleton")
class RoddySnvCallingStartJob extends AbstractBamFilePairAnalysisStartJob implements RoddyBamFilePairAnalysisStartJob {

    @Autowired
    SnvCallingService snvCallingService

    @Override
    void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        assert bamFilePairAnalysis : "bamFilePairAnalysis must not be null"
        trackingService.setStartedForSeqTracks(bamFilePairAnalysis.getContainedSeqTracks(), OtrsTicket.ProcessingStep.SNV)
        bamFilePairAnalysis.samplePair.snvProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        bamFilePairAnalysis.samplePair.save()
    }

    @Override
    BamFileAnalysisService getBamFileAnalysisService() {
        return snvCallingService
    }
}
