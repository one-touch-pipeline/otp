package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class MergingWorkPackage {

    enum ProcessingType {
        MANUAL, //files to be merged are defined by the user
        SYSTEM // files to be merged are defined by the system according to the mergingCriteria
    }

    /**
     * a criteria to collect {@link ProcessedBamFile}s
     * to be merged
     */
    enum MergingCriteria {
        DEFAULT // files with the same platform and seq type
        // e.g. ILLUMINA_2000_2500: illumina 2000 and 2500 and the same seq type
    }

    ProcessingType processingType = ProcessingType.SYSTEM
    MergingCriteria mergingCriteria = MergingCriteria.DEFAULT

    static belongsTo = [sample: Sample]

    static constraints = {
        mergingCriteria (nullable: true)
    }
}
