package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import org.hibernate.cache.ehcache.internal.strategy.ReadWriteEhcacheNaturalIdRegionAccessStrategy
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component('RnaAlignmentStartJob')
@Scope('singleton')
class RnaAlignmentStartJob extends RoddyAlignmentStartJob {
    @Override
    List<SeqType> getSeqTypes() {
        return [SeqType.getRnaPairedSeqType()]
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
