package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

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
        return [SeqTypeService.wholeGenomeBisulfitePairedSeqType, SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType]
    }
}
