package de.dkfz.tbi.otp.dataprocessing.cellRanger

import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.util.spreadsheet.*
import groovy.transform.*

class CellRangerService {

    @TupleConstructor
    enum MetricsSummaryCsvColumn {
        ESTIMATED_NUMBER_OF_CELLS("estimatedNumberOfCells", "Estimated Number of Cells"),
        MEAN_READS_PER_CELL("meanReadsPerCell", "Mean Reads per Cell"),
        MEDIAN_GENES_PER_CELL("medianGenesPerCell", "Median Genes per Cell"),
        NUMBER_OF_READS("numberOfReads", "Number of Reads"),
        VALID_BARCODES("validBarcodes", "Valid Barcodes"),
        SEQUENCING_SATURATION("sequencingSaturation", "Sequencing Saturation"),
        Q30_BASES_IN_BARCODE("q30BasesInBarcode", "Q30 Bases in Barcode"),
        Q30_BASES_IN_RNA_READ("q30BasesInRnaRead", "Q30 Bases in RNA Read"),
        Q30_BASES_IN_UMI("q30BasesInUmi", "Q30 Bases in UMI"),
        READS_MAPPED_TO_GENOME("readsMappedToGenome", "Reads Mapped to Genome"),
        READS_MAPPED_CONFIDENTLY_TO_GENOME("readsMappedConfidentlyToGenome", "Reads Mapped Confidently to Genome"),
        READS_MAPPED_CONFIDENTLY_TO_INTERGENIC_REGIONS("readsMappedConfidentlyToIntergenicRegions", "Reads Mapped Confidently to Intergenic Regions"),
        READS_MAPPED_CONFIDENTLY_TO_INTRONIC_REGIONS("readsMappedConfidentlyToIntronicRegions", "Reads Mapped Confidently to Intronic Regions"),
        READS_MAPPED_CONFIDENTLY_TO_EXONIC_REGIONS("readsMappedConfidentlyToExonicRegions", "Reads Mapped Confidently to Exonic Regions"),
        READS_MAPPED_CONFIDENTLY_TO_TRANSCRIPTOME("readsMappedConfidentlyToTranscriptome", "Reads Mapped Confidently to Transcriptome"),
        READS_MAPPED_ANTISENSE_TO_GENE("readsMappedAntisenseToGene", "Reads Mapped Antisense to Gene"),
        FRACTION_READS_IN_CELLS("fractionReadsInCells", "Fraction Reads in Cells"),
        TOTAL_GENES_DETECTED("totalGenesDetected", "Total Genes Detected"),
        MEDIAN_UMI_COUNTS_PER_CELL("medianUmiCountsPerCell", "Median UMI Counts per Cell"),

        final String attributeName
        final String columnName
    }

    CellRangerQualityAssessment parseCellRangerQaStatistics(SingleCellBamFile singleCellBamFile) {
        Spreadsheet spreadsheet = new Spreadsheet(singleCellBamFile.qualityAssessmentCsvFile.text, ",")
        CellRangerQualityAssessment qa = new CellRangerQualityAssessment()
        MetricsSummaryCsvColumn.values().each {
            Cell cell = spreadsheet.dataRows.first().getCellByColumnTitle(it.columnName)
            assert cell: "${it.columnName} can not be found in ${singleCellBamFile.qualityAssessmentCsvFile.absolutePath}"
            try {
                qa."${it.attributeName}" = cell.text.replaceAll("%\$", "") as Double
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Failed to parse '${cell.text}'\n")
            }
        }
        qa.qualityAssessmentMergedPass = singleCellBamFile.findOrSaveQaPass()
        qa.save(flush: true)
        return qa
    }

}
