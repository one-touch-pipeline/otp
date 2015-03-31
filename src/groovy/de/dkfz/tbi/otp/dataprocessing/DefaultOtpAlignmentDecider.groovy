package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

/**
 * An {@link AlignmentDecider} which decides to do the default OTP alignment if the conditions for that are satisfied.
 */
@Component
@Scope("singleton")
class DefaultOtpAlignmentDecider extends AbstractAlignmentDecider {

    @Override
    Workflow getWorkflow() {
        Workflow workflow = atMostOneElement(Workflow.findAllByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT))
        if(!workflow) {
            workflow = new Workflow(
                    name: Workflow.Name.DEFAULT_OTP,
                    type: Workflow.Type.ALIGNMENT
            ).save(failOnError: true)
        }
        return workflow
    }

    @Override
    boolean canWorkflowAlign(SeqTrack seqTrack) {
        return SeqTypeService.alignableSeqTypes()*.id.contains(seqTrack.seqType.id)
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
