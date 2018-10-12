package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class RnaRoddyAlignmentChecker extends AbstractRoddyAlignmentChecker {

    @Override
    String getWorkflowName() {
        return 'RnaAlignmentWorkflow'
    }

    @Override
    Pipeline.Name getPipeLineName() {
        return Pipeline.Name.RODDY_RNA_ALIGNMENT
    }

    @Override
    List<SeqType> getSeqTypes() {
        return SeqTypeService.rnaAlignableSeqTypes
    }

}
