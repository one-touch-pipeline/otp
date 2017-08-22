package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class RnaRoddyAlignmentChecker extends AbstractRoddyAlignmentChecker {

    String getWorkflowName() {
        return 'RnaAlignmentWorkflow'
    }

    Pipeline.Name getPipeLineName() {
        return Pipeline.Name.RODDY_RNA_ALIGNMENT
    }

    List<SeqType> getSeqTypes() {
        return SeqType.rnaAlignableSeqTypes
    }

}
