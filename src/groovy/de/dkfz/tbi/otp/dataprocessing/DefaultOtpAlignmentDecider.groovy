package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog

/**
 * An {@link AlignmentDecider} which decides to do the default OTP alignment if the conditions for that are satisfied.
 */
@Component
@Scope("singleton")
class DefaultOtpAlignmentDecider extends AbstractAlignmentDecider {

    @Override
    Workflow.Name getWorkflowName() {
        return Workflow.Name.DEFAULT_OTP
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
            threadLog?.info("Will align ${seqTrack} for ${workPackage}.")
        } else {
            threadLog?.info("Not realigning ${seqTrack} for ${workPackage}, because forceRealign is false.")
        }
    }

}
