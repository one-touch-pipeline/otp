package workflows

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*

@Ignore
class PanCanWholeGenomeAlignmentWorkflowTests extends PanCanAlignmentWorkflowTests {
    @Override
    SeqType findSeqType() {
        return CollectionUtils.exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        ))
    }


    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_WithAdapterTrimming_allFine() {

        // prepare
        SeqTrack seqTrack = createSeqTrack("readGroup1")

        setUpAdapterFile([seqTrack])

        executeAndVerify_AlignLanesOnly_AllFine()
    }
}
