package de.dkfz.tbi.otp.dataprocessing.singleCell

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.utils.CollectionUtils

class SingleCellMergingWorkPackage extends MergingWorkPackage {

    int expectedCells
    Integer enforcedCells
    SingleCellConfig config

    static constraints = {
        sample(validator: { val, obj ->
            SingleCellMergingWorkPackage singleCellMergingWorkPackage = CollectionUtils.atMostOneElement(
                    SingleCellMergingWorkPackage.findAllBySampleAndSeqTypeAndExpectedCellsAndEnforcedCells(val, obj.seqType, obj.expectedCells, obj.enforcedCells),
                    "More than one MWP exists for sample ${val}, seqType ${obj.seqType}, expectedCells ${obj.expectedCells} and enforcedCells ${obj.enforcedCells}")
            if (singleCellMergingWorkPackage && singleCellMergingWorkPackage.id != obj.id) {
                return "The singleCellMergingWorkPackage must be unique for one sample and seqType, expectedCells and enforcedCells"
            }
        })
        enforcedCells(nullable: true)
        config(nullable: true)
    }

    @Override
    String toString() {
        return "SCMWP ${id}: ${toStringWithoutIdAndPipeline()} ${pipeline?.name}"
    }
}
