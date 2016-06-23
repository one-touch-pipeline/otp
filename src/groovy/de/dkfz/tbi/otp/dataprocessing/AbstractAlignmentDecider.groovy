package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.MailHelperService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

abstract class AbstractAlignmentDecider implements AlignmentDecider {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    MailHelperService mailHelperService

    Pipeline getPipeline() {
        Pipeline pipeline = atMostOneElement(Pipeline.findAllByNameAndType(pipelineName(), Pipeline.Type.ALIGNMENT))
        if(!pipeline) {
            pipeline = new Pipeline(
                    name: pipelineName(),
                    type: Pipeline.Type.ALIGNMENT
            ).save(failOnError: true)
        }
        return pipeline
    }

    @Override
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign) {

        if (!SeqTrackService.mayAlign(seqTrack)) {
            return Collections.emptyList()
        }

        if (!canPipelineAlign(seqTrack)) {
            logNotAligning(seqTrack, "${this.getClass().simpleName} says it cannot do so")
            return Collections.emptyList()
        }

        ensureConfigurationIsComplete(seqTrack)

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = seqTrack.configuredReferenceGenomeProjectSeqType
        Collection<MergingWorkPackage> workPackages = findOrSaveWorkPackages(
                seqTrack,
                referenceGenomeProjectSeqType.referenceGenome,
                referenceGenomeProjectSeqType.statSizeFileName,
                pipeline,
        )

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

    Collection<MergingWorkPackage> findOrSaveWorkPackages(SeqTrack seqTrack,
                                                          ReferenceGenome referenceGenome,
                                                          String statSizeFileName,
                                                          Pipeline pipeline) {

        // TODO OTP-1401: In the future there may be more than one MWP for the sample and seqType.
        MergingWorkPackage workPackage = atMostOneElement(
                MergingWorkPackage.findAllWhere(sample: seqTrack.sample, seqType: seqTrack.seqType))
        if (workPackage != null) {
            assert workPackage.referenceGenome.id == referenceGenome.id
            assert workPackage.statSizeFileName == statSizeFileName
            assert workPackage.pipeline.id == pipeline.id
            if (!workPackage.satisfiesCriteria(seqTrack)) {
                logNotAligning(seqTrack, "it does not satisfy the criteria of the existing MergingWorkPackage ${workPackage}.")
                List<String> body = []
                body << "A SeqTrack can not be aligned, because it is not compatible with the existing MergingWorkPackage."
                body << "\nInfos:"
                MergingWorkPackage.getMergingProperties(seqTrack).each {key, value ->
                    body << "- ${key}: ${value}"
                    if (value != workPackage[key]) {
                        body << "    MergingWorkPackage uses the value: ${workPackage[key]}"
                    }
                }
                body << "\n\nThis e-mail was generated automatically by OTP."
                mailHelperService.sendNotificationEmail("Will not be aligned: ${seqTrack.ilseId ? "ILSe ${seqTrack.ilseId} " : ""} ${seqTrack.run.name} ${seqTrack.project} ${seqTrack.sample}", body.join('\n'))
                return Collections.emptyList()
            }
        } else {
            workPackage = new MergingWorkPackage(
                    MergingWorkPackage.getMergingProperties(seqTrack) + [
                    referenceGenome: referenceGenome,
                    statSizeFileName: statSizeFileName,
                    pipeline: pipeline,
            ]).save(failOnError: true)
            assert workPackage
        }

        return [workPackage]
    }

    static void logNotAligning(SeqTrack seqTrack, String reason, boolean saveInSeqTrack = true) {
        seqTrack.log("Not aligning{0}, because ${reason}.", saveInSeqTrack)
    }

    boolean canPipelineAlign(SeqTrack seqTrack) {
        return SeqTypeService.alignableSeqTypes()*.id.contains(seqTrack.seqType.id)
    }

    abstract void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign)

    abstract Pipeline.Name pipelineName()
}
