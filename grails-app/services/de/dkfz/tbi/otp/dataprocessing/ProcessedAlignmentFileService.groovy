package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog
import static org.springframework.util.Assert.notNull

/**
 * This service implements alignment files organization convention
 *
 *
 */
class ProcessedAlignmentFileService {

    /**
     * deleteOldAlignmentProcessingFiles and deleteProcessingFiles should not be executed transactionally because if the
     * deletion of one processing file fails, the successfully deleted files should still be marked as deleted in the
     * database.
     */
    static transactional = false

    @Autowired
    ApplicationContext applicationContext
    AbstractBamFileService abstractBamFileService
    DataProcessingFilesService dataProcessingFilesService

    public String getDirectory(AlignmentPass alignmentPass) {
        Individual ind = alignmentPass.seqTrack.sample.individual
        def dirType = DataProcessingFilesService.OutputDirectories.ALIGNMENT
        String baseDir = dataProcessingFilesService.getOutputDirectory(ind, dirType)
        String middleDir = getRunLaneDirectory(alignmentPass.seqTrack)
        return "${baseDir}/${middleDir}/${alignmentPass.getDirectory()}"
    }

    public String getRunLaneDirectory(SeqTrack seqTrack) {
        String runName = seqTrack.run.name
        String lane = seqTrack.laneId
        return "${runName}_${lane}"
    }

