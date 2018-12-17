package workflows.alignment

import org.junit.Ignore

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

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
