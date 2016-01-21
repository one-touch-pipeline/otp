package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog
import static org.springframework.util.Assert.*

import org.springframework.transaction.annotation.Transactional

import de.dkfz.tbi.otp.ngsdata.*

class MergingPassService {

    /**
     * deleteOldMergingProcessingFiles and deleteProcessingFiles should not be executed transactionally because if the
     * deletion of one processing file fails, the successfully deleted files should still be marked as deleted in the
     * database.
     */
    static transactional = false

    ConfigService configService
    AbstractBamFileService abstractBamFileService
    DataProcessingFilesService dataProcessingFilesService
    MergingSetService mergingSetService
    ProcessedMergedBamFileService processedMergedBamFileService
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Transactional
    MergingPass create(short minPriority) {
        MergingSet mergingSet = mergingSetService.mergingSetInStateNeedsProcessing(minPriority)
        if (mergingSet) {
            MergingPass mergingPass = new MergingPass(
                    mergingSet: mergingSet,
                    identifier: MergingPass.nextIdentifier(mergingSet),
            )
            assertSave(mergingPass)
            log.debug("created a new mergingPass ${mergingPass} for mergingSet ${mergingSet}")
            return mergingPass
        }
        return null
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }

    @Transactional
    public Realm realmForDataProcessing(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        return configService.getRealmDataProcessing(project(mergingPass))
    }

    @Transactional
    public Project project(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        return mergingPass.project
    }

    @Transactional
    public void mergingPassStarted(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        updateMergingSet(mergingPass, MergingSet.State.INPROGRESS)
    }

    /**
     * After the merging is finished the state of the MergingSet is set to PROCESSED
     * and the mergedQA is triggered automatically.
     */
    @Transactional
    public void mergingPassFinishedAndStartQA(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        updateMergingSet(mergingPass, MergingSet.State.PROCESSED)
        mergedBamFileSetQaNotStarted(mergingPass)
    }

    private void updateMergingSet(MergingPass mergingPass, MergingSet.State state) {
        mergingPass.mergingSet.status = state
        assertSave(mergingPass.mergingSet)
    }

