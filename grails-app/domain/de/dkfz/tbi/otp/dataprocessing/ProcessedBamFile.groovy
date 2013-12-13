package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqTrack

class ProcessedBamFile extends AbstractFileSystemBamFile {

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]

    public SeqTrack getSeqTrack() {
        return alignmentPass.seqTrack
    }

    @Override
    public String toString() {
        return "PBF (${id}) on ${alignmentPass}"
    }
}
