package workflows.analysis.pair.aceseq

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WgsAceseqWorkflowTests extends AbstractAceseqWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div8 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.wholeGenomePairedSeqType
    }
}
