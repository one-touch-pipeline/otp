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

    /**
     * @return <code>true</code>, if this {@link ProcessedBamFile} is from the latest alignment
     * @see AlignmentPass#isLatestPass()
     */
    public boolean isMostRecentBamFile() {
        return alignmentPass.isLatestPass()
    }
}
