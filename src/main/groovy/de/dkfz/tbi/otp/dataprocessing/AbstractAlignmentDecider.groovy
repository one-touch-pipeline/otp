/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.MailHelperService

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

/**
 * @deprecated class is part of the old workflow system, use {@link de.dkfz.tbi.otp.workflowExecution.decider.Decider} instead
*/
@Deprecated
abstract class AbstractAlignmentDecider implements AlignmentDecider {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    OtrsTicketService otrsTicketService

    @Autowired
    UnalignableSeqTrackEmailCreator unalignableSeqTrackEmailCreator

    @Deprecated
    Pipeline getPipeline(SeqTrack seqTrack) {
        return atMostOneElement(Pipeline.findAllByNameAndType(pipelineName(seqTrack), Pipeline.Type.ALIGNMENT)) ?: new Pipeline(
                name: pipelineName(seqTrack),
                type: Pipeline.Type.ALIGNMENT,
        ).save(flush: true)
    }

    /** This method is used externally. Please discuss a change in the team */
    @Override
    @Deprecated
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign) {
        assert !seqTrack.project.archived

        if (!SeqTrackService.mayAlign(seqTrack)) {
            return Collections.emptyList()
        }

        if (!canPipelineAlign(seqTrack)) {
            logNotAligning(seqTrack, "${this.class.simpleName} says it cannot do so")
            return Collections.emptyList()
        }

        ensureConfigurationIsComplete(seqTrack)

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = seqTrack.configuredReferenceGenomeProjectSeqType
        Collection<MergingWorkPackage> workPackages = findOrSaveWorkPackages(
                seqTrack,
                referenceGenomeProjectSeqType,
                getPipeline(seqTrack),
        )

        workPackages.each {
            prepareForAlignment(it, seqTrack, forceRealign)
        }

        return workPackages
    }

    // ignore: in case of an adaptation to the new workflow system
    @SuppressWarnings('ThrowRuntimeException')
    @Deprecated
    void ensureConfigurationIsComplete(SeqTrack seqTrack) {
        if (seqTrack.configuredReferenceGenome == null) {
            throw new RuntimeException("Reference genome is not configured for SeqTrack ${seqTrack}.")
        }
        if (!hasLibraryPreparationKitAndBedFile(seqTrack)) {
            throw new RuntimeException("Library preparation kit is not set or BED file is missing for SeqTrack ${seqTrack}.")
        }
    }

    @Deprecated
    static boolean hasLibraryPreparationKitAndBedFile(SeqTrack seqTrack) {
        assert seqTrack: "The input seqTrack of method hasLibraryPreparationKitAndBedFile is null"

        if (seqTrack.seqType.exome) {
            return seqTrack.libraryPreparationKit && seqTrack.configuredReferenceGenome &&
                    atMostOneElement(BedFile.findAllWhere(
                            libraryPreparationKit: seqTrack.libraryPreparationKit,
                            referenceGenome: seqTrack.configuredReferenceGenome,
                    ))
        }
        return true
    }

    @Deprecated
    Collection<MergingWorkPackage> findOrSaveWorkPackages(SeqTrack seqTrack,
                                                          ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType,
                                                          Pipeline pipeline) {
        MergingWorkPackage workPackage = atMostOneElement(
                MergingWorkPackage.findAllWhere(
                        sample        : seqTrack.sample,
                        seqType       : seqTrack.seqType,
                        antibodyTarget: seqTrack.seqType.hasAntibodyTarget ? seqTrack.antibodyTarget : null,
                ))
        if (workPackage) {
            assert workPackage.referenceGenome.id == referenceGenomeProjectSeqType.referenceGenome.id
            assert workPackage.statSizeFileName == referenceGenomeProjectSeqType.statSizeFileName
            assert workPackage.pipeline.id == pipeline.id
            if (!workPackage.satisfiesCriteria(seqTrack)) {
                logNotAligning(seqTrack, "it does not satisfy the criteria of the existing MergingWorkPackage ${workPackage}.")
                Map<String, String> content = unalignableSeqTrackEmailCreator.getMailContent(workPackage, seqTrack)
                mailHelperService.sendEmailToTicketSystem(content["subject"], content["body"])
                return Collections.emptyList()
            }
        } else {
            workPackage = new MergingWorkPackage(
                    MergingWorkPackage.getMergingProperties(seqTrack) + [
                            referenceGenome : referenceGenomeProjectSeqType.referenceGenome,
                            statSizeFileName: referenceGenomeProjectSeqType.statSizeFileName,
                            pipeline        : pipeline,
                    ])
            workPackage.save(flush: true)
            workPackage.alignmentProperties = referenceGenomeProjectSeqType.alignmentProperties?.collect {
                ReferenceGenomeProjectSeqTypeAlignmentProperty alignmentProperty ->
                new MergingWorkPackageAlignmentProperty(name: alignmentProperty.name, value: alignmentProperty.value, mergingWorkPackage: workPackage)
            } as Set
        }

        workPackage.addToSeqTracks(seqTrack)
        workPackage.save(flush: true)

        return [workPackage]
    }

    @Deprecated
    static void logNotAligning(SeqTrack seqTrack, String reason, boolean saveInSeqTrack = true) {
        seqTrack.log("Not aligning{0}, because ${reason}.", saveInSeqTrack)
    }

    @Deprecated
    boolean canPipelineAlign(SeqTrack seqTrack) {
        return SeqTypeService.defaultOtpAlignableSeqTypes.contains(seqTrack.seqType)
    }

    @Deprecated
    abstract void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign)

    @Deprecated
    abstract Pipeline.Name pipelineName(SeqTrack seqTrack)
}
