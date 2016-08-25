package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity

class AlignmentPass implements ProcessParameterObject, Entity {

    enum AlignmentState {
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED,
        /**
         * For legacy data only.
         */
        UNKNOWN,
    }

    int identifier
    SeqTrack seqTrack
    MergingWorkPackage workPackage
    AlignmentState alignmentState

    static belongsTo = [MergingWorkPackage, SeqTrack]

    static constraints = {
        identifier(unique: 'seqTrack')
        seqTrack(validator: { SeqTrack seqTrack, AlignmentPass pass ->
            pass.workPackage?.satisfiesCriteria(seqTrack) })
        workPackage(validator: {workPackage -> workPackage.pipeline.name == Pipeline.Name.DEFAULT_OTP})
    }

    /**
     * The reference genome which is/was used by this alignment pass. This value does not change (in contrast to the
     * return value of {@link SeqTrack#getConfiguredReferenceGenome()} when the configuration changes).
     */
    public ReferenceGenome getReferenceGenome() {
        return workPackage.referenceGenome
    }

    public String getDirectory() {
        return "pass${identifier}"
    }

    public String toString() {
        return "AP ${id}: pass ${identifier} " + (latestPass ? "(latest) " : "") + "on ${seqTrack}"
    }

    /**
     * @return <code>true</code>, if this pass is the latest for the referenced {@link MergingWorkPackage} and {@link SeqTrack}
     */
    public boolean isLatestPass() {
        return identifier == maxIdentifier(workPackage, seqTrack)
    }

    public static Integer maxIdentifier(final MergingWorkPackage workPackage, final SeqTrack seqTrack) {
        assert workPackage
        assert seqTrack
        return AlignmentPass.createCriteria().get {
            eq("workPackage", workPackage)
            eq("seqTrack", seqTrack)
            projections {
                max("identifier")
            }
        }
    }

    public static Integer maxIdentifier(final SeqTrack seqTrack) {
        assert seqTrack
        return AlignmentPass.createCriteria().get {
            eq("seqTrack", seqTrack)
            projections {
                max("identifier")
            }
        }
    }

    public static int nextIdentifier(final SeqTrack seqTrack) {
        assert seqTrack
        final Integer maxIdentifier = maxIdentifier(seqTrack)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

    Project getProject() {
        return seqTrack.project
    }

    @Override
    short getProcessingPriority() {
        return project.processingPriority
    }

    Sample getSample() {
        return seqTrack.sample
    }

    @Override
    SeqType getSeqType() {
        return seqTrack.seqType
    }

    @Override
    Individual getIndividual() {
        return seqTrack.individual
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return new HashSet<SeqTrack>([seqTrack])
    }

    static mapping = {
        seqTrack index: "alignment_pass_seq_track_idx"
        workPackage index: "alignment_pass_work_package_idx"
        alignmentState index: "alignment_pass_alignment_state_idx"  // partial index: WHERE alignment_state = 'NOT_STARTED'
    }
}
