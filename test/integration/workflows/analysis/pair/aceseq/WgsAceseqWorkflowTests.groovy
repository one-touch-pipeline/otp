package workflows.analysis.pair.aceseq

import org.junit.Ignore
import workflows.analysis.pair.bamfiles.SeqTypeAndInputBamFilesHCC1187Div8

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

@Ignore
class WgsAceseqWorkflowTests extends AbstractAceseqWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div8 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.wholeGenomePairedSeqType
    }
}
