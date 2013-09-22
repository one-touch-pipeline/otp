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
    EXOME("EXON"),
    MEDIP,
    SNC_RNA("sncRNA"),
    CHIP_SEQ("ChIP Seq"),
    WHOLE_GENOME_BISULFITE_TAGMENTATION

    /**
     * the name of the seq type
     */
    final String seqTypeName

    private SeqTypeNames() {
        this.seqTypeName = name()
    }

    private SeqTypeNames(String seqTypeName) {
        this.seqTypeName = seqTypeName
    }
}
