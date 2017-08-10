package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

/**
 * An {@link AlignmentDecider} which decides not to align.
 */
@Component
@Scope("singleton")
class NoAlignmentDecider implements AlignmentDecider {

    @Override
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign) {
        seqTrack.log("Not aligning{0}, because it is configured to use the ${this.getClass().simpleName}.")
        return Collections.emptyList()
    }
}
