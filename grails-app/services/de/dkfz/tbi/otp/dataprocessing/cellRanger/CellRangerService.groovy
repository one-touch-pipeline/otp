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

import grails.gorm.transactions.Transactional
import groovy.transform.TupleConstructor
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeIndexService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightCheckService
import de.dkfz.tbi.otp.utils.Md5SumService
import de.dkfz.tbi.util.spreadsheet.*

import java.nio.file.*

@Transactional
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

    AbstractMergedBamFileService abstractMergedBamFileService

    ReferenceGenomeIndexService referenceGenomeIndexService

    ProcessingOptionService processingOptionService

    QcTrafficLightCheckService qcTrafficLightCheckService

    Md5SumService md5SumService

    CellRangerWorkflowService cellRangerWorkflowService

    void createInputDirectoryStructure(SingleCellBamFile singleCellBamFile) {
        Realm realm = singleCellBamFile.realm
        String sampleName = singleCellBamFile.singleCellSampleName

        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)
        String unixGroup = singleCellBamFile.project.unixGroup

        Path sampleDirectory = fileSystem.getPath(singleCellBamFile.sampleDirectory.path)

        fileService.deleteDirectoryRecursively(sampleDirectory) //delete dir if exist from previous run
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(sampleDirectory, realm, unixGroup)

        singleCellBamFile.containedSeqTracks.groupBy { it.sampleIdentifier }.each { String sampleIdentifier, List<SeqTrack> seqTracks ->
            String sampleIdentifierDirName = sampleIdentifierForDirectoryStructure(sampleIdentifier)
            Path sampleIdentifierDirectory = sampleDirectory.resolve(sampleIdentifierDirName)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(sampleIdentifierDirectory, realm, unixGroup)
            seqTracks.sort { it.id }.withIndex(1).each { SeqTrack seqTrack, int laneCounter ->
                seqTrack.dataFilesWhereIndexFileIsFalse.sort { it.id }.each { DataFile dataFile ->
                    String formattedLaneNumber = String.valueOf(laneCounter).padLeft(3, '0')
                    String fileName = "${sampleName}_S1_L${formattedLaneNumber}_R${dataFile.mateNumber}_${formattedLaneNumber}.fastq.gz"
                    Path link = sampleIdentifierDirectory.resolve(fileName)
                    Path target = fileSystem.getPath(lsdfFilesService.getFileViewByPidPath(dataFile))
                    fileService.createLink(link, target, realm, unixGroup)
                }
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

        ReferenceGenomeIndex referenceGenomeIndex = workPackage.referenceGenomeIndex

        File indexFile = referenceGenomeIndexService.getFile(referenceGenomeIndex)

        String localCores = processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_CORE_COUNT)
        String localMem = processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_CORE_MEM)

        String fastqDirectories = singleCellBamFile.containedSeqTracks*.sampleIdentifier.unique().collect { String sampleIdentifier ->
            new File(singleCellBamFile.sampleDirectory, sampleIdentifierForDirectoryStructure(sampleIdentifier)).absolutePath
        }.join(",")

        Map<String, String> parameters = [
                (CellRangerParameters.ID.parameterName)           : singleCellBamFile.singleCellSampleName,
                (CellRangerParameters.FASTQ.parameterName)        : fastqDirectories,
                (CellRangerParameters.TRANSCRIPTOME.parameterName): indexFile.absolutePath,
                (CellRangerParameters.SAMPLE.parameterName)       : singleCellBamFile.singleCellSampleName,
                (CellRangerParameters.LOCAL_CORES.parameterName)  : localCores,
                (CellRangerParameters.LOCAL_MEM.parameterName)    : localMem,
        ]
        if (workPackage.expectedCells) {
            parameters[CellRangerParameters.EXPECT_CELLS.parameterName] = workPackage.expectedCells.toString()
        }
        if (workPackage.enforcedCells) {
            parameters[CellRangerParameters.FORCE_CELLS.parameterName] = workPackage.enforcedCells.toString()
        }
        return parameters
    }

    CellRangerQualityAssessment parseCellRangerQaStatistics(SingleCellBamFile singleCellBamFile) {
        Path path = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm).getPath(singleCellBamFile.qualityAssessmentCsvFile.absolutePath)
        Spreadsheet spreadsheet = new Spreadsheet(path.text, Delimiter.COMMA)
        CellRangerQualityAssessment qa = new CellRangerQualityAssessment()
        MetricsSummaryCsvColumn.values().each {
            Cell cell = spreadsheet.dataRows.first().getCellByColumnTitle(it.columnName)
            assert cell: "${it.columnName} can not be found in ${singleCellBamFile.qualityAssessmentCsvFile.absolutePath}"
            try {
                qa."${it.attributeName}" = cell.text.replaceAll("%\$", "").replaceAll(/,/, "") as Double
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Failed to parse '${cell.text}'\n")
            }
        }
        qa.qualityAssessmentMergedPass = singleCellBamFile.findOrSaveQaPass()
        qa.save(flush: true)
        return qa
    }

    void finishCellRangerWorkflow(SingleCellBamFile singleCellBamFile) {
        cellRangerWorkflowService.cleanupOutputDirectory(singleCellBamFile)

        completeBamFile(singleCellBamFile)

        qcTrafficLightCheckService.handleQcCheck(singleCellBamFile) {
            cellRangerWorkflowService.linkResultFiles(singleCellBamFile)
        }
    }

    private void completeBamFile(SingleCellBamFile singleCellBamFile) {
        assert singleCellBamFile.isMostRecentBamFile(): "The BamFile ${singleCellBamFile} is not the most recent one. This must not happen!"
        assert [
                AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ].contains(singleCellBamFile.fileOperationStatus)

        updateBamFile(singleCellBamFile)

        singleCellBamFile.workPackage.bamFileInProjectFolder = singleCellBamFile
        assert singleCellBamFile.workPackage.save(flush: true)

        abstractMergedBamFileService.updateSamplePairStatusToNeedProcessing(singleCellBamFile)
    }

    private void updateBamFile(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        Path resultDirectory = fileSystem.getPath(singleCellBamFile.resultDirectory.path)

        Path bamFile = resultDirectory.resolve(SingleCellBamFile.ORIGINAL_BAM_FILE_NAME)
        Path md5SumFileName = resultDirectory.resolve(SingleCellBamFile.ORIGINAL_BAM_MD5SUM_FILE_NAME)

        String md5SumFile = md5SumService.extractMd5Sum(md5SumFileName)

        singleCellBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        singleCellBamFile.fileSize = Files.size(bamFile)
        singleCellBamFile.md5sum = md5SumFile
        singleCellBamFile.fileExists = true
        singleCellBamFile.dateFromFileSystem = new Date(Files.getLastModifiedTime(bamFile).toMillis())
        assert singleCellBamFile.save(flush: true)
    }

    String sampleIdentifierForDirectoryStructure(String sampleIdentifier) {
        return sampleIdentifier.replaceAll(/[^A-Za-z0-9_-]/, "_")
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#singleCellBamFile.project, 'OTP_READ_ACCESS')")
    String getWebSummaryResultFileContent(SingleCellBamFile singleCellBamFile) throws NoSuchFileException, AccessDeniedException {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)

        Path file = fileSystem.getPath(singleCellBamFile.webSummaryResultFile.path)
        if (!Files.exists(file)) {
            throw new NoSuchFileException(file.toAbsolutePath().toString())
        }
        if (!Files.isReadable(file)) {
            throw new AccessDeniedException(file.toAbsolutePath().toString())
        }
        return file.text
    }
}