    /**
     * Deletes the files of the specified alignment pass from the "processing" directory on the file system.
     *
     * These will be deleted:
     * <ul>
     *     <li>Processing files from QA. See {@link ProcessedBamFileQaFileService#deleteProcessingFiles(QualityAssessmentPass)}.</li>
     *     <li>The "QualityAssessment" directory if it is empty.</li>
     *     <li>The BAM file. See {@link ProcessedBamFileService#deleteProcessingFiles(ProcessedBamFile)}.</li>
     *     <li>The SAI file(s). See {@link ProcessedSaiFileService#deleteProcessingFiles(ProcessedSaiFile)}.</li>
     *     <li>The processing directory of the alignment pass if it is empty.</li>
     * </ul>
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteProcessingFiles(final AlignmentPass alignmentPass) {
        notNull alignmentPass
        final Project project = alignmentPass.project
        long freedBytes = 0L
        final Collection<ProcessedBamFile> bamFiles = ProcessedBamFile.findAllByAlignmentPass(alignmentPass)
        if (bamFiles.size() == 1) {
            final ProcessedBamFile bamFile = bamFiles.first()
            final Collection<QualityAssessmentPass> qaPasses = QualityAssessmentPass.findAllByProcessedBamFile(bamFile)
            final Collection<ProcessedSaiFile> saiFiles = ProcessedSaiFile.findAllByAlignmentPass(alignmentPass)
            boolean consistent = true
            if (!bamFile.withdrawn) {
                qaPasses.each {
                    if (!applicationContext.processedBamFileQaFileService.checkConsistencyForProcessingFilesDeletion(it)) {
                        consistent = false
                    }
                }
                if (!applicationContext.processedBamFileService.checkConsistencyForProcessingFilesDeletion(bamFile)) {
                    consistent = false
                }
                saiFiles.each {
                    if (!applicationContext.processedSaiFileService.checkConsistencyForProcessingFilesDeletion(it)) {
                        consistent = false
                    }
                }
            }
            if (consistent) {
                qaPasses.each {
                    freedBytes += applicationContext.processedBamFileQaFileService.deleteProcessingFiles(it)
                }
                dataProcessingFilesService.deleteProcessingDirectory(project,
                        applicationContext.processedBamFileQaFileService.directoryPath(alignmentPass))
                freedBytes += applicationContext.processedBamFileService.deleteProcessingFiles(bamFile)
                saiFiles.each {
                    freedBytes += applicationContext.processedSaiFileService.deleteProcessingFiles(it)
                }
                dataProcessingFilesService.deleteProcessingDirectory(project, getDirectory(alignmentPass))
                threadLog.debug "${freedBytes} bytes have been freed for alignment pass ${alignmentPass}."
            } else {
                threadLog.error "There was at least one inconsistency (see earlier log message(s)) for alignment pass ${alignmentPass}. Skipping that alignment pass."
            }
        } else {
            threadLog.error "Found ${bamFiles.size()} ProcessedBamFiles for AlignmentPass ${alignmentPass}. That's weird. Skipping that alignment pass."
        }
        return freedBytes
    }

    /**
     * Deletes the processing files of alignment passes that have been created before the specified date and satisfy at
     * least one of the following criteria:
     * <ul>
     *     <li>For the same SeqTrack there is a later alignment pass that has been processed.</li>
     *     <li>The resulting BAM file has been merged.</li>
     *     <li>The bam file is withdrawn.</li>
     * </ul>
     *
     * See {@link #deleteProcessingFiles(AlignmentPass)} for details about which files are deleted.
     *
     * @param millisMaxRuntime If more than this number of milliseconds elapse during the execution of this
     * method, the method will return even if not all alignment passes have been processed.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteOldAlignmentProcessingFiles(final Date createdBefore, final long millisMaxRuntime = Long.MAX_VALUE) {
        notNull createdBefore
        return dataProcessingFilesService.deleteOldProcessingFiles(this, "alignment", createdBefore, millisMaxRuntime, {
            final String query1 =
                "FROM ProcessedBamFile bf1 WHERE " +
                "((" +
                    // later pass has been processed
                    "bf1.alignmentPass.alignmentState = :alignmentState AND EXISTS (" +
                        "FROM ProcessedBamFile bf2 " +
                        "WHERE bf2.qualityAssessmentStatus = :qaStatus " +
                        "AND bf2.dateCreated < :createdBefore " +
                        "AND (bf2.withdrawn = false OR bf1.withdrawn = true) " +
                        "AND bf2.alignmentPass.seqTrack = bf1.alignmentPass.seqTrack " +
                        "AND bf2.alignmentPass.identifier > bf1.alignmentPass.identifier)" +
                ") OR (" +
                    // merged
                    AbstractBamFileService.QUALITY_ASSESSED_AND_MERGED_QUERY_WITHOUT_QA_CHECK +
                ") OR (" +
                    // withdrawn
                    "bf1.withdrawn = true" +
                "))"
            final String query2 = " AND (fileExists = true OR deletionDate IS NULL) AND dateCreated < :createdBefore"
            final Map params = [
                    createdBefore: createdBefore,
                    qaStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                    mergingSetStatus: MergingSet.State.PROCESSED,
                    status: AbstractBamFile.State.PROCESSED,
                    alignmentState: AlignmentState.FINISHED,
            ]
            final Collection<AlignmentPass> passes = []
            passes.addAll(ProcessedBamFile.findAll(
                    query1 + query2,
                    params)*.alignmentPass)
            passes.addAll(ProcessedSaiFile.findAll(
                    "FROM ProcessedSaiFile sf WHERE EXISTS (" + query1 + " AND bf1.alignmentPass = sf.alignmentPass)" + query2,
                    params)*.alignmentPass)
            passes.unique()
        })
    }

    public boolean mayProcessingFilesBeDeleted(final AlignmentPass pass, final Date createdBefore) {
        notNull pass
        notNull createdBefore
        // The ProcessedBamFile and all ProcessedSaiFiles of one alignment pass should have the same dateCreated, but
        // the database does not enforce this. So, to be safe, check the dates of all SAI files and the BAM file.
        if (ProcessedSaiFile.findByAlignmentPassAndDateCreatedGreaterThan(pass, createdBefore) != null) {
            // A newer SAI file belongs to the alignment pass.
            return false
        }
        final Collection<ProcessedBamFile> bamFiles = ProcessedBamFile.findAllByAlignmentPass(pass)
        if (bamFiles.size() != 1) {
            threadLog.error "Found ${bamFiles.size()} ProcessedBamFiles for AlignmentPass ${pass}. That's weird."
            return false
        }
        final ProcessedBamFile bamFile = bamFiles.first()
        if (bamFile.dateCreated >= createdBefore) {
            // The BAM file is not old enough.
            return false
        }
        if (pass.alignmentState == AlignmentState.FINISHED) {
            if (ProcessedBamFile.createCriteria().get {
                eq("qualityAssessmentStatus", QaProcessingStatus.FINISHED)
                lt("dateCreated", createdBefore)
                eq("withdrawn", false)
                alignmentPass {
                    eq("seqTrack", pass.seqTrack)
                    gt("identifier", pass.identifier)
                }
                maxResults(1)
            } != null) {
                // For the same SeqTrack there is a later alignment pass that has been processed.
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
