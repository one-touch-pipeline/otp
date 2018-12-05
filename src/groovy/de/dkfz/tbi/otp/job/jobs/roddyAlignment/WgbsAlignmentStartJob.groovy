package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component('WgbsAlignmentStartJob')
@Scope('singleton')
class WgbsAlignmentStartJob extends AbstractRoddyAlignmentStartJob {

    @Override
    List<SeqType> getSeqTypes() {
        return SeqType.findAllByNameInListAndLibraryLayout(SeqType.WGBS_SEQ_TYPE_NAMES*.seqTypeName, LibraryLayout.PAIRED)
    }

    /**
     * For WGBS alignment the incremental merging is not implemented yet, therefore null is returned.
     */
    @Override
    RoddyBamFile findUsableBaseBamFile(MergingWorkPackage mergingWorkPackage) {
        return null
    }
}
