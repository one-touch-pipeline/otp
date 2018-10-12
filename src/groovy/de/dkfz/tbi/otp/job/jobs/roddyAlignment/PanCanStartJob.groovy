package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component('PanCanStartJob')
@Scope('singleton')
class PanCanStartJob extends RoddyAlignmentStartJob {

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqTypeService.getExomePairedSeqType(), SeqTypeService.getWholeGenomePairedSeqType(), SeqTypeService.getChipSeqPairedSeqType()]
    }
}
