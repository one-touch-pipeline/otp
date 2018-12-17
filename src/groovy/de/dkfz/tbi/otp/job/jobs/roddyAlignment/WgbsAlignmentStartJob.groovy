package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.LibraryLayout
import de.dkfz.tbi.otp.ngsdata.SeqType

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
