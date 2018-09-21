package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class WgbsRoddyAlignmentChecker extends AbstractRoddyAlignmentChecker {

    @Override
    String getWorkflowName() {
        return 'WgbsAlignmentWorkflow'
    }

    @Override
    Pipeline.Name getPipeLineName() {
        return Pipeline.Name.PANCAN_ALIGNMENT
    }

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqType.wholeGenomeBisulfitePairedSeqType, SeqType.wholeGenomeBisulfiteTagmentationPairedSeqType]
    }
}
