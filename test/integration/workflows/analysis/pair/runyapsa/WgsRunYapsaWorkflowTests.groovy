package workflows.analysis.pair.runyapsa

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import workflows.analysis.pair.bamfiles.*

@Ignore
class WgsRunYapsaWorkflowTests extends AbstractRunYapsaWorkflowTests implements SeqTypeAndInputBigBamFiles {

    @Override
    SeqType seqTypeToUse() {
        return SeqType.wholeGenomePairedSeqType
    }
}
