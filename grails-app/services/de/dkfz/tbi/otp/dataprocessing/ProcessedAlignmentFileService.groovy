package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getLog

/**
 * This service implements alignment files organization convention
 *
 *
 */
class ProcessedAlignmentFileService {

    @Autowired
    ApplicationContext applicationContext
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
        final Project project = alignmentPass.project
        long freedBytes = 0L
        final Collection<ProcessedBamFile> bamFiles = ProcessedBamFile.findAllByAlignmentPass(alignmentPass)
        if (bamFiles.size() == 1) {
            final ProcessedBamFile bamFile = bamFiles.first()
            try {
                for (final QualityAssessmentPass qaPass : QualityAssessmentPass.findAllByProcessedBamFile(bamFile)) {
                    freedBytes += applicationContext.processedBamFileQaFileService.deleteProcessingFiles(qaPass)
                }
                dataProcessingFilesService.deleteProcessingDirectory(project,
                        applicationContext.processedBamFileQaFileService.directoryPath(alignmentPass))
                freedBytes += applicationContext.processedBamFileService.deleteProcessingFiles(bamFile)
                for (final ProcessedSaiFile saiFile : ProcessedSaiFile.findAllByAlignmentPass(alignmentPass)) {
                    freedBytes += applicationContext.processedSaiFileService.deleteProcessingFiles(saiFile)
                }
                dataProcessingFilesService.deleteProcessingDirectory(project, getDirectory(alignmentPass))
                log.debug "${freedBytes} bytes have been freed for alignment pass ${alignmentPass}."
            } catch (final FileNotInFinalDestinationException e) {
                log.error "The single lane QA files of alignment pass ${alignmentPass} are not in their final destination as expected: ${e.message} Skipping that alignment pass."
            }
        } else {
            log.error "Found ${bamFiles.size()} ProcessedBamFiles for AlignmentPass ${alignmentPass}. That's weird. Skipping that alignment pass."
        }
        return freedBytes
    }

    /**
     * Deletes the processing files of alignment passes that have already been merged and have been created before the
     * specified date.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteOldAlignmentProcessingFiles(final Date createdBefore) {
        log.info "Deleting processing files of merged alignment passes created before ${createdBefore}."
        final String queryPart =
                "WHERE (fileExists = true OR deletionDate IS NULL) AND dateCreated < :createdBefore " +
                        "AND EXISTS (FROM MergingSetAssignment msa " +
                        "            JOIN msa.mergingSet ms WITH ms.status = :processed "
        final Map params = [
                createdBefore: createdBefore,
                processed: MergingSet.State.PROCESSED,
        ]
        final Collection<AlignmentPass> alignmentPassesWithOldFiles = []
        alignmentPassesWithOldFiles.addAll(ProcessedBamFile.findAll(
                "FROM ProcessedBamFile pbf " + queryPart +
                        "            WHERE msa.bamFile = pbf)",
                params)*.alignmentPass)
        alignmentPassesWithOldFiles.addAll(ProcessedSaiFile.findAll(
                "FROM ProcessedSaiFile psf " + queryPart +
                        "            JOIN msa.bamFile pbf " +
                        "            WHERE pbf.alignmentPass = psf.alignmentPass)",
                params)*.alignmentPass)
        long freedBytes = 0L
        long processedPasses = 0L
        try {
            for (final AlignmentPass alignmentPass : alignmentPassesWithOldFiles.unique()) {
                if (ProcessedSaiFile.findByAlignmentPassAndDateCreatedGreaterThan(alignmentPass, createdBefore) != null) {
                    // A newer SAI file belongs to the alignment pass.
                    continue
                }
                for (final ProcessedBamFile bamFile : ProcessedBamFile.findAllByAlignmentPass(alignmentPass)) {
                    if (bamFile.dateCreated >= createdBefore) {
                        // A newer BAM file belongs to the alignment pass.
                        continue
                    }
                    final Collection<MergingSetAssignment> mergingSetAssignments = MergingSetAssignment.findAllByBamFile(bamFile)
                    if (mergingSetAssignments.empty) {
                        // The BAM file is not assigned to any merging set, specifically not to any processed merging set.
                        // TODO: Nevertheless we might delete it if it is withdrawn. -> OTP-711
                        continue
                    }
                    if (mergingSetAssignments.find {it.mergingSet.status != MergingSet.State.PROCESSED}) {
                        // The BAM file is assigned to a merging set which has not been processed yet.
                        // TODO: It might be sufficient to ensure that the latest merging set for the BAM file is in state PROCESSED. -> OTP-711
                        continue
                    }
                }
                freedBytes += deleteProcessingFiles(alignmentPass)
                processedPasses++
            }
        } finally {
            log.info "${freedBytes} bytes have been freed by deleting the processing files of ${processedPasses} merged alignment passes created before ${createdBefore}."
        }
        return freedBytes
    }
}
