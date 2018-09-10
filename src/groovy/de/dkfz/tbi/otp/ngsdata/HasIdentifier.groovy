package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*

trait HasIdentifier {

    int identifier

    static int nextIdentifier(MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        Integer maxIdentifier = maxIdentifier(mergingWorkPackage)
        return (maxIdentifier == null) ? 0 : (maxIdentifier + 1)
    }

    static Integer maxIdentifier(MergingWorkPackage workPackage) {
        assert workPackage
        return AbstractMergedBamFile.createCriteria().get {
            eq("workPackage", workPackage)
            projections {
                max("identifier")
            }
        }
    }
}
