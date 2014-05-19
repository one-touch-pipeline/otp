package de.dkfz.tbi.otp.dataprocessing


/**
 * represents the MarkDuplicatesMetrics file of a BamFile and
 * stores all the information, which were generated while removing duplicates by Picard
 *
 */
class PicardMarkDuplicatesMetrics {

    String metricsClass
    String library
    long unpaired_reads_examined
    long read_pairs_examined
    long unmapped_reads
    long unpaired_read_duplicates
    long read_pair_duplicates
    long read_pair_optical_duplicates
    /**
     * this is only the label, which is used in the MetricsFile itself
     * the value represents the fraction of duplications
     */
    double percent_duplication
    long estimated_library_size

    static belongsTo = [
        abstractBamFile: AbstractBamFile
    ]

    static constraints = {
        metricsClass(blank: false)
        library(blank: false)
    }

    static mapping = {
        abstractBamFile index: "picard_mark_duplicates_metrics_abstract_bam_file_idx"
    }
}
