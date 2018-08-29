package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.isTrue
import static org.springframework.util.Assert.notNull
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog

class ProcessedMergedBamFileService {

    AbstractBamFileService abstractBamFileService

    ProcessedAlignmentFileService processedAlignmentFileService

    DataProcessingFilesService dataProcessingFilesService

    ChecksumFileService checksumFileService

    MergingWorkPackageService mergingWorkPackageService


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
        if (processedMergedBamFile?.md5sum) {
            return AbstractMergedBamFileService.destinationDirectory(processedMergedBamFile)
        } else {
            return processingDirectory(mergingPass)
        }
    }

    public String processingDirectory(MergingPass mergingPass) {
        notNull mergingPass
        MergingSet mergingSet = mergingPass.mergingSet
        MergingWorkPackage mergingWorkPackage = mergingSet.mergingWorkPackage
        Sample sample = mergingWorkPackage.sample
        Individual individual = sample.individual
        DataProcessingFilesService.OutputDirectories dirType = DataProcessingFilesService.OutputDirectories.MERGING
        String baseDir = dataProcessingFilesService.getOutputDirectory(individual, dirType)
        String seqTypeName = "${mergingWorkPackage.seqType.name}/${mergingWorkPackage.seqType.libraryLayout}"
        String workPackageCriteriaPart = "${(mergingWorkPackage.processingType == MergingWorkPackage.ProcessingType.SYSTEM ? 'DEFAULT' : MergingWorkPackage.ProcessingType.MANUAL)}"
        String workPackageNamePart = "${seqTypeName}/${workPackageCriteriaPart}"
        String dir = "${sample.sampleType.name}/${workPackageNamePart}/${mergingSet.identifier}/pass${mergingPass.identifier}"
        return "${baseDir}/${dir}"
    }

    public String directory(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        return directory(mergedBamFile.mergingPass)
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
        locations.put("destinationDirectory", AbstractMergedBamFileService.destinationDirectory(file))

        locations.put("temporalDestinationDir", destinationTempDirectory(file))

        locations.put("bamFile", file.getBamFileName())
        locations.put("baiFile", file.getBaiFileName())

        locations.put("md5BamFile", checksumFileService.md5FileName(locations["bamFile"]))
        locations.put("md5BaiFile", checksumFileService.md5FileName(locations["baiFile"]))

        return locations
    }

    public String filePath(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String dir = directory(mergedBamFile)
        String filename = mergedBamFile.getBamFileName()
        return "${dir}/${filename}"
    }

    public String fileNameForMetrics(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        return mergedBamFile.fileNameNoSuffix() + "_metrics.txt"
    }

    public String filePathForBai(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        String dir = directory(mergedBamFile)
        String filename = mergedBamFile.baiFileName
        return "${dir}/${filename}"
    }

    /**
     * Names of additional files that are both in the processing directory and in the final destination project
     * directory.
     */
    public Collection<String> additionalFileNames(final ProcessedMergedBamFile bamFile) {
        return [
                bamFile.getBaiFileName(),
                checksumFileService.md5FileName(bamFile.getBaiFileName()),
                checksumFileService.md5FileName(bamFile.getBamFileName()),
        ]
    }

    /**
     * Names of additional files that are in the processing directory but not in the final destination project
     * directory.
     */
    public Collection<String> additionalFileNamesProcessingDirOnly(final ProcessedMergedBamFile bamFile) {
        return [
                fileNameForMetrics(bamFile),
        ]
    }

    public Project project(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, 'The parameter mergedBamFile are not allowed to be null')
        return mergedBamFile.project
    }

    public Sample sample(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "the processedMergedBamFile of the method sample is null")
        return processedMergedBamFile.sample
    }

    public SeqType seqType(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, 'The parameter mergedBamFile are not allowed to be null')
        return mergedBamFile.seqType
    }

    /**
     * @return the ProcessedMergedBamFile, which has to be copied to the project folder
     */
    public ProcessedMergedBamFile mergedBamFileWithFinishedQA(short minPriority) {
        List<AbstractBamFile.State> disallowedStatesAbstractBamFile = [
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
        List<ProcessedMergedBamFile> files = ProcessedMergedBamFile.createCriteria().list {
            eq("qualityAssessmentStatus", QaProcessingStatus.FINISHED)
            eq("fileOperationStatus", FileOperationStatus.NEEDS_PROCESSING)
            eq("withdrawn", false)
            isNull('md5sum')
            not { 'in'("status", disallowedStatesAbstractBamFile) }
            mergingPass {
                mergingSet {
                    eq ("status", MergingSet.State.PROCESSED)
                    mergingWorkPackage {
                        if (workPackageIdsOfFilesInTransfer) {
                            not { 'in'("id", workPackageIdsOfFilesInTransfer) }
                        }
                        sample {
                            individual {
                                project {
                                    ge('processingPriority', minPriority)
                                    order("processingPriority", "desc")
                                }
                            }
                        }
                    }
                }
            }
            order("id", "asc")
        }

        /**
         * a mergedBamFile will only be moved when
         * - it is not currently used in another merging process
         */
        for (file in files) {
            if (MergingSetAssignment.findAllByBamFile(file).every { it.mergingSet.status == MergingSet.State.PROCESSED }) {
                return file
            }
        }
        return null
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
        file.updateFileOperationStatus(FileOperationStatus.PROCESSED)
        file.md5sum = md5.toLowerCase(Locale.ENGLISH)
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
        return AbstractMergedBamFileService.destinationDirectory(file) + ".tmp"
    }

    /**
     * @param file, the ProcessedMergedBamFile for which the QA results were produced
     * @return path to the directory where the qa results for the merged and single lane bam files will be stored
     */
    public String qaResultDestinationDirectory(ProcessedMergedBamFile file) {
        notNull(file, "the input of the method qaResultDestinationDirectory is null")
        return AbstractMergedBamFileService.destinationDirectory(file) + '/' + QUALITY_ASSESSMENT_DIR
    }

    /**
     * @param file, the ProcessedMergedBamFile for which the QA results were produced
     * @return path to the directory where the qa results for the merged and single lane bam files will be stored temporarily (during the transfer workflow)
     */
    public String qaResultTempDestinationDirectory(ProcessedMergedBamFile file) {
        notNull(file, "the input of the method qaResultTempDestinationDirectory is null")
        return destinationTempDirectory(file) + '/' + QUALITY_ASSESSMENT_DIR
    }

    public LibraryPreparationKit libraryPreparationKit(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, 'bam file must not be null')
        isTrue(seqType(bamFile).name == SeqTypeNames.EXOME.seqTypeName, 'This method must be called only on exon data')

        List<ProcessedBamFile> singleLaneBamFiles = abstractBamFileService.findAllByProcessedMergedBamFile(bamFile)
        boolean hasSingleLaneBamFiles = singleLaneBamFiles
        isTrue(hasSingleLaneBamFiles, "there are no singleLaneBamFiles corresponding to the given $bamFile")

        List<SeqTrack> seqTracks = singleLaneBamFiles*.alignmentPass*.seqTrack
        // The domain ExomeSeqTrack is new, therefore it is possible that there are many bamFiles,
        // which do not have the connection to the LibraryPreparationKit.
        List wrongSeqTracks = seqTracks.findAll { it.class != ExomeSeqTrack }
        isTrue(wrongSeqTracks.empty, "The following seqTracks used to create the given $bamFile have not the type ExomeSeqTrack: $wrongSeqTracks.")

        LibraryPreparationKit firstKit = seqTracks.first().libraryPreparationKit
        wrongSeqTracks = seqTracks.findAll { it.libraryPreparationKit != firstKit }
        isTrue(wrongSeqTracks.empty, "Different kits were used in the following seqTracks: $wrongSeqTracks, which were used to create $bamFile.")

        return firstKit
    }

    /**
     * returns all fastq files, which are combined in the mergedBamFile
     */
    public List<DataFile> fastqFilesPerMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The input of the method fastqFilesPerMergedBamFile is null")
        Set<SeqTrack> seqTracks = processedMergedBamFile.getContainedSeqTracks()
        if (seqTracks) {
            return DataFile.createCriteria().list {
                'in'("seqTrack", seqTracks)
                fileType {
                    eq("type", FileType.Type.SEQUENCE)
                }
            }
        }
        return null
    }


    /**
     * Checks if the {@link LibraryPreparationKit} was inferred at least for one lane within the given {@link ProcessedMergedBamFile}.
     * This only has to be checked for seqType = Exome
     */
    public LibraryPreparationKit getInferredKit(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The input of method getInferredKit is null")
        SeqType seqType = seqType(processedMergedBamFile)
        // Only in case of seqtype = exome a library preparation kit can be available, for all other seqTypes null has to be returned
        if(seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            Collection<ExomeSeqTrack> exomeSeqTracks = processedMergedBamFile.containedSeqTracks.findAll { it instanceof ExomeSeqTrack }
            if(exomeSeqTracks*.libraryPreparationKit.unique().size > 1) {
                throw new ProcessingException("There is no unique kit for the mergedBamFile " + processedMergedBamFile)
            }
            return exomeSeqTracks.find({ it.kitInfoReliability == InformationReliability.INFERRED })?.libraryPreparationKit
        }
        return null
    }

    /**
     * Checks consistency for {@link #deleteProcessingFiles(ProcessedMergedBamFile)}.
     *
     * If there are inconsistencies, details are logged to the thread log (see {@link LogThreadLocal}).
     *
     * @return true if there is no serious inconsistency.
     */
    public boolean checkConsistencyForProcessingFilesDeletion(final ProcessedMergedBamFile bamFile) {
        notNull bamFile
        final File directory = new File(processingDirectory(bamFile.mergingPass))
        final String fileName = bamFile.getBamFileName()
        final File fsBamFile = new File(directory, fileName)
        if (!dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(bamFile, fsBamFile)) {
            return false
        }
        if (!bamFile.mergingPass.isLatestPass() || !bamFile.mergingSet.isLatestSet()) {
            // The merging results of this pass are outdated, so in the final location they will have been overwritten with
            // the results of a later pass. Hence, checking if the files of this pass are in the final location does not
            // make sense.
            return true
        }
        if (bamFile.md5sum == null) {
            threadLog.error "ProcessedMergedBamFile ${bamFile} does not have its md5sum set, although it belongs to the latest MergingPass of the latest MergingSet for its MergingWorkpackage."
            return false
        }
        dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(
                directory,
                new File(AbstractMergedBamFileService.destinationDirectory(bamFile)),
                additionalFileNames(bamFile) << fileName)
    }

    /**
     * Deletes the files listed below from the "processing" directory on the file system.
     * Sets {@link ProcessedMergedBamFile#fileExists} to <code>false</code> and
     * {@link ProcessedMergedBamFile#deletionDate} to the current time.
     *
     * <p>
     * The following files are deleted:
     * <ul>
     *     <li>.bam</li>
     *     <li>.bam.md5sum</li>
     *     <li>.bai</li>
     *     <li>.bai.md5sum</li>
     *     <li>_metrics.txt</li>
     * </ul>
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteProcessingFiles(final ProcessedMergedBamFile bamFile) {
        notNull bamFile
        if (!checkConsistencyForProcessingFilesDeletion(bamFile)) {
            return 0L
        }
        final File directory = new File(processingDirectory(bamFile.mergingPass))
        final Collection<String> allAdditionalFileNames = additionalFileNames(bamFile) + additionalFileNamesProcessingDirOnly(bamFile)
        return dataProcessingFilesService.deleteProcessingFiles(
                bamFile,
                new File(directory, bamFile.getBamFileName()),
                allAdditionalFileNames.collect { new File(directory, it) }.toArray(new File[0])
        )
    }
}
