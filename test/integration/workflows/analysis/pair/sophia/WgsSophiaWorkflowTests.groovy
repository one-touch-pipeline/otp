package workflows.analysis.pair.sophia

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WgsSophiaWorkflowTests extends AbstractSophiaWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div128 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.wholeGenomePairedSeqType
    }
}
