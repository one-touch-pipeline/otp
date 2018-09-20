package workflows.analysis.pair.aceseq

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WgsAceseqWorkflowTests extends AbstractAceseqWorkflowTests implements SeqTypeAndInputBigBamFiles {

    @Override
    SeqType seqTypeToUse() {
        return SeqType.wholeGenomePairedSeqType
    }
}
