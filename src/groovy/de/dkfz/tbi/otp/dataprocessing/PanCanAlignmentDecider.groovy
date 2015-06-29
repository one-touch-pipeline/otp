package de.dkfz.tbi.otp.dataprocessing

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("singleton")
class PanCanAlignmentDecider extends RoddyAlignmentDecider {

    @Override
    Workflow.Name getWorkflowName() {
        return Workflow.Name.PANCAN_ALIGNMENT
    }
}
