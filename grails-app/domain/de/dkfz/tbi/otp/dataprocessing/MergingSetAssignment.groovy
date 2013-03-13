package de.dkfz.tbi.otp.dataprocessing

class MergingSetAssignment {
    static belongsTo = [
        mergingSet: MergingSet,
        bamFile: ProcessedBamFile
    ]
}