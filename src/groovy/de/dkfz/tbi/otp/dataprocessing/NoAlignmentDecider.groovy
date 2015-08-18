package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SeqTrack

/**
 * An {@link AlignmentDecider} which decides not to align.
 */
@Component
@Scope("singleton")
class NoAlignmentDecider implements AlignmentDecider {

    @Override
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign) {
        threadLog?.info("Not aligning ${seqTrack}, because it is configured to use the ${this.getClass().simpleName}.")
        return Collections.emptyList()
    }

    @Override
    String alignmentMessage() {
        return "shall not be aligned"
    }
}
