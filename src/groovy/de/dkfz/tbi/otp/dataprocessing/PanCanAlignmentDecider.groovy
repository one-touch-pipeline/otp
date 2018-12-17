package de.dkfz.tbi.otp.dataprocessing

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SeqTrack

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
