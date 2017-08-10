package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("singleton")
class PanCanAlignmentDecider extends RoddyAlignmentDecider {

    @Override
    Pipeline.Name pipelineName(SeqTrack seqTrack) {
        if (seqTrack.seqType.isRna()) {
            return Pipeline.Name.RODDY_RNA_ALIGNMENT
        } else {
            return Pipeline.Name.PANCAN_ALIGNMENT
        }
    }
}
