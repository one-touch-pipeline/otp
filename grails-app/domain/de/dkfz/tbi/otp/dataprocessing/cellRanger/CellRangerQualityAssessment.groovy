/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.dataprocessing.AbstractQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.QualityAssessmentWithMergedPass
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue

@ManagedEntity
class CellRangerQualityAssessment extends AbstractQualityAssessment implements QualityAssessmentWithMergedPass, QcTrafficLightValue {

    Integer getExpectedCells() {
        return ((CellRangerMergingWorkPackage) qualityAssessmentMergedPass.mergingWorkPackage).expectedCells
    }

    Integer getEnforcedCells() {
        return ((CellRangerMergingWorkPackage) qualityAssessmentMergedPass.mergingWorkPackage).enforcedCells
    }

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
     * Fraction of bases with Q-score at least 30 in the UMI sequences.
     */
    @QcThresholdEvaluated
    Double q30BasesInUmi

    /**
     * This metric is not defined in the official Cell Ranger output definition:
     * https://support.10xgenomics.com/single-cell-gene-expression/software/pipelines/latest/output/metrics
     */
    @QcThresholdEvaluated
    Double readsMappedToGenome

    /**
     * This metric is not defined in the official Cell Ranger output definition:
     * https://support.10xgenomics.com/single-cell-gene-expression/software/pipelines/latest/output/metrics
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToGenome

    /**
     * Fraction of reads that mapped to the intergenic regions of the genome
     * with a high mapping quality score as reported by the aligner.
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToIntergenicRegions

    /**
     * Fraction of reads that mapped to the intronic regions of the genome
     * with a high mapping quality score as reported by the aligner.
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToIntronicRegions

    /**
     * Fraction of reads that mapped to the exonic regions of the genome
     * with a high mapping quality score as reported by the aligner.
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToExonicRegions

    /**
     * Fraction of reads that mapped to a unique gene in the transcriptome
     * with a high mapping quality score as reported by the aligner.
     */
    @QcThresholdEvaluated
    Double readsMappedConfidentlyToTranscriptome

    /**
     * This metric is not defined in the official Cell Ranger output definition:
     * https://support.10xgenomics.com/single-cell-gene-expression/software/pipelines/latest/output/metrics
     */
    @QcThresholdEvaluated
    Double readsMappedAntisenseToGene

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

    static constraints = {
        referenceLength validator: { it == null }
    }

    SingleCellBamFile getBamFile() {
        return qualityAssessmentMergedPass.abstractBamFile as SingleCellBamFile
    }
}
