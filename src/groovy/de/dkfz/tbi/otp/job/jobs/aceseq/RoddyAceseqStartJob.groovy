package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component("aceseqStartJob")
@Scope("singleton")
class RoddyAceseqStartJob extends RoddyBamFilePairAnalysisStartJob {

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
