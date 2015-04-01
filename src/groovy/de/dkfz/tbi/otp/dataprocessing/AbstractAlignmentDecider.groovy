package de.dkfz.tbi.otp.dataprocessing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService

abstract class AbstractAlignmentDecider implements AlignmentDecider {

    @Autowired
    ApplicationContext applicationContext

    abstract Workflow getWorkflow()

    @Override
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign) {

        if (!SeqTrackService.mayAlign(seqTrack)) {
            return Collections.emptyList()
        }

        if (!canWorkflowAlign(seqTrack)) {
            logNotAligning(seqTrack, "${this.getClass().simpleName} says it cannot do so")
            return Collections.emptyList()
        }

        ensureConfigurationIsComplete(seqTrack)

        Collection<MergingWorkPackage> workPackages = findOrSaveWorkPackages(seqTrack,
                seqTrack.configuredReferenceGenome, workflow)

        workPackages.each {
            prepareForAlignment(it, seqTrack, forceRealign)
        }

        return workPackages
    }

    void ensureConfigurationIsComplete(SeqTrack seqTrack) {
        if (seqTrack.configuredReferenceGenome == null) {
            throw new RuntimeException("Reference genome is not configured for SeqTrack ${seqTrack}.")
        }
        if (applicationContext.alignmentPassService.isLibraryPreparationKitOrBedFileMissing(seqTrack)) {
            throw new RuntimeException("Library preparation kit is not set or BED file is missing for SeqTrack ${seqTrack}.")
        }
    }

    static Collection<MergingWorkPackage> findOrSaveWorkPackages(SeqTrack seqTrack, ReferenceGenome referenceGenome,
                                                                 Workflow workflow) {

        // TODO OTP-1401: In the future there may be more than one MWP for the sample and seqType.
        MergingWorkPackage workPackage = atMostOneElement(
                MergingWorkPackage.findAllWhere(sample: seqTrack.sample, seqType: seqTrack.seqType))
        if (workPackage != null) {
            assert workPackage.referenceGenome.id == referenceGenome.id
            assert workPackage.workflow.id == workflow.id
            if (!workPackage.satisfiesCriteria(seqTrack)) {
                logNotAligning(seqTrack, "it does not satisfy the criteria of the existing MergingWorkPackage ${workPackage}.")
                return Collections.emptyList()
            }
        } else {
            workPackage = new MergingWorkPackage(
                    MergingWorkPackage.getMergingProperties(seqTrack) + [
                    referenceGenome: referenceGenome,
                    workflow: workflow,
            ]).save(failOnError: true)
            assert workPackage
        }

        return [workPackage]
    }

    static void logNotAligning(SeqTrack seqTrack, String reason) {
        threadLog?.info("Not aligning ${seqTrack}, because ${reason}.")
    }

    abstract boolean canWorkflowAlign(SeqTrack seqTrack)

    abstract void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign)
}
