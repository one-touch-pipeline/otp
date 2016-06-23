package de.dkfz.tbi.otp.dataprocessing

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("singleton")
class PanCanAlignmentDecider extends RoddyAlignmentDecider {

    @Override
    Pipeline.Name pipelineName() {
        return Pipeline.Name.PANCAN_ALIGNMENT
    }

    @Override
    String alignmentMessage() {
        return "will be aligned with the PanCan pipeline"
    }
}
