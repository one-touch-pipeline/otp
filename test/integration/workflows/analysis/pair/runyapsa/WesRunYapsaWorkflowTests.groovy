package workflows.analysis.pair.runyapsa

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WesRunYapsaWorkflowTests extends AbstractRunYapsaWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div128 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.exomePairedSeqType
    }

}
