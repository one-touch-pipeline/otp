package de.dkfz.tbi.otp.dataprocessing.cellRanger

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.util.spreadsheet.*
import groovy.transform.*

import java.nio.file.*

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


    FileSystemService fileSystemService

    FileService fileService

    LsdfFilesService lsdfFilesService

    ReferenceGenomeIndexService referenceGenomeIndexService

    ProcessingOptionService processingOptionService


    void createInputDirectoryStructure(SingleCellBamFile singleCellBamFile) {
        Realm realm = singleCellBamFile.realm
        String sampleName = singleCellBamFile.singleCellSampleName

        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

        Path sampleDirectory = fileSystem.getPath(singleCellBamFile.sampleDirectory.path)

        fileService.deleteDirectoryRecursively(sampleDirectory) //delete dir if exist from previous run
        fileService.createDirectoryRecursively(sampleDirectory)

        singleCellBamFile.containedSeqTracks.sort {
            it.id
        }.withIndex(1).collect { SeqTrack seqTrack, int laneCounter ->
            String formattedLaneNumber = String.valueOf(laneCounter).padLeft(3, '0')
            seqTrack.dataFiles.each { DataFile dataFile ->
                String fileName = "${sampleName}_S1_L${formattedLaneNumber}_R${dataFile.mateNumber}_${formattedLaneNumber}.fastq.gz"
                Path link = sampleDirectory.resolve(fileName)
                Path target = fileSystem.getPath(lsdfFilesService.getFileViewByPidPath(dataFile))
                fileService.createRelativeLink(link, target)
            }
        }
    }

    void deleteOutputDirectoryStructureIfExists(SingleCellBamFile singleCellBamFile) {
        Realm realm = singleCellBamFile.realm
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)
        Path outputDirectory = fileSystem.getPath(singleCellBamFile.outputDirectory.path)
        fileService.deleteDirectoryRecursively(outputDirectory)
    }

    void validateFilesExistsInResultDirectory(SingleCellBamFile singleCellBamFile) {
        Realm realm = singleCellBamFile.realm
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)
        Path resultDir = fileService.toPath(singleCellBamFile.resultDirectory, fileSystem)

        SingleCellBamFile.CREATED_RESULT_FILES.each {
            fileService.ensureFileIsReadableAndNotEmpty(resultDir.resolve(it))
        }

        SingleCellBamFile.CREATED_RESULT_DIRS.each {
            fileService.ensureDirIsReadableAndNotEmpty(resultDir.resolve(it))
        }
    }

    Map<String, String> createCellRangerParameters(SingleCellBamFile singleCellBamFile) {
        assert singleCellBamFile

        CellRangerMergingWorkPackage workPackage = singleCellBamFile.workPackage

        ReferenceGenomeIndex referenceGenomeIndex = workPackage.config.referenceGenomeIndex

        File indexFile = referenceGenomeIndexService.getFile(referenceGenomeIndex)

        String localCores = processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_CORE_COUNT)
        String localMem = processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_CORE_MEM)

        Map<String, String> parameters = [
                (CellRangerParameters.ID.parameterName)           : singleCellBamFile.singleCellSampleName,
                (CellRangerParameters.FASTQ.parameterName)        : singleCellBamFile.sampleDirectory.absolutePath,
                (CellRangerParameters.TRANSCRIPTOME.parameterName): indexFile.absolutePath,
                (CellRangerParameters.SAMPLE.parameterName)       : singleCellBamFile.singleCellSampleName,
                (CellRangerParameters.EXPECT_CELLS.parameterName) : workPackage.expectedCells.toString(),
                (CellRangerParameters.LOCAL_CORES.parameterName)  : localCores,
                (CellRangerParameters.LOCAL_MEM.parameterName)    : localMem,
        ]
        if (workPackage.enforcedCells) {
            parameters[CellRangerParameters.FORCE_CELLS.parameterName] = workPackage.enforcedCells.toString()
        }
        return parameters
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
