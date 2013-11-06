package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.notNull
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*

/**
 *
 */
class ProcessedMergedBamFileService {

    ProcessedBamFileService processedBamFileService

    AbstractBamFileService abstractBamFileService

    ProcessedAlignmentFileService processedAlignmentFileService

    ConfigService configService

    DataProcessingFilesService dataProcessingFilesService

    MergingPassService mergingPassService

    ChecksumFileService checksumFileService

    MergedAlignmentDataFileService mergedAlignmentDataFileService

    QualityAssessmentPassService qualityAssessmentPassService

    MergingWorkPackageService mergingWorkPackageService

    ProcessedBamFileQaFileService processedBamFileQaFileService

    // "QualityAssessment" is part of the folder structure in the project directory.
    // The results of the QA-workflow are copied to "QualityAssessment"
    private static final String QUALITY_ASSESSMENT_DIR = "QualityAssessment"

    /**
     * The returned directory depends, if the file was already transfered or not. This can be checked by the property "md5sum"
     * in the ProcessedMergedBamFile.
     * When the field is empty the file was not copied already.
     *
     * @param mergingPass, the mergingPass, which belongs to the the mergedBamFile for which the directory is requested
     * @return the directory of the processedMergedBamFile
     */
    public String directory(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)
        if (processedMergedBamFile.md5sum) {
            return destinationDirectory(processedMergedBamFile)
        } else {
            MergingSet mergingSet = mergingPass.mergingSet
            MergingWorkPackage mergingWorkPackage = mergingSet.mergingWorkPackage
            Sample sample = mergingWorkPackage.sample
            Individual individual = sample.individual
            DataProcessingFilesService.OutputDirectories dirType = DataProcessingFilesService.OutputDirectories.MERGING
            String baseDir = dataProcessingFilesService.getOutputDirectory(individual, dirType)
            String seqTypeName = "${mergingWorkPackage.seqType.name}/${mergingWorkPackage.seqType.libraryLayout}"
            String workPackageCriteraPart = "${(mergingWorkPackage.processingType == MergingWorkPackage.ProcessingType.SYSTEM ? mergingWorkPackage.mergingCriteria : MergingWorkPackage.ProcessingType.MANUAL)}"
            String workPackageNamePart = "${seqTypeName}/${workPackageCriteraPart}"
            String dir = "${sample.sampleType.name}/${workPackageNamePart}/${mergingSet.identifier}/pass${mergingPass.identifier}"
            return "${baseDir}/${dir}"
        }
    }

    public String directory(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        return directory(mergedBamFile.mergingPass)
    }

    /**
     * @param bamFile, the mergedBamFile which has to be copied
     * @return the final directory of the mergedBamFile after copying
     */
    public String destinationDirectory (ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the input of the method destinationDirectory is null")
        Project project = project(bamFile)

        SeqType type = seqType(bamFile)
        Sample sample = sample(bamFile)

        String root = configService.getProjectRootPath(project)
        String relative = mergedAlignmentDataFileService.buildRelativePath(type, sample)

        return "${root}/${relative}"
    }

    /**
     * @param file, the mergedBamFile, which has to be copied
     * @return a {@link Map} containing the actual directory of the mergedBamFile,
     * the directory where the mergedBamFile has to be copied,
     * the name of the mergedBamFile,
     * the name of the baiFile,
     * the name of the md5sum file of the mergedBamFile,
     * the name of the md5sum file of the baiFile
     */
    public Map<String, String> locationsForFileCopying(ProcessedMergedBamFile file) {
        notNull(file, "the input for the method locationsForFileCopying is null")
        Map<String, String> locations = [:]

        locations.put("sourceDirectory", directory(file))
        locations.put("destinationDirectory", destinationDirectory(file))

        locations.put("temporalDestinationDir", destinationTempDirectory(file))

        locations.put("bamFile", fileName(file))
        locations.put("baiFile", fileNameForBai(file))

        locations.put("md5BamFile", checksumFileService.md5FileName(locations["bamFile"]))
        locations.put("md5BaiFile", checksumFileService.md5FileName(locations["baiFile"]))

        return locations
    }

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
        String dir = directory(mergedBamFile)
        String filename = fileName(mergedBamFile)
        return "${dir}/${filename}"
    }

    public String fileNameForMetrics(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        return fileNameNoSuffix(mergedBamFile) + "_metrics.txt"
    }

    public String filePathForMetrics(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String dir = directory(mergedBamFile)
        String filename = fileNameForMetrics(mergedBamFile)
        return "${dir}/${filename}"
    }

    public String fileNameForBai(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String body = fileNameNoSuffix(mergedBamFile)
        return "${body}.bai"
    }

    public String filePathForBai(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String dir = directory(mergedBamFile)
        String filename = fileNameForBai(mergedBamFile)
        return "${dir}/${filename}"
    }

    public Project project(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, 'The parameter mergedBamFile are not allowed to be null')
        return mergedBamFile.mergingPass.mergingSet.mergingWorkPackage.sample.individual.project
    }

    public Sample sample(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "the processedMergedBamFile of the method sample is null")
        return processedMergedBamFile.mergingPass.mergingSet.mergingWorkPackage.sample
    }

    public SeqType seqType(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, 'The parameter mergedBamFile are not allowed to be null')
        return mergedBamFile.mergingPass.mergingSet.mergingWorkPackage.seqType
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
    /**
     * @return the ProcessedMergedBamFile, which has to be copied to the project folder
     */
    public ProcessedMergedBamFile mergedBamFileWithFinishedQA() {
        List<MergingSet.State> disallowedStatesMergingSet = [
            MergingSet.State.DECLARED,
            MergingSet.State.NEEDS_PROCESSING,
            MergingSet.State.INPROGRESS
        ]

        List<AbstractBamFile.State> disallowedStatesAbstractBamFile = [
            AbstractBamFile.State.DECLARED,
            AbstractBamFile.State.NEEDS_PROCESSING,
            AbstractBamFile.State.INPROGRESS
        ]
        List<Long> workPackageIdsOfFilesInTransfer = []
        List<ProcessedMergedBamFile> workPackagesOfFilesInTransfer = mergingWorkPackageService.workPackagesOfFilesInTransfer()
        if (workPackagesOfFilesInTransfer) {
            workPackageIdsOfFilesInTransfer = workPackagesOfFilesInTransfer*.id
        }
        /**
         * processedMergedBamFiles are only picked if they fulfill the following constraints:
         * - the QA-WF for all related files is finished
         * - it needs to be moved
         * - it was not already moved (md5sum is null)
         * - the creation of this mergedBamFile (merging) was finished
         * - no other processedMergedBamFile from the same workpackage is currently in transfer
         */
        List<ProcessedMergedBamFile> files =  ProcessedMergedBamFile.createCriteria().list {
            eq("qualityAssessmentStatus", QaProcessingStatus.FINISHED)
            eq("fileOperationStatus", FileOperationStatus.NEEDS_PROCESSING)
            eq("withdrawn", false)
            isNull('md5sum')
            not { 'in'("status", disallowedStatesAbstractBamFile) }
            mergingPass {
                mergingSet {
                    not { 'in' ("status", disallowedStatesMergingSet) }
                    if (workPackageIdsOfFilesInTransfer) {
                        mergingWorkPackage {
                            not { 'in' ("id", workPackageIdsOfFilesInTransfer) }
                        }
                    }
                }
            }
            // the processedMergedBamFiles are sorted by id to receive the oldest file
            order("id", "asc")
        }
        /**
         * a mergedBamFile will only be moved when
         * - the qa-workflow for the single lane bam files, which build this merged bam file, is finished
         * - it is not currently used in another merging process
         */
        for (file in files) {
            List<ProcessedBamFile> bamFiles = abstractBamFileService.findByProcessedMergedBamFile(file).findAll { it instanceof ProcessedBamFile }
            List<MergingSetAssignment> mergingSetAssignments = MergingSetAssignment.findAllByBamFile(file)

            if (bamFiles.every { it.qualityAssessmentStatus == QaProcessingStatus.FINISHED }) {
                if (mergingSetAssignments.every { it.mergingSet.status == MergingSet.State.PROCESSED }) {
                    return file
                }
            }
        }
        return null
    }

    public void updateFileOperationStatus(ProcessedMergedBamFile file, FileOperationStatus status) {
        notNull(file, "the input file for the method updateFileOperationStatus is null")
        notNull(status, "the input status for the method updateFileOperationStatus is null")
        file.fileOperationStatus = status
        assertSave(file)
    }

    /**
     * stores the md5Sum of the ProcessedMergedBamFile in the database, after the copying step was finished successfully
     *
     * @param file, the processedMergedBamFile, which was copied
     * @param md5, the md5 checksum for the copied file
     */
    public boolean storeMD5Digest(ProcessedMergedBamFile file, String md5) {
        notNull(file, "the input 'file' for the method storeMD5Digest is null")
        notNull(md5, "the input 'md5' for the method storeMD5Digest is null")
        file.fileOperationStatus = FileOperationStatus.PROCESSED
        file.md5sum = md5
        return (file.save(flush: true) != null)
    }

    /**
     * Construct the path to a <i>temporary</i> directory that is used as a staging area during copying of
     * the latest QA results (highest pass number).
     *
     * @param file, the processedMergedBamFile, which has to be copied
     * @return the path to the temporary directory
     */
    public String destinationTempDirectory(ProcessedMergedBamFile file) {
        notNull(file, "the input of the method destinationTempDirectory is null")
        return destinationDirectory(file) + ".tmp"
    }

    /**
     * @param file, the ProcessedMergedBamFile for which the QA results were produced
     * @return path to the directory where the qa results for the merged and single lane bam files will be stored temporarily (during the transfer workflow)
     */
    public String qaResultTempDestinationDirectory(ProcessedMergedBamFile file) {
        notNull(file, "the input of the method qaResultTempDestinationDirectory is null")
        return destinationTempDirectory(file) + '/' + QUALITY_ASSESSMENT_DIR
    }

    /**
     * @param mergedBamFile, the processedMergedBamFile for which the corresponding single lane qa result files are needed
     * @return a map containing the run lane directories as keys and the source directories of the single lane qa results
     */
    public Map<String, String> singleLaneQAResultsDirectories (ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "the input of the method singleLaneQAResultsDirectories is null")
        Map<String, String> singleLaneQAResultsDirectories = [:]
        List<AbstractBamFile> bamFiles = abstractBamFileService.findByProcessedMergedBamFile(mergedBamFile)
        for (bamFile in bamFiles) {
            if (bamFile instanceof ProcessedBamFile) {
                QualityAssessmentPass pass = qualityAssessmentPassService.latestQualityAssessmentPass(bamFile)
                SeqTrack track = processedBamFileService.seqTrack(bamFile)
                String sourcePath = processedBamFileQaFileService.directoryPath(pass)
                String destinationDirectoryName = processedAlignmentFileService.getRunLaneDirectory(track)
                singleLaneQAResultsDirectories.put(destinationDirectoryName, sourcePath)
            }
        }
        return singleLaneQAResultsDirectories
    }
}
