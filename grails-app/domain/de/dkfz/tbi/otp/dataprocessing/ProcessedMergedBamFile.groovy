package de.dkfz.tbi.otp.dataprocessing

class ProcessedMergedBamFile extends AbstractFileSystemBamFile {

    static belongsTo = [
        mergingPass: MergingPass
    ]
}
