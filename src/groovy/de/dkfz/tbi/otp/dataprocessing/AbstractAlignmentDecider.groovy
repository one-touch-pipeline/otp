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

abstract class AbstractAlignmentDecider implements AlignmentDecider {

    @Autowired
    ApplicationContext applicationContext

    abstract Workflow getWorkflow()

    @Override
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign) {

        if (!mayAlign(seqTrack)) {
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

    boolean mayAlign(SeqTrack seqTrack) {

        def notAligning = { String reason -> threadLog?.info("Not aligning ${seqTrack}, because ${reason}.") }

        if (seqTrack.withdrawn) {
            notAligning('it is withdrawn')
            return false
        }

        if (!DataFile.withCriteria {
            eq 'seqTrack', seqTrack
            fileType {
                eq 'type', FileType.Type.SEQUENCE
            }
            eq 'fileWithdrawn', false
        }) {
            notAligning('it has no non-withdrawn sequence file')
            return false
        }

        if (DataFile.withCriteria {
            eq 'seqTrack', seqTrack
            runSegment {
                eq 'align', false
            }
        }) {
            notAligning('alignment is disabled for the RunSegment')
            return false
        }

        if (seqTrack instanceof ExomeSeqTrack &&
                seqTrack.libraryPreparationKit == null &&
                seqTrack.kitInfoReliability == InformationReliability.UNKNOWN_VERIFIED) {
            notAligning('kitInfoReliability is UNKNOWN_VERIFIED')
            return false
        }

        if (seqTrack.seqPlatform.seqPlatformGroup == null) {
            notAligning("seqPlatformGroup is null for ${seqTrack.seqPlatform}")
            return false
        }

        if (!canWorkflowAlign(seqTrack)) {
            notAligning("${this.getClass().simpleName} says it cannot do so")
            return false
        }

        return true
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
                threadLog?.info("Not aligning ${seqTrack}, because it does not satisfy the criteria of the existing MergingWorkPackage ${workPackage}.")
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

    abstract boolean canWorkflowAlign(SeqTrack seqTrack)

    abstract void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign)
}
