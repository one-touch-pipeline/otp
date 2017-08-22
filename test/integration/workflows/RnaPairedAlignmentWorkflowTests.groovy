package workflows

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

@Ignore
class RnaPairedAlignmentWorkflowTests extends AbstractRnaAlignmentWorkflowTests {

    @Override
    SeqType findSeqType() {
        DomainFactory.createRnaPairedSeqType()
    }
}
