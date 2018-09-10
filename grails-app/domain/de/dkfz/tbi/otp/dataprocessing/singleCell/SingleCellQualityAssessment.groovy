package de.dkfz.tbi.otp.dataprocessing.singleCell

import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.QualityAssessmentMergedPass
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated

class SingleCellQualityAssessment extends AbstractQualityAssessment {

    QualityAssessmentMergedPass qualityAssessmentMergedPass

    /**
     * The number of barcodes associated with cell-containing partitions,
     * estimated from the barcode UMI count distribution.
     */
    @QcThresholdEvaluated
    Double estimatedNumberOfCells

    /**
     * The total number of sequenced reads divided by the estimated number of cells.
     */
    @QcThresholdEvaluated
    Double meanReadsPerCell

    /**
     * The median number of genes detected (with nonzero UMI counts) across all cell-associated barcodes.
     */
    @QcThresholdEvaluated
    Double medianGenesPerCell

    /**
     * Total number of sequenced reads.
     */
    @QcThresholdEvaluated
    Double numberOfReads

    /**
     * Fraction of reads with cell-barcodes that match the whitelist.
     */
    @QcThresholdEvaluated
    Double validBarcodes

    /**
     * Fraction of reads that mapped to a unique gene in the transcriptome
     * with a high mapping quality score as reported by the aligner.
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToTranscriptome

    /**
     * Fraction of reads that mapped to the exonic regions of the genome
     * with a high mapping quality score as reported by the aligner.
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToExonicRegions

    /**
     * Fraction of reads that mapped to the intronic regions of the genome
     * with a high mapping quality score as reported by the aligner.
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToIntronicRegions

    /**
     * Fraction of reads that mapped to the intergenic regions of the genome
     * with a high mapping quality score as reported by the aligner.
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToIntergenicRegions

    /**
     * The fraction of reads originating from an already-observed UMI.
     */
    @QcThresholdEvaluated
    Double sequencingSaturation

    /**
     * Fraction of bases with Q-score at least 30 in the cell barcode sequences.
     */
    @QcThresholdEvaluated
    Double q30BasesInBarcode

    /**
     * Fraction of bases with Q-score at least 30 in the RNA read sequences.
     */
    @QcThresholdEvaluated
    Double q30BasesInRnaRead

    /**
     * Fraction of bases with Q-score at least 30 in the sample index sequences.
     */
    @QcThresholdEvaluated
    Double q30BasesInSampleIndex

    /**
     * 	Fraction of bases with Q-score at least 30 in the UMI sequences.
     */
    @QcThresholdEvaluated
    Double q30BasesInUmi

    /**
     * The fraction of cell-barcoded, confidently mapped reads with cell-associated barcodes.
     */
    @QcThresholdEvaluated
    Double fractionReadsInCells

    /**
     * The number of genes with at least one UMI count in any cell.
     */
    @QcThresholdEvaluated
    Double totalGenesDetected

    /**
     * The median number of total UMI counts across all cell-associated barcodes.
     */
    @QcThresholdEvaluated
    Double medianUmiCountsPerCell

    static constraints = { }

}
