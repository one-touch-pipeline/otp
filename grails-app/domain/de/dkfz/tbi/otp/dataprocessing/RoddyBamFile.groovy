package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*

/**
 * This bam file is produced by some Roddy alignment workflow.
 * The file is based on earlier created bam file (with the same workflow), if exists and
 * new SeqTracks which were not merged into the earlier created bam file (base bam file).
 */
class RoddyBamFile extends AbstractMergedBamFile {

    RoddyBamFile baseBamFile

    Set<SeqTrack> seqTracks

    static hasMany = [
            seqTracks: SeqTrack
    ]

    /**
     * config file used to create this bam file
     */
    RoddyWorkflowConfig config

    /**
     * bam file satisfies criteria from this {@link MergingWorkPackage}
     */
    MergingWorkPackage workPackage

    /**
     * unique identifier of this bam file in {@link RoddyBamFile#workPackage}
     */
    int identifier

    static constraints = {
        type validator: { true }
        seqTracks minSize: 1, validator: { val, obj, errors ->
            obj.isConsistentAndContainsNoWithdrawnData().each {
                errors.reject(null, it)
            }
        }
        baseBamFile nullable: true
        workPackage validator: { val, obj -> val?.workflow?.name == Workflow.Name.RODDY }
        config validator: { val, obj -> val?.workflow?.id == obj.workPackage?.workflow?.id }
        identifier unique: 'workPackage'
    }

    static mapping = {
        baseBamFile index: "roddy_bam_file_base_bam_file_idx"
        config index: "roddy_bam_file_config_idx"
        workPackage index: "roddy_bam_file_work_package_idx"
    }

    List<String> isConsistentAndContainsNoWithdrawnData() {

        List<String> errors = []

        def assertAndTrackOnError = { def expression, String errorMessage ->
            if (!expression) {
                errors << errorMessage
            }
        }

        seqTracks.each {
            assertAndTrackOnError !mergingWorkPackage || mergingWorkPackage.satisfiesCriteria(it),
                    "seqTrack ${it} does not satisfy merging criteria for ${mergingWorkPackage}"
        }

        withNewSession { session ->
            if (baseBamFile) {
                assertAndTrackOnError !mergingWorkPackage || mergingWorkPackage.satisfiesCriteria(baseBamFile),
                        "the base bam file does not satisfy work package criteria"

                assertAndTrackOnError baseBamFile.md5sum != null,
                        "the base bam file is not finished"

                assertAndTrackOnError withdrawn || !baseBamFile.withdrawn,
                        "base bam file is withdrawn for not withdrawn bam file ${this}"

                List<Long> duplicatedSeqTracksIds = baseBamFile.containedSeqTracks*.id.intersect(seqTracks*.id)
                assertAndTrackOnError duplicatedSeqTracksIds.empty,
                        "the same seqTrack is going to be merged for the second time: ${seqTracks.findAll{duplicatedSeqTracksIds.contains(it.id)}}"
            }

            Set<SeqTrack>  allContainedSeqTracks = this.getContainedSeqTracks()

            assertAndTrackOnError withdrawn || !allContainedSeqTracks.any { it.withdrawn },
                    "not withdrawn bam file has withdrawn seq tracks"

            assertAndTrackOnError numberOfMergedLanes == allContainedSeqTracks.size(),
                    "total number of merged lanes is not equal to number of contained seq tracks: ${numberOfMergedLanes} vs ${allContainedSeqTracks.size()}"
        }


        return errors
    }


    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return workPackage
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        def tmpSet = baseBamFile?.containedSeqTracks ?: []
        tmpSet.addAll(seqTracks)
        return tmpSet as Set
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        throw new UnsupportedOperationException("It has not been decided if we import QA results.")
    }

    @Override
    public boolean isMostRecentBamFile() {
        return identifier == maxIdentifier(workPackage)
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

    public static Integer maxIdentifier(MergingWorkPackage workPackage) {
        return RoddyBamFile.createCriteria().get {
            eq("workPackage", workPackage)
            projections {
                max("identifier")
            }
        }
    }

    @Override
    String toString() {
        String latest = isMostRecentBamFile() ? ' (latest)' : ''
        String withdrawn = withdrawn ? ' (withdrawn)' : ''
        return "RBF ${id}: ${identifier}${latest}${withdrawn} ${mergingWorkPackage.toStringWithoutIdAndWorkflow()}"
    }
}
