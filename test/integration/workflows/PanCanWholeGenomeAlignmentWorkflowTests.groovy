package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
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

        RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForProject(seqTrack.project, seqTrack.seqType, Pipeline.findByName(Pipeline.Name.PANCAN_ALIGNMENT))
        config.adapterTrimmingNeeded = true
        assert config.save(flush: true)

        executeAndVerify_AlignLanesOnly_AllFine()
    }
}
