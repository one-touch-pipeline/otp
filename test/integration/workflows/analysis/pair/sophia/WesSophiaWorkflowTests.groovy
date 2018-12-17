package workflows.analysis.pair.sophia

import org.junit.Ignore
import workflows.analysis.pair.bamfiles.SeqTypeAndInputBamFilesHCC1187Div128

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

@Ignore
class WesSophiaWorkflowTests extends AbstractSophiaWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div128 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.exomePairedSeqType
    }
}
