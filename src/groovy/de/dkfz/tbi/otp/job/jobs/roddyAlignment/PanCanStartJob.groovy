package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component('PanCanStartJob')
@Scope('singleton')
class PanCanStartJob extends RoddyAlignmentStartJob {

    @Override
    String getJobExecutionPlanName() {
        return 'PanCanWorkflow'
    }

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqType.getExomePairedSeqType(), SeqType.getWholeGenomePairedSeqType()]
    }
}
