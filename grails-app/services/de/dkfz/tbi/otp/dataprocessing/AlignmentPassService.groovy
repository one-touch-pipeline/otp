package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState

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
    QualityAssessmentPassService qualityAssessmentPassService

    private static final String DATA_FILE_CRITERIA = "seqTrack = st AND fileType.type = :fileType"

    /**
     * HQL for finding <em>alignable</em> {@link SeqTrack}s.
     *
     * <p>A SeqTrack is considered <em>alignable</em> if and only if all of these criteria are
     * fulfilled:</p>
     * <ul>
     *   <li>It belongs to a project with a reference genome known to OTP.</li>
     *   <li>No {@link RunSegment} in a state other than {@link RunSegment.Status#COMPLETE} belongs
     *       to it.</li>
     *   <li>At least one non-withdrawn data file belongs to it.</li>
     *   <li>No withdrawn data file belongs to it.</li>
     * </ul>
     */
    public static final String ALIGNABLE_SEQTRACK_HQL =
    "FROM SeqTrack st " +
    "WHERE alignmentState = :alignmentState " +
    "AND NOT EXISTS (FROM RunSegment WHERE run = st.run AND metaDataStatus <> :metaDataStatus) " +
    "AND EXISTS (FROM DataFile WHERE ${DATA_FILE_CRITERIA} AND fileExists = true AND fileSize > 0 AND fileWithdrawn = false) " +
    "AND NOT EXISTS (FROM DataFile WHERE ${DATA_FILE_CRITERIA} AND fileWithdrawn = true) "

    public static final Map ALIGNABLE_SEQTRACK_QUERY_PARAMETERS = Collections.unmodifiableMap([
        metaDataStatus: RunSegment.Status.COMPLETE,
        fileType: FileType.Type.SEQUENCE,
    ])

    /**
     * Returns any <em>alignable</em> {@link SeqTrack} in alignment state
     * {@link SeqTrack.DataProcessingState#NOT_STARTED}.
     *
     * <p>See {@link #ALIGNABLE_SEQTRACK_HQL} for the definition of <em>alignable</em>.
     * <p>If no such SeqTrack exists, <code>null</code> is returned.</p>
     *
     * <p>Note that all these criteria have already been checked in
     * {@link SeqTrackService#setRunReadyForAlignment(Run)}. As opposed to that method, the sequence
     * type is <em>not</em> checked in this method because the alignment workflow might be extended
     * such that it can process further sequencing types, and it shall be possible to trigger those
     * alignments manually although they are not triggered automatically by the mentioned method.</p>
     * <p>The criteria for <em>alignable</em> {@link SeqTrack}s (see {@link #ALIGNABLE_SEQTRACK_HQL})
     * <em>could</em> be checked only in one place (i.e. either in this method or in the
     * aforementioned method), but it is safer to do it in both places. (A user might accidentally
     * manually set the alignment state of a SeqTrack to <code>NOT_STARTED</code> which belongs to a
     * project with a reference genome unknown to OTP.)</p>
     */
    private SeqTrack findAlignableSeqTrack() {
        SeqTrack seqTrack = SeqTrack.find(ALIGNABLE_SEQTRACK_HQL, [
            alignmentState: SeqTrack.DataProcessingState.NOT_STARTED
        ] << ALIGNABLE_SEQTRACK_QUERY_PARAMETERS)
        if (!seqTrack) {
            return null
        }
        if (isReferenceGenomeAvailable(seqTrack) && !isExomeEnrichmentKitAndBedFileMissing(seqTrack)) {
            return seqTrack
        } else {
            changeDataProcessingStateToInComplete(seqTrack)
            return null
        }
    }


    /**
     * Checks if the {@link ReferenceGenome} is available for this {@link Project} and {@link SeqType}.
     * If it is missing the method returns false, otherwise true.
     */
    public boolean isReferenceGenomeAvailable(SeqTrack seqTrack) {
        notNull(seqTrack, "The input seqTrack of method isReferenceGenomeAvailable is null")
        return ReferenceGenomeProjectSeqType.findByProjectAndSeqTypeAndDeprecatedDate(seqTrack.project, seqTrack.seqType, null)
    }


    /**
     * Checks if the {@link ExomeEnrichmentKit} and the {@link BedFile} are available for this {@link SeqTrack}.
     * If it is missing the method returns false, otherwise true.
     */
    public boolean isExomeEnrichmentKitAndBedFileMissing(SeqTrack seqTrack) {
        notNull(seqTrack, "The input seqTrack of method isExomeEnrichmentKitAvailable is null")

        if (seqTrack instanceof ExomeSeqTrack) {
            ExomeEnrichmentKit exomeEnrichmentKit = seqTrack.exomeEnrichmentKit
            if (!exomeEnrichmentKit) {
                return true
            }

            ReferenceGenome referenceGenome = ReferenceGenomeProjectSeqType.
                            findByProjectAndSeqTypeAndDeprecatedDate(seqTrack.project, seqTrack.seqType, null).referenceGenome
            return !(BedFile.findByReferenceGenomeAndExomeEnrichmentKit(referenceGenome, exomeEnrichmentKit))
        } else {
            return false
        }
    }


    /**
     * If the seqTrack can not be processed because of missing information the {@link DataProcessingState} will be changed to INCOMPLETE.
     */
    public void changeDataProcessingStateToInComplete(SeqTrack seqTrack) {
        notNull(seqTrack, "The input seqTrack of method changeDataProcessingStateToInComplete is null")

        updateSeqTrackDataProcessingState(seqTrack, SeqTrack.DataProcessingState.INCOMPLETE)
    }

    public AlignmentPass createAlignmentPass() {
        SeqTrack seqTrack = findAlignableSeqTrack()
        if (!seqTrack) {
            return null
        }
        int pass = AlignmentPass.countBySeqTrack(seqTrack)
        AlignmentPass alignmentPass = new AlignmentPass(identifier: pass, seqTrack: seqTrack)
        assert(alignmentPass.save(flush: true))
        return alignmentPass
    }

    public void alignmentPassStarted(AlignmentPass alignmentPass) {
        update(alignmentPass, SeqTrack.DataProcessingState.IN_PROGRESS)
    }

    public void alignmentPassFinished(AlignmentPass alignmentPass) {
        notNull(alignmentPass, "the alignmentPass for the method alignmentPassFinished ist null")
        update(alignmentPass, SeqTrack.DataProcessingState.FINISHED)
        ProcessedBamFile bamFile = ProcessedBamFile.findByAlignmentPass(alignmentPass)
        qualityAssessmentPassService.notStarted(bamFile)
    }

    private void update(AlignmentPass alignmentPass, SeqTrack.DataProcessingState state) {
        notNull(alignmentPass, "The input alignmentPass of the method update is null")
        notNull(state, "The input state of the method update is null")

        updateSeqTrackDataProcessingState(alignmentPass.seqTrack, state)
    }


    private void updateSeqTrackDataProcessingState(SeqTrack seqTrack, SeqTrack.DataProcessingState state) {
        notNull(seqTrack, "The input seqTrack of the method updateSeqTrackDataProcessingState is null")
        notNull(state, "The input state of the method updateSeqTrackDataProcessingState is null")

        seqTrack.alignmentState = state
        assert(seqTrack.save(flush: true))
    }

    public Realm realmForDataProcessing(AlignmentPass alignmentPass) {
        return configService.getRealmDataProcessing(project(alignmentPass))
    }

    public Project project(AlignmentPass alignmentPass) {
        return alignmentPass.seqTrack.sample.individual.project
    }

    public SeqType seqType(AlignmentPass alignmentPass) {
        return alignmentPass.seqTrack.seqType
    }

    public String referenceGenomePath(AlignmentPass alignmentPass) {
        Project project = project(alignmentPass)
        SeqType seqType = seqType(alignmentPass)
        ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
        String path = referenceGenomeService.fastaFilePath(project, referenceGenome)
        if (!path) {
            throw new ProcessingException("Undefined path to reference genome for project ${project.name}")
        }
        return path
    }

    public int maximalIdentifier(ProcessedBamFile processedBamFile) {
        notNull(processedBamFile, "the input bam file for the method maximalIdentifier is null")
        SeqTrack seqTrackPerBamFile = processedBamFile.alignmentPass.seqTrack
        int maxIdentifier = AlignmentPass.createCriteria().get {
            eq("seqTrack", seqTrackPerBamFile)
            projections{
                max("identifier")
            }
        }
        return maxIdentifier
    }
}
