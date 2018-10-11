package workflows

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*

@Ignore
class PanCanExomeAlignmentWorkflowTests extends PanCanAlignmentWorkflowTests {
    @Override
    SeqType findSeqType() {
        return CollectionUtils.exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.EXOME.seqTypeName,
                libraryLayout: LibraryLayout.PAIRED,
        ))
    }
}
