package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * An {@link AlignmentDecider} which decides to do the default OTP alignment if the conditions for that are satisfied.
 */
@Component
@Scope("singleton")
class DefaultOtpAlignmentDecider extends AbstractAlignmentDecider {

    @Override
    Pipeline.Name pipelineName(SeqTrack seqTrack) {
        return Pipeline.Name.DEFAULT_OTP
    }

    @Override
    void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign) {
        if (forceRealign || !AlignmentPass.findWhere(workPackage: workPackage, seqTrack: seqTrack)) {
            assert new AlignmentPass(
                    workPackage: workPackage,
                    seqTrack: seqTrack,
                    identifier: AlignmentPass.nextIdentifier(seqTrack),
                    alignmentState: AlignmentState.NOT_STARTED,
            ).save(failOnError: true)
            seqTrack.log("Will align{0} for ${workPackage}.")
        } else {
            seqTrack.log("Not realigning{0} for ${workPackage}, because forceRealign is false.")
        }
    }

    @Override
    String alignmentMessage() {
        return "will be aligned with bwa aln"
    }
}
