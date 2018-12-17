package de.dkfz.tbi.otp.job.jobs.indelCalling

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.RoddyBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.tracking.OtrsTicket

@Component("roddyIndelStartJob")
@Scope("singleton")
class RoddyIndelCallingStartJob extends AbstractBamFilePairAnalysisStartJob implements RoddyBamFilePairAnalysisStartJob {

    @Autowired
    IndelCallingService indelCallingService

    @Override
    void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        assert bamFilePairAnalysis : "bamFilePairAnalysis must not be null"
        trackingService.setStartedForSeqTracks(bamFilePairAnalysis.getContainedSeqTracks(), OtrsTicket.ProcessingStep.INDEL)
        bamFilePairAnalysis.samplePair.indelProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        bamFilePairAnalysis.samplePair.save()
    }

    @Override
    BamFileAnalysisService getBamFileAnalysisService() {
        return indelCallingService
    }
}
