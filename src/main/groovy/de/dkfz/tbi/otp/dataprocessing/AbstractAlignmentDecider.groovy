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
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.TrackingService
import de.dkfz.tbi.otp.utils.MailHelperService

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

abstract class AbstractAlignmentDecider implements AlignmentDecider {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    TrackingService trackingService

    @Autowired
    ProcessingOptionService processingOptionService

    Pipeline getPipeline(SeqTrack seqTrack) {
        Pipeline pipeline = atMostOneElement(Pipeline.findAllByNameAndType(pipelineName(seqTrack), Pipeline.Type.ALIGNMENT))
        if (!pipeline) {
            pipeline = new Pipeline(
                    name: pipelineName(seqTrack),
                    type: Pipeline.Type.ALIGNMENT
            ).save(flush: true)
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
                referenceGenomeProjectSeqType,
                getPipeline(seqTrack),
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
        if (!hasLibraryPreparationKitAndBedFile(seqTrack)) {
            throw new RuntimeException("Library preparation kit is not set or BED file is missing for SeqTrack ${seqTrack}.")
        }
    }

    static boolean hasLibraryPreparationKitAndBedFile(SeqTrack seqTrack) {
        assert seqTrack: "The input seqTrack of method hasLibraryPreparationKitAndBedFile is null"

        if (seqTrack instanceof ExomeSeqTrack) {
            return seqTrack.libraryPreparationKit && seqTrack.configuredReferenceGenome &&
                    BedFile.findWhere(
                            libraryPreparationKit: seqTrack.libraryPreparationKit,
                            referenceGenome: seqTrack.configuredReferenceGenome,
                    )
        } else {
            return true
        }
    }


    Collection<MergingWorkPackage> findOrSaveWorkPackages(SeqTrack seqTrack,
                                                          ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType,
                                                          Pipeline pipeline) {

        // TODO OTP-1401: In the future there may be more than one MWP for the sample and seqType.
        MergingWorkPackage workPackage = atMostOneElement(
                MergingWorkPackage.findAllWhere(
                        sample: seqTrack.sample,
                        seqType: seqTrack.seqType,
                        antibodyTarget: seqTrack instanceof ChipSeqSeqTrack ? seqTrack.antibodyTarget : null,
                ))
        if (workPackage) {
            assert workPackage.referenceGenome.id == referenceGenomeProjectSeqType.referenceGenome.id
            assert workPackage.statSizeFileName == referenceGenomeProjectSeqType.statSizeFileName
            assert workPackage.pipeline.id == pipeline.id
            if (!workPackage.satisfiesCriteria(seqTrack)) {
                logNotAligning(seqTrack, "it does not satisfy the criteria of the existing MergingWorkPackage ${workPackage}.")
                List<String> body = []
                body << "A SeqTrack can not be aligned, because it is not compatible with the existing MergingWorkPackage."
                body << "\nInfos:"
                MergingWorkPackage.getMergingProperties(seqTrack).each { key, value ->
                    body << "- ${key}: ${value}"
                    if (value != workPackage[key]) {
                        body << "    MergingWorkPackage uses the value: ${workPackage[key]}"
                    }
                }
                OtrsTicket ticket = atMostOneElement(trackingService.findAllOtrsTickets([seqTrack]))
                if (ticket) {
                    body << "\nThe corresponding ticket is: ${processingOptionService.findOptionAsString(TICKET_SYSTEM_NUMBER_PREFIX)}#${ticket.ticketNumber}"
                }
                body << "\n\nThis e-mail was generated automatically by OTP."

                mailHelperService.sendEmail(
                        "Will not be aligned: ${seqTrack.ilseId ? "ILSe ${seqTrack.ilseId} " : ""} " +
                                "${seqTrack.run.name} ${seqTrack.project} ${seqTrack.sample}",
                        body.join('\n'),
                        processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION),
                )
                return Collections.emptyList()
            }
        } else {
            workPackage = new MergingWorkPackage(
                    MergingWorkPackage.getMergingProperties(seqTrack) + [
                            referenceGenome : referenceGenomeProjectSeqType.referenceGenome,
                            statSizeFileName: referenceGenomeProjectSeqType.statSizeFileName,
                            pipeline        : pipeline,
                    ])
            workPackage.alignmentProperties = referenceGenomeProjectSeqType.alignmentProperties?.collect { ReferenceGenomeProjectSeqTypeAlignmentProperty alignmentProperty ->
                new MergingWorkPackageAlignmentProperty(name: alignmentProperty.name, value: alignmentProperty.value, mergingWorkPackage: workPackage)
            } as Set
        }

        workPackage.addToSeqTracks(seqTrack)
        assert workPackage.save(flush: true)

        return [workPackage]
    }

    static void logNotAligning(SeqTrack seqTrack, String reason, boolean saveInSeqTrack = true) {
        seqTrack.log("Not aligning{0}, because ${reason}.", saveInSeqTrack)
    }

    boolean canPipelineAlign(SeqTrack seqTrack) {
        return SeqTypeService.getDefaultOtpAlignableSeqTypes().contains(seqTrack.seqType)
    }

    abstract void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign)

    abstract Pipeline.Name pipelineName(SeqTrack seqTrack)
}
