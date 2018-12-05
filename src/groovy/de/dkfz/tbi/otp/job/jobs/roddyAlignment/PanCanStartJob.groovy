package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component('PanCanStartJob')
@Scope('singleton')
class PanCanStartJob extends AbstractRoddyAlignmentStartJob {

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqTypeService.getExomePairedSeqType(), SeqTypeService.getWholeGenomePairedSeqType(), SeqTypeService.getChipSeqPairedSeqType()]
    }
}
