package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity

/**
 * Represents a set of {@link ProcessedBamFile}s to be merged
 * for one {@link Sample}. The files are selected using the criteria
 * defined in the corresponding {@link MergingWorkPackage}.
 * A {@link MergingSet} instance is a part of corresponding
 * {@link MergingWorkPackage}.
 *
 *
 */
class MergingSet implements Entity {

    /**
     * state of processing of {@link MergingSet} instance
     */
    enum State {
        /**
         * The {@link MergingSet} has been declared (created).
         * No processing has been started on the bam files
         * from this set. No processing is planed to be started.
         * Enables manual selection of merging sets
         * to be processed (more control).
         */
        DECLARED,
        /**
         * Flag to be used by workflows to start processing of
         * files of this merging set
         */
        NEEDS_PROCESSING,
        /**
         * Files of this merging set are being processed (merged)
         */
        INPROGRESS,
        /**
         * Files of this merging set has been processed (merged)
         */
        PROCESSED
    }

    /**
     * identifier unique within the corresponding {@link MergingWorkPackage}
     */
    int identifier

    /**
     * current {@link State} of this instance
     */
    State status = State.DECLARED

    static belongsTo = [
        mergingWorkPackage: MergingWorkPackage
    ]

    Project getProject() {
        return mergingWorkPackage.project
    }

    Individual getIndividual() {
        return mergingWorkPackage.individual
    }

    Sample getSample() {
        return mergingWorkPackage.sample
    }

    SampleType getSampleType() {
        return mergingWorkPackage.sampleType
    }

    SeqType getSeqType() {
        return mergingWorkPackage.seqType
    }

    /**
     * @return bam files connected directly with this mergingSet
     */
    List<AbstractBamFile> getBamFiles() {
        return MergingSetAssignment.findAllByMergingSet(this)*.bamFile
    }

    Set<SeqTrack> getContainedSeqTracks() {
        final Set<SeqTrack> seqTracks = new HashSet<SeqTrack>()
        MergingSetAssignment.findAllByMergingSet(this).each {
            final Set<SeqTrack> seqTracksInIt = it.bamFile.containedSeqTracks
            if (!seqTracksInIt) {
                throw new RuntimeException("BAM file ${it.bamFile} has reported not to contain any SeqTracks.")
            }
            final Collection intersection = seqTracks*.id.intersect(seqTracksInIt*.id)
            if (!intersection.empty) {
                throw new IllegalStateException(
                        "MergingSet ${this} contains at least the SeqTracks with the following IDs more than once:\n${intersection.join(', ')}")
            }
            assert seqTracks.addAll(seqTracksInIt)
        }
        return seqTracks
    }

    /**
     * @return Whether this is the most recent merging set on the referenced {@link MergingWorkPackage}.
     */
    public boolean isLatestSet() {
        return identifier == maxIdentifier(mergingWorkPackage)
    }

    public static Integer maxIdentifier(final MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        return MergingSet.createCriteria().get {
            eq("mergingWorkPackage", mergingWorkPackage)
            projections{
                max("identifier")
            }
        }
    }

    public static int nextIdentifier(final MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        final Integer maxIdentifier = maxIdentifier(mergingWorkPackage)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

    static constraints = {
        identifier(unique: 'mergingWorkPackage')
        mergingWorkPackage(validator: {mergingWorkPackage -> mergingWorkPackage.pipeline.name == Pipeline.Name.DEFAULT_OTP})
    }

    static mapping = {
        mergingWorkPackage index: "merging_set_merging_work_package_idx"
    }
}
