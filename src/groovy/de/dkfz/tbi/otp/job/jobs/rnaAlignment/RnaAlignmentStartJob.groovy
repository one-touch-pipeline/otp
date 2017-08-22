package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component('rnaAlignmentStartJob')
@Scope('singleton')
class RnaAlignmentStartJob extends RoddyAlignmentStartJob {
    @Override
    List<SeqType> getSeqTypes() {
        return SeqType.getRnaAlignableSeqTypes()
    }

    /**
     * For RNA alignment the incremental merging is not implemented yet, therefore null is returned.
     */
    @Override
    RoddyBamFile findUsableBaseBamFile(MergingWorkPackage mergingWorkPackage) {
        return null
    }

    @Override
    protected Class<RnaRoddyBamFile> getInstanceClass() {
        return RnaRoddyBamFile
    }
}
