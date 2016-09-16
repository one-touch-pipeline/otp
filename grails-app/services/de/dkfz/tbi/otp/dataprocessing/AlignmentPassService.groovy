package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Execution of the alignment on the particular data file is called AlignmentPass
 * AlignmentPass object is a process parameter of any AlignmentWorkflow
 * and AlignmentPassService is responsible to create and update this object
 *
 *
 */
class AlignmentPassService {

    def configService
    def processingOptionService
    ReferenceGenomeService referenceGenomeService

    private static final String ALIGNABLE_ALIGNMENT_PASS_DATA_FILE_CRITERIA = "seqTrack = ap.seqTrack AND fileType.type = :fileType"
    public static final String ALIGNABLE_ALIGNMENT_PASS_HQL =
    "FROM AlignmentPass ap " +
    "WHERE alignmentState = :alignmentState " +
    "AND seqTrack.fastqcState = :finished " +
    "AND NOT EXISTS (FROM RunSegment WHERE run = ap.seqTrack.run AND metaDataStatus <> :metaDataStatus) " +
    "AND EXISTS (FROM DataFile WHERE ${ALIGNABLE_ALIGNMENT_PASS_DATA_FILE_CRITERIA} AND fileExists = true AND fileSize > 0 AND fileWithdrawn = false) " +
    "AND NOT EXISTS (FROM DataFile WHERE ${ALIGNABLE_ALIGNMENT_PASS_DATA_FILE_CRITERIA} AND fileWithdrawn = true) " +
    "AND NOT EXISTS (FROM AlignmentPass WHERE seqTrack = ap.seqTrack AND workPackage = ap.workPackage AND identifier > ap.identifier) " +
    "AND seqTrack.sample.individual.project.processingPriority >= :minPriority " +
    "ORDER BY seqTrack.sample.individual.project.processingPriority DESC, ap.id ASC"

    public static final Map ALIGNABLE_ALIGNMENT_PASS_QUERY_PARAMETERS = Collections.unmodifiableMap([
        alignmentState: AlignmentState.NOT_STARTED,
        finished: SeqTrack.DataProcessingState.FINISHED,
        metaDataStatus: RunSegment.Status.COMPLETE,
        fileType: FileType.Type.SEQUENCE,
    ])


    /**
     * Checks if the {@link LibraryPreparationKit} and the {@link BedFile} are available for this {@link SeqTrack}.
     * If it is missing the method returns false, otherwise true.
     */
    public boolean isLibraryPreparationKitOrBedFileMissing(SeqTrack seqTrack) {
        notNull(seqTrack, "The input seqTrack of method isLibraryPreparationKitAvailable is null")

        if (seqTrack instanceof ExomeSeqTrack) {
            return seqTrack.libraryPreparationKit == null || seqTrack.configuredReferenceGenome == null ||
                   BedFile.findWhere(
                       libraryPreparationKit: seqTrack.libraryPreparationKit,
                       referenceGenome: seqTrack.configuredReferenceGenome,
                   ) == null
        } else {
            return false
        }
    }


    public AlignmentPass findAlignmentPassForProcessing(short minPriority) {
        return AlignmentPass.find(ALIGNABLE_ALIGNMENT_PASS_HQL, ALIGNABLE_ALIGNMENT_PASS_QUERY_PARAMETERS + [minPriority: minPriority])
    }

    public void alignmentPassStarted(AlignmentPass alignmentPass) {
        update(alignmentPass, AlignmentState.IN_PROGRESS)
    }

    public void alignmentPassFinished(AlignmentPass alignmentPass) {
        notNull(alignmentPass, "the alignmentPass for the method alignmentPassFinished ist null")
        update(alignmentPass, AlignmentState.FINISHED)
    }

    private void update(AlignmentPass alignmentPass, AlignmentState state) {
        notNull(alignmentPass, "The input alignmentPass of the method update is null")
        notNull(state, "The input state of the method update is null")

        alignmentPass.alignmentState = state
        assert(alignmentPass.save(flush: true))
    }

    public Realm realmForDataProcessing(AlignmentPass alignmentPass) {
        return configService.getRealmDataProcessing(project(alignmentPass))
    }

    public Project project(AlignmentPass alignmentPass) {
        return alignmentPass.project
    }

    public SeqType seqType(AlignmentPass alignmentPass) {
        return alignmentPass.seqType
    }

    public String referenceGenomePath(AlignmentPass alignmentPass) {
        ReferenceGenome referenceGenome = alignmentPass.referenceGenome
        assert referenceGenome
        String path = referenceGenomeService.fastaFilePath(alignmentPass.project, referenceGenome)
        if (!path) {
            throw new ProcessingException("Undefined path to reference genome for project ${project.name}")
        }
        return path
    }
}
