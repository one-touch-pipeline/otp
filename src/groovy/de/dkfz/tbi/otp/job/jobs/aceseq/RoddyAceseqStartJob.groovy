package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.AceseqService
import de.dkfz.tbi.otp.dataprocessing.BamFileAnalysisService
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.tracking.OtrsTicket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("aceseqStartJob")
@Scope("singleton")
class RoddyAceseqStartJob extends AbstractBamFilePairAnalysisStartJob {

    @Autowired
    AceseqService aceseqService

    @Override
    protected void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis) {
        assert bamFilePairAnalysis : "bamFilePairAnalysis must not be null"
        trackingService.setStartedForSeqTracks(bamFilePairAnalysis.getContainedSeqTracks(), OtrsTicket.ProcessingStep.ACESEQ)
        bamFilePairAnalysis.samplePair.aceseqProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        bamFilePairAnalysis.samplePair.save()
    }

    @Override
    BamFileAnalysisService getBamFileAnalysisService() {
        return aceseqService
    }

}
