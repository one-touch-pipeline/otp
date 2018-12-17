package workflows.alignment

import org.junit.Ignore
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

@Ignore
class PanCanWholeGenomeAlignmentWorkflowTests extends PanCanAlignmentWorkflowTests {
    @Override
    SeqType findSeqType() {
        return CollectionUtils.exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                libraryLayout: LibraryLayout.PAIRED,
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
