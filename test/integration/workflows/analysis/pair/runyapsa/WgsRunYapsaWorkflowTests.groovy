package workflows.analysis.pair.runyapsa

import org.junit.Ignore
import workflows.analysis.pair.bamfiles.SeqTypeAndInputBamFilesHCC1187Div128

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

@Ignore
class WgsRunYapsaWorkflowTests extends AbstractRunYapsaWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div128 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.wholeGenomePairedSeqType
    }
}
