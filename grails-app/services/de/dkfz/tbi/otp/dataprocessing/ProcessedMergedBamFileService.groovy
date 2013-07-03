package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.*

class ProcessedMergedBamFileService {

    ProcessedMergingFileService processedMergingFileService

    DataProcessingFilesService dataProcessingFilesService

    MergingPassService mergingPassService

    public String fileNameNoSuffix(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        MergingSet mergingSet = mergedBamFile.mergingPass.mergingSet
        MergingWorkPackage mergingWorkPackage = mergingSet.mergingWorkPackage
        Sample sample = mergingWorkPackage.sample
        Individual individual = sample.individual
        String seqTypeName = "${mergingWorkPackage.seqType.name}_${mergingWorkPackage.seqType.libraryLayout}"
        return "${sample.sampleType.name}_${individual.pid}_${seqTypeName}_merged.mdup"
    }

    public String fileName(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String body = fileNameNoSuffix(mergedBamFile)
        return "${body}.bam"
    }

    public String filePath(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String dir = processedMergingFileService.directory(mergedBamFile)
        String filename = fileName(mergedBamFile)
        return "${dir}/${filename}"
    }

    public String fileNameForMetrics(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        return fileNameNoSuffix(mergedBamFile) + "_metrics.txt"
    }

    public String filePathForMetrics(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String dir = processedMergingFileService.directory(mergedBamFile)
        String filename = fileNameForMetrics(mergedBamFile)
        return "${dir}/${filename}"
    }

    public String fileNameForBai(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String body = fileName(mergedBamFile)
        return "${body}.bai"
    }

    public String filePathForBai(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String dir = processedMergingFileService.directory(mergedBamFile)
        String filename = fileNameForBai(mergedBamFile)
        return "${dir}/${filename}"
    }

    public ProcessedMergedBamFile save(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The parameter processedMergedBamFile are not allowed to be null")
        return assertSave(processedMergedBamFile)
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }

    public ProcessedMergedBamFile createMergedBamFile(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        type: AbstractBamFile.BamType.MDUP
                        )
        return save(processedMergedBamFile)
    }

    public boolean updateBamFile(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "The parameter bamFile is not allowed to be null")
        File file = new File(filePath(bamFile))
        if (!file.canRead()) {
            throw new RuntimeException("Can not read the bam file ${file}")
        }
        if (!file.size()) {
            throw new RuntimeException("The bam file ${file} is empty")
        }
        bamFile.fileExists = true
        bamFile.fileSize = file.length()
        bamFile.dateFromFileSystem = new Date(file.lastModified())
        assertSave(bamFile)
        return bamFile.fileSize
    }

    public boolean updateBamMetricsFile(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "The parameter bamFile is not allowed to be null")
        File file = new File(filePathForMetrics(bamFile))
        if (!file.canRead()) {
            throw new RuntimeException("Can not read the metrics file ${file}")
        }
        if (!file.size()) {
            throw new RuntimeException("The metrics file ${file} is empty")
        }
        bamFile.hasMetricsFile = true
        assertSave(bamFile)
        return true
    }

    public boolean updateBamFileIndex(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "The parameter bamFile is not allowed to be null")
        String path = filePathForBai(bamFile)
        File file = new File(path)
        if (!file.canRead()) {
            throw new RuntimeException("Can not read the index file ${file}")
        }
        if (!file.size()) {
            throw new RuntimeException("The index file ${file} is empty")
        }
        bamFile.hasIndexFile = true
        assertSave(bamFile)
        return true
    }
}
