package workflows.analysis.pair.snv

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WesSnvWorkflowTests extends AbstractSnvWorkflowTests implements SeqTypeAndInputBigBamFiles {

    @Override
    SeqType seqTypeToUse() {
        return SeqType.exomePairedSeqType
    }
}
