package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class WgbsRoddyAlignmentChecker extends AbstractRoddyAlignmentChecker {

    String getWorkflowName() {
        return 'WgbsAlignmentWorkflow'
    }

    Pipeline.Name getPipeLineName() {
        return Pipeline.Name.PANCAN_ALIGNMENT
    }

    List<SeqType> getSeqTypes() {
        return [SeqType.wholeGenomeBisulfitePairedSeqType, SeqType.wholeGenomeBisulfiteTagmentationPairedSeqType]
    }
}
