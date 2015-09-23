package de.dkfz.tbi.otp.ngsdata

/**
 * Holds possible seqType names for using in the code
 *
 *
 */
public enum SeqTypeNames {
    WHOLE_GENOME,
    WHOLE_GENOME_BISULFITE,
    RNA,
    MI_RNA,
    EXOME("EXON", ExomeSeqTrack),
    MEDIP,
    SNC_RNA("sncRNA"),
    CHIP_SEQ("ChIP Seq", ChipSeqSeqTrack),
    WHOLE_GENOME_BISULFITE_TAGMENTATION

    /**
     * the name of the seq type
     */
    final String seqTypeName

    final Class<? extends SeqTrack> seqTrackClass

    private SeqTypeNames(String seqTypeName = null, Class<? extends SeqTrack> seqTrackClass = SeqTrack) {
        this.seqTypeName = seqTypeName ?: name()
        this.seqTrackClass = seqTrackClass
    }

    public static SeqTypeNames fromSeqTypeName(String seqTypeName) {
        return values().find { it.seqTypeName == seqTypeName }
    }
}
