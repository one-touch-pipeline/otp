package de.dkfz.tbi.otp.ngsdata

/**
 * Holds possible seqType names for using in the code
 */
public enum SeqTypeNames {
    WHOLE_GENOME,
    WHOLE_GENOME_BISULFITE,
    RNA,
    MI_RNA,
    EXOME("EXON", ExomeSeqTrack, { Map properties -> new ExomeSeqTrack(properties) }),
    MEDIP,
    SNC_RNA("sncRNA"),
    CHIP_SEQ("ChIP Seq", ChipSeqSeqTrack, { Map properties -> new ChipSeqSeqTrack(properties) }),
    WHOLE_GENOME_BISULFITE_TAGMENTATION

    /**
     * the name of the seq type
     */
    final String seqTypeName

    final Class<? extends SeqTrack> seqTrackClass

    final Closure<? extends SeqTrack> factory

    private SeqTypeNames(String seqTypeName = null,
                         Class<? extends SeqTrack> seqTrackClass = SeqTrack,
                         Closure<? extends SeqTrack> factory = SeqTrack.FACTORY) {
        assert factory([:]).getClass() == seqTrackClass
        this.seqTypeName = seqTypeName ?: name()
        this.seqTrackClass = seqTrackClass
        this.factory = factory
    }

    public boolean isWgbs() {
        return SeqType.WGBS_SEQ_TYPE_NAMES.contains(this)
    }

    public static SeqTypeNames fromSeqTypeName(String seqTypeName) {
        return values().find { it.seqTypeName == seqTypeName }
    }
}
