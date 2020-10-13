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
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.MailHelperService

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

abstract class AbstractAlignmentDecider implements AlignmentDecider {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    OtrsTicketService otrsTicketService

    @Autowired
    ProcessingOptionService processingOptionService

    Pipeline getPipeline(SeqTrack seqTrack) {
        return atMostOneElement(Pipeline.findAllByNameAndType(pipelineName(seqTrack), Pipeline.Type.ALIGNMENT)) ?: new Pipeline(
                name: pipelineName(seqTrack),
                type: Pipeline.Type.ALIGNMENT,
        ).save(flush: true)
    }

    /** This method is used externally. Please discuss a change in the team */
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

        if (seqTrack.seqType.exome) {
            return seqTrack.libraryPreparationKit && seqTrack.configuredReferenceGenome &&
                    BedFile.findWhere(
                            libraryPreparationKit: seqTrack.libraryPreparationKit,
                            referenceGenome: seqTrack.configuredReferenceGenome,
                    )
        }
        return true
    }

    Collection<MergingWorkPackage> findOrSaveWorkPackages(SeqTrack seqTrack,
                                                          ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType,
                                                          Pipeline pipeline) {
        // TODO OTP-1401: In the future there may be more than one MWP for the sample and seqType.
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
                Map<String, String> content = buildUnalignableSeqTrackMailContent(workPackage, seqTrack)
                mailHelperService.sendEmail(content["subject"], content["body"], content["recipient"])
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

    Map<String, String> buildUnalignableSeqTrackMailContent(MergingWorkPackage workPackage, SeqTrack seqTrack) {
        return [
                "subject"  : buildUnalignableSeqTrackMailSubject(seqTrack),
                "body"     : buildUnalignableSeqTrackMailBody(workPackage, seqTrack),
                "recipient": processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION),
        ]
    }

    String buildUnalignableSeqTrackMailSubject(SeqTrack seqTrack) {
        OtrsTicket ticket = atMostOneElement(otrsTicketService.findAllOtrsTickets([seqTrack]))
        return [
            ticket ? "[${ticket.prefixedTicketNumber}]" : null,
            "Will not be aligned:",
            seqTrack.ilseId ? "[ILSe ${seqTrack.ilseId}]" : null,
            seqTrack.run.name,
            seqTrack.project,
            seqTrack.sample,
        ].findAll().join(" ")
    }

    String buildUnalignableSeqTrackMailBody(MergingWorkPackage workPackage, SeqTrack seqTrack) {
        List<String> propertyOverview = MergingWorkPackage.getMergingProperties(seqTrack).collect { key, value ->
            return getComparedPropertiesForMail(workPackage, key, value)
        }

        return """\
            |Processing was stopped: samples which must be merged according to PID/Sample Type combination could not \
be merged because of incompatible sequencing platforms or used chemistry.
            |
            |OTP considered the following properties when checking for merging:
            |${propertyOverview.join("\n\n")}
            |
            |Please be aware that OTP can currently only handle one bam file, therefore your current samples will not be aligned.
            |Please contact ${processingOptionService.findOptionAsString(EMAIL_RECIPIENT_NOTIFICATION)} if you wish the samples \
nevertheless to be merged or if you want to withdraw the old samples (would result in deletion of the old bam files), to align \
the current ones.""".stripMargin()
    }

    String getComparedPropertiesForMail(MergingWorkPackage workPackage, String key, Object value) {
        List<String> props = ["- ${key}: ${transformObjectForMail(value ?: "None")}"]
        if (value != workPackage[key]) {
            props << "    Currently active BAM file uses: ${transformObjectForMail(workPackage[key]) ?: "None"}"
        }
        return props.join("\n")
    }

    String transformObjectForMail(Object object) {
        switch (object.class) {
            case Sample:
                Sample sample = (Sample) object
                return "${sample} from project '${sample.project.name}'"
            default:
                return object
        }
    }

    static void logNotAligning(SeqTrack seqTrack, String reason, boolean saveInSeqTrack = true) {
        seqTrack.log("Not aligning{0}, because ${reason}.", saveInSeqTrack)
    }

    boolean canPipelineAlign(SeqTrack seqTrack) {
        return SeqTypeService.defaultOtpAlignableSeqTypes.contains(seqTrack.seqType)
    }

    abstract void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign)

    abstract Pipeline.Name pipelineName(SeqTrack seqTrack)
}
