package workflows.analysis.pair.snv

import org.junit.Ignore
import workflows.analysis.pair.bamfiles.SeqTypeAndInputBamFilesHCC1187Div32

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

@Ignore
class WesSnvWorkflowTests extends AbstractSnvWorkflowTests implements SeqTypeAndInputBamFilesHCC1187Div32 {

    @Override
    SeqType seqTypeToUse() {
        return SeqTypeService.exomePairedSeqType
    }
}
