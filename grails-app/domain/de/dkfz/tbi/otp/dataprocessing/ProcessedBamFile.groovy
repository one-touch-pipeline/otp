package de.dkfz.tbi.otp.dataprocessing

class ProcessedBamFile extends AbstractFileSystemBamFile {

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]

    @Override
    public String toString() {
        return "PBF (${id}) on ${alignmentPass}"
    }
}
