package de.dkfz.tbi.otp.dataprocessing.cellRanger

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.utils.CollectionUtils

class CellRangerMergingWorkPackage extends MergingWorkPackage {

    int expectedCells
    Integer enforcedCells
    CellRangerConfig config

    static constraints = {
        sample(validator: { val, obj ->
            CellRangerMergingWorkPackage cellRangerMergingWorkPackage = CollectionUtils.atMostOneElement(
                    CellRangerMergingWorkPackage.findAllBySampleAndSeqTypeAndExpectedCellsAndEnforcedCells(
                            val, obj.seqType, obj.expectedCells, obj.enforcedCells),
                    "More than one MWP exists for sample ${val}, " +
                            "seqType ${obj.seqType}, " +
                            "expectedCells ${obj.expectedCells} and enforcedCells ${obj.enforcedCells}")
            if (cellRangerMergingWorkPackage && cellRangerMergingWorkPackage.id != obj.id) {
                return "The CellRangerMergingWorkPackage must be unique for one sample and seqType, expectedCells and enforcedCells"
            }
        })
        enforcedCells(nullable: true)
        config(nullable: true)
    }

    @Override
    String toString() {
        return "CRMWP ${id}: ${toStringWithoutIdAndPipeline()} ${pipeline?.name}"
    }
}