    /**
     * Change the qualityAssessmentStatus of the processedMergedBamFile, which belongs to the input mergingPass, to NOT_STARTED
     */
    @Transactional
    public void mergedBamFileSetQaNotStarted(MergingPass mergingPass) {
        notNull(mergingPass, "The input of the method passNotStarted is null")
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)
        processedMergedBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.NOT_STARTED
        assertSave(processedMergedBamFile)
    }

    /**
     * Deletes the files of the specified merging pass from the "processing" directory on the file system.
     *
     * These will be deleted:
     * <ul>
     *     <li>Processing files from QA. See {@link ProcessedMergedBamFileQaFileService#deleteProcessingFiles(QualityAssessmentMergedPass)}.</li>
     *     <li>The "QualityAssessment" directory if it is empty.</li>
     *     <li>The BAM file. See {@link ProcessedMergedBamFileService#deleteProcessingFiles(ProcessedMergedBamFile)}.</li>
     *     <li>The processing directory of the merging pass if it is empty.</li>
     * </ul>
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteProcessingFiles(final MergingPass mergingPass) {
        notNull mergingPass
        final Project project = mergingPass.project
        long freedBytes = 0L
        final Collection<ProcessedMergedBamFile> bamFiles = ProcessedMergedBamFile.findAllByMergingPass(mergingPass)
        if (bamFiles.size() == 1) {
            final ProcessedMergedBamFile bamFile = bamFiles.first()
            final Collection<QualityAssessmentMergedPass> qaPasses = QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(bamFile)
            boolean consistent = true
            if (!bamFile.withdrawn) {
                qaPasses.each {
                    if (!processedMergedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(it)) {
                        consistent = false
                    }
                }
                if (!processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(bamFile)) {
                    consistent = false
                }
            }
            if (consistent) {
                qaPasses.each {
                    freedBytes += processedMergedBamFileQaFileService.deleteProcessingFiles(it)
                }
                dataProcessingFilesService.deleteProcessingDirectory(project,
                        processedMergedBamFileQaFileService.qaProcessingDirectory(mergingPass))
                freedBytes += processedMergedBamFileService.deleteProcessingFiles(bamFile)
                dataProcessingFilesService.deleteProcessingDirectory(project, processedMergedBamFileService.processingDirectory(mergingPass))
                threadLog.debug "${freedBytes} bytes have been freed for merging pass ${mergingPass}."
            } else {
                threadLog.error "There was at least one inconsistency (see earlier log message(s)) for merging pass ${mergingPass}. Skipping that merging pass."
            }
        } else {
            threadLog.error "Found ${bamFiles.size()} ProcessedMergedBamFiles for MergingPass ${mergingPass}. That's weird. Skipping that merging pass."
        }
        return freedBytes
    }

    /**
     * Deletes the processing files of merging passes that have been created before the specified date and satisfy at
     * least one of the following criteria:
     * <ul>
     *     <li>It has been copied to the final destination.</li>
     *     <li>For the same merging set there is a later merging pass that has been processed.</li>
     *     <li>The resulting BAM file has been further merged in a subsequent merging set.</li>
     *     <li>The bam file is withdrawn.</li>
     * </ul>
     *
     * See {@link #deleteProcessingFiles(MergingPass)} for details about which files are deleted.
     *
     * @param millisMaxRuntime If more than this number of milliseconds elapse during the execution of this
     * method, the method will return even if not all merging passes have been processed.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteOldMergingProcessingFiles(final Date createdBefore, final long millisMaxRuntime = Long.MAX_VALUE) {
        notNull createdBefore
        return dataProcessingFilesService.deleteOldProcessingFiles(this, "merging", createdBefore, millisMaxRuntime, {
            ProcessedMergedBamFile.findAll(
                "FROM ProcessedMergedBamFile bf1 WHERE " +
                "((" +
                    // already transferred
                    "qualityAssessmentStatus = :qaStatus " +
                    "AND fileOperationStatus = :fileOpStatus " +
                    "AND md5sum IS NOT NULL" +
                ") OR (" +
                    // later pass has been processed
                    "bf1.mergingPass.mergingSet.status = :mergingSetStatus AND EXISTS (" +
                        "FROM ProcessedMergedBamFile bf2 " +
                        "WHERE bf2.qualityAssessmentStatus = :qaStatus " +
                        "AND bf2.dateCreated < :createdBefore " +
                        "AND (bf2.withdrawn = false OR bf1.withdrawn = true) " +
                        "AND bf2.mergingPass.mergingSet = bf1.mergingPass.mergingSet " +
                        "AND bf2.mergingPass.identifier > bf1.mergingPass.identifier)" +
                ") OR (" +
                    // further merged
                    AbstractBamFileService.QUALITY_ASSESSED_AND_MERGED_QUERY +
                ") OR (" +
                    // withdrawn
                    "bf1.withdrawn = true " +
                ")) AND (fileExists = true OR deletionDate IS NULL) AND dateCreated < :createdBefore",
                [
                        createdBefore: createdBefore,
                        qaStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                        mergingSetStatus: MergingSet.State.PROCESSED,
                        status: AbstractBamFile.State.PROCESSED,
                        fileOpStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                ])*.mergingPass.unique()
        })
    }

    public boolean mayProcessingFilesBeDeleted(final MergingPass pass, final Date createdBefore) {
        notNull pass
        notNull createdBefore
        final Collection<ProcessedMergedBamFile> bamFiles = ProcessedMergedBamFile.findAllByMergingPass(pass)
        if (bamFiles.size() != 1) {
            threadLog.error "Found ${bamFiles.size()} ProcessedMergedBamFiles for MergingPass ${pass}. That's weird."
            return false
        }
        final ProcessedMergedBamFile bamFile = bamFiles.first()
        if (bamFile.dateCreated >= createdBefore) {
            // The BAM file is not old enough.
            return false
        }
        if (bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED &&
                bamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED &&
                bamFile.md5sum != null) {
            // Processing files have already been copied to the final destination.
            return true
        }
        if (bamFile.mergingSet.status == MergingSet.State.PROCESSED) {
            if (ProcessedMergedBamFile.createCriteria().get {
                eq("qualityAssessmentStatus", AbstractBamFile.QaProcessingStatus.FINISHED)
                lt("dateCreated", createdBefore)
                eq("withdrawn", false)
                mergingPass {
                    eq("mergingSet", pass.mergingSet)
                    gt("identifier", pass.identifier)
                }
                maxResults(1)
            } != null) {
                // For the same merging set there is a later merging pass that has been processed.
                return true
            }
        }
        if (abstractBamFileService.hasBeenQualityAssessedAndMerged(bamFile, createdBefore)) {
            return true
        }
        if (bamFile.withdrawn) {
            return true
        }
        return false
    }
}
