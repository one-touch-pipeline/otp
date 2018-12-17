package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

@Component('PanCanStartJob')
@Scope('singleton')
class PanCanStartJob extends AbstractRoddyAlignmentStartJob {

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqTypeService.getExomePairedSeqType(), SeqTypeService.getWholeGenomePairedSeqType(), SeqTypeService.getChipSeqPairedSeqType()]
    }
}
